package routing;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.spec.SecretKeySpec;

import core.Connection;
import core.DTNHost;
import core.DTNSim;
import core.Message;
import core.MessageListener;
import core.Settings;
import core.SimClock;
import core.SimScenario;

import routing.podc.AckMessage;
import routing.podc.E2ECrypto;
import routing.podc.PoDCMetrics;
import routing.podc.ProofEntry;
import routing.util.RoutingInfo;

/**
 * Proof-of-Delivery-Chain router: reputation-filtered epidemic forwarding
 * with Ed25519-signed proof chains and locally verified reputation.
 *
 * <h3>Reputation</h3>
 * Every node keeps its own ledger. When a node receives an ACK it verifies
 * the chain itself and credits the relays listed in it (never the source or
 * the destination): the relay at position i from the destination receives
 * 1/(i+1). Accumulated work decays continuously,
 * W(t) = sum_k c_k exp(-lambda (t - t_k)), where t_k is the delivery time
 * signed by the destination. No node reads or writes another node's state.
 *
 * <h3>Score</h3>
 * {@code Score(X) = alpha * W(X) + beta * Connectivity(X)}, where W(X) is
 * taken from the observer's own ledger and Connectivity(X) is the value X
 * announces in its hello beacon (current neighbours / maximum seen).
 *
 * <h3>Settings (namespace {@code PoDCRouter})</h3>
 * alpha (0.7), beta (0.3), decayLambda (0.001 1/s), dropThreshold (0.5),
 * normalizeWork (true: W divided by the largest W in the local ledger),
 * ackBudget (10 ACKs per contact, 0 = unlimited; data always goes first),
 * minContribution (0 = off; strict mode threshold on normalized W),
 * matureLedger (5), explore (0.1).
 */
public class PoDCRouter extends ActiveRouter {

    static { DTNSim.registerForReset(PoDCRouter.class.getCanonicalName()); }

    public static void reset() {
        ADDRESS_BOOK.clear();
        PoDCMetrics.get().reset();
    }

    public static final String SETTINGS_NS       = "PoDCRouter";
    public static final String ALPHA_S           = "alpha";
    public static final String BETA_S            = "beta";
    public static final String DECAY_LAMBDA_S    = "decayLambda";
    public static final String DROP_THRESHOLD_S  = "dropThreshold";

    public static final String ACK_PREFIX = "PoDC_ACK_";
    public static final String PROP_IS_ACK       = "podc_isAck";
    public static final String PROP_ACK_PAYLOAD  = "podc_ackPayload";
    public static final String PROP_E2E_CIPHERTEXT  = "podc_e2e_ct";
    public static final String PROP_E2E_SENDER_PUB  = "podc_e2e_senderPub";

    /**
     * Public identity of each host: Ed25519 fingerprint (the address) and
     * X25519 key for payload encryption. Senders are assumed to know the
     * public identity of the destination, as with a wallet address.
     */
    private static final Map<String, String[]> ADDRESS_BOOK =
            new ConcurrentHashMap<String, String[]>();
    private static final Map<String, byte[]> X25519_BOOK =
            new ConcurrentHashMap<String, byte[]>();

    protected final double alpha, beta, decayLambda, dropThreshold;
    protected final boolean normalizeWork;
    protected final int ackBudget;
    private final Map<Connection, Integer> acksSentOnContact =
            new HashMap<Connection, Integer>();
    private double maxCache, maxCacheTime = -1;
    protected final double minContribution, explore;
    protected final int matureLedger;
    private final Map<Connection, Boolean> exploreOnContact =
            new HashMap<Connection, Boolean>();
    private final Random exploreRng = new Random(1);

    private final PrivateKey ed25519Private;
    private final byte[]     ed25519Public;
    private final String     myId;
    private final PrivateKey x25519Private;
    private final byte[]     x25519Public;

    /** Local ledger: identity -> {work at time t, t}. */
    private final Map<String, double[]> ledger = new HashMap<String, double[]>();
    private final Set<String> processedAcks = new HashSet<String>();
    private final Set<String> ackIssuedIds = new HashSet<String>();
    private int maxNeighborsSeen;
    private int nodeDataForwards;
    private final Random payloadRng = new Random();

    public PoDCRouter(Settings s) {
        super(s);
        Settings ps = new Settings(SETTINGS_NS);
        alpha         = ps.getDouble(ALPHA_S, 0.7);
        beta          = ps.getDouble(BETA_S, 0.3);
        decayLambda   = ps.getDouble(DECAY_LAMBDA_S, 0.001);
        dropThreshold = ps.getDouble(DROP_THRESHOLD_S, 0.5);
        normalizeWork = ps.getBoolean("normalizeWork", true);
        ackBudget     = ps.getInt("ackBudget", 10);
        minContribution = ps.getDouble("minContribution", 0);
        matureLedger  = ps.getInt("matureLedger", 5);
        explore       = ps.getDouble("explore", 0.1);
        ed25519Private = null; ed25519Public = null; myId = null;
        x25519Private = null; x25519Public = null;
    }

    protected PoDCRouter(PoDCRouter proto) {
        super(proto);
        alpha         = proto.alpha;
        beta          = proto.beta;
        decayLambda   = proto.decayLambda;
        dropThreshold = proto.dropThreshold;
        normalizeWork = proto.normalizeWork;
        ackBudget     = proto.ackBudget;
        minContribution = proto.minContribution;
        matureLedger  = proto.matureLedger;
        explore       = proto.explore;
        KeyPair kp = newEd25519();
        ed25519Private = kp.getPrivate();
        ed25519Public  = kp.getPublic().getEncoded();
        myId = ProofEntry.fingerprint(ed25519Public);
        KeyPair xkp = E2ECrypto.generateKeyPair();
        x25519Private = xkp.getPrivate();
        x25519Public  = xkp.getPublic().getEncoded();
    }

    @Override
    public PoDCRouter replicate() { return new PoDCRouter(this); }

    protected static KeyPair newEd25519() {
        try {
            return KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new AssertionError("Ed25519 unavailable", e);
        }
    }

    @Override
    public void init(DTNHost host, List<MessageListener> mListeners) {
        super.init(host, mListeners);
        ADDRESS_BOOK.put(host.toString(), new String[]{myId});
        X25519_BOOK.put(host.toString(), x25519Public);
        PoDCMetrics.get().register(myId, this);
    }

    public String getNodeId() { return myId; }

    // ── reputation ledger ────────────────────────────────────────────

    public double observedWork(String id) {
        double[] e = ledger.get(id);
        if (e == null) return 0;
        return e[0] * Math.exp(-decayLambda * (SimClock.getTime() - e[1]));
    }

    private void credit(String id, double c, double tDelivery) {
        double now = SimClock.getTime();
        double add = c * Math.exp(-decayLambda * Math.max(0, now - tDelivery));
        ledger.put(id, new double[]{observedWork(id) + add, now});
    }

    /** Verifies an ACK locally and credits the relays it lists. */
    protected void processAck(AckMessage ack) {
        if (!processedAcks.add(ack.key())) return;
        if (ack.getTimestamp() > SimClock.getTime() + 1 || !ack.verifyChain()) {
            PoDCMetrics.get().recordInvalidAck();
            return;
        }
        PoDCMetrics.get().recordAckProcessed();
        List<String> relays = ack.relaysFromDestination();
        for (int i = 0; i < relays.size(); i++) {
            credit(relays.get(i), 1.0 / (i + 1), ack.getTimestamp());
        }
    }

    /** Connectivity announced in this node's hello beacon. */
    public double advertisedConnectivity() {
        int n = getHost().getConnections().size();
        return maxNeighborsSeen > 0
                ? Math.min(1.0, (double) n / maxNeighborsSeen)
                : Math.min(1.0, n / 10.0);
    }

    /** Largest work in the local ledger, used to normalize W to [0, 1]. */
    private double maxObservedWork() {
        double now = SimClock.getTime();
        if (now != maxCacheTime) {
            double m = 0;
            for (double[] e : ledger.values()) {
                m = Math.max(m, e[0] * Math.exp(-decayLambda * (now - e[1])));
            }
            maxCache = m;
            maxCacheTime = now;
        }
        return maxCache;
    }

    public double normalizedWork(String id) {
        if (!normalizeWork) return observedWork(id);
        double m = maxObservedWork();
        return m > 0 ? observedWork(id) / m : 0;
    }

    /** Score of a node as seen by this node. */
    public double scoreOf(DTNHost node) {
        MessageRouter r = node.getRouter();
        if (!(r instanceof PoDCRouter)) return 0;
        PoDCRouter pr = (PoDCRouter) r;
        return alpha * normalizedWork(pr.getNodeId())
             + beta * pr.advertisedConnectivity();
    }

    private int contributorsKnown() {
        double now = SimClock.getTime();
        int n = 0;
        for (double[] e : ledger.values()) {
            if (e[0] * Math.exp(-decayLambda * (now - e[1])) > 0) n++;
        }
        return n;
    }

    /**
     * A neighbour is excluded from relaying data if its score is far below
     * this node's own, or if this node's ledger is mature and the neighbour
     * has no confirmed contribution. In the latter case a contact is still
     * admitted with probability {@code explore}, decided once per contact,
     * so that new nodes can earn reputation.
     */
    protected boolean blocked(Connection con) {
        DTNHost neighbor = con.getOtherNode(getHost());
        double my = scoreOf(getHost());
        if (my > 0 && scoreOf(neighbor) < my * (1.0 - dropThreshold)) {
            return true;
        }
        MessageRouter r = neighbor.getRouter();
        if (minContribution <= 0 || !(r instanceof PoDCRouter)) return false;
        if (contributorsKnown() < matureLedger) return false;
        if (normalizedWork(((PoDCRouter) r).getNodeId()) >= minContribution) {
            return false;
        }
        Boolean admit = exploreOnContact.get(con);
        if (admit == null) {
            admit = exploreRng.nextDouble() < explore;
            exploreOnContact.put(con, admit);
        }
        return !admit;
    }

    // ── messages ─────────────────────────────────────────────────────

    @Override
    public boolean createNewMessage(Message m) {
        if (!isAck(m)) encryptPayload(m);
        boolean ok = super.createNewMessage(m);
        if (ok && !isAck(m)) PoDCMetrics.get().recordCreated();
        return ok;
    }

    private void encryptPayload(Message m) {
        byte[] recipientPub = X25519_BOOK.get(m.getTo().toString());
        if (recipientPub == null) return;
        byte[] payload = new byte[m.getSize()];
        payloadRng.nextBytes(payload);
        SecretKeySpec key = E2ECrypto.deriveKey(x25519Private, recipientPub);
        m.updateProperty(PROP_E2E_CIPHERTEXT, E2ECrypto.encrypt(key, payload));
        m.updateProperty(PROP_E2E_SENDER_PUB, x25519Public);
    }

    private boolean decryptPayload(Message m) {
        Object ct = m.getProperty(PROP_E2E_CIPHERTEXT);
        Object sp = m.getProperty(PROP_E2E_SENDER_PUB);
        if (!(ct instanceof byte[]) || !(sp instanceof byte[])) return false;
        SecretKeySpec key = E2ECrypto.deriveKey(x25519Private, (byte[]) sp);
        return E2ECrypto.decrypt(key, (byte[]) ct) != null;
    }

    protected void addProofToMessage(Message msg) {
        List<ProofEntry> proof = msg.getRouteProof();
        if (!proof.isEmpty()
                && proof.get(proof.size() - 1).getNodeId().equals(myId)) {
            return;
        }
        String prev;
        if (proof.isEmpty()) {
            String[] dst = ADDRESS_BOOK.get(msg.getTo().toString());
            prev = ProofEntry.genesis(msg.getId(), myId,
                    dst != null ? dst[0] : "");
        } else {
            prev = proof.get(proof.size() - 1).computeHash();
        }
        msg.addProof(ProofEntry.create((long) SimClock.getTime(), prev,
                ed25519Private, ed25519Public));
    }

    @Override
    protected int startTransfer(Message m, Connection con) {
        if (!isAck(m)) addProofToMessage(m);
        int ret = super.startTransfer(m, con);
        if (ret == RCV_OK && !isAck(m)) {
            nodeDataForwards++;
            PoDCMetrics.get().recordDataForward(SimClock.getTime(),
                    con.getOtherNode(getHost()));
        }
        return ret;
    }

    private void issueAck(Message delivered) {
        AckMessage ack = AckMessage.issue(delivered.getId(),
                delivered.getRouteProof(), (long) SimClock.getTime(),
                ed25519Private, ed25519Public);
        processAck(ack);
        Message m = new Message(getHost(), delivered.getFrom(),
                ACK_PREFIX + delivered.getId(), ack.sizeBytes());
        m.updateProperty(PROP_IS_ACK, Boolean.TRUE);
        m.updateProperty(PROP_ACK_PAYLOAD, ack);
        createNewMessage(m);
    }

    @Override
    public Message messageTransferred(String id, DTNHost from) {
        Message m = super.messageTransferred(id, from);
        if (m == null) return null;
        if (isAck(m)) {
            Object raw = m.getProperty(PROP_ACK_PAYLOAD);
            if (raw instanceof AckMessage) processAck((AckMessage) raw);
        } else if (m.getTo() == getHost() && ackIssuedIds.add(m.getId())) {
            PoDCMetrics.get().recordDelivered(
                    SimClock.getTime() - m.getCreationTime(),
                    m.getProofLength(), m.getProofSizeEstimate(),
                    decryptPayload(m), m.getRouteProof());
            issueAck(m);
        }
        return m;
    }

    // ── forwarding ───────────────────────────────────────────────────

    @Override
    public void update() {
        super.update();
        PoDCMetrics.get().tick();
        if (isTransferring() || !canStartTransfer()) return;
        if (exchangeDeliverableMessages() != null) return;
        tryAllMessagesToAllConnections();
    }

    @Override
    public void changedConnection(Connection con) {
        super.changedConnection(con);
        int n = getConnections().size();
        if (n > maxNeighborsSeen) maxNeighborsSeen = n;
        if (!con.isUp()) {
            acksSentOnContact.remove(con);
            exploreOnContact.remove(con);
        }
    }

    /** Data first; ACKs only when no data can be sent, at most ackBudget per contact. */
    @Override
    protected Message tryAllMessages(Connection con, List<Message> messages) {
        if (!blocked(con)) {
            for (Message m : messages) {
                if (isAck(m)) continue;
                int retVal = startTransfer(m, con);
                if (retVal == RCV_OK) return m;
                else if (retVal > 0) return null;
            }
        }
        Integer sent = acksSentOnContact.get(con);
        int used = sent == null ? 0 : sent;
        for (Message m : messages) {
            if (!isAck(m)) continue;
            if (ackBudget > 0 && used >= ackBudget) return null;
            int retVal = startTransfer(m, con);
            if (retVal == RCV_OK) {
                acksSentOnContact.put(con, used + 1);
                return m;
            }
            else if (retVal > 0) return null;
        }
        return null;
    }

    public static boolean isAck(Message m) {
        return Boolean.TRUE.equals(m.getProperty(PROP_IS_ACK));
    }

    @Override
    public RoutingInfo getRoutingInfo() {
        RoutingInfo top = super.getRoutingInfo();
        RoutingInfo podc = new RoutingInfo("--- PoDC Status ---");
        podc.addMoreInfo(new RoutingInfo(String.format(
                "Own work (local view) = %.5f", observedWork(myId))));
        podc.addMoreInfo(new RoutingInfo("Ledger entries = " + ledger.size()));
        podc.addMoreInfo(new RoutingInfo("Forwards = " + nodeDataForwards));
        top.addMoreInfo(podc);
        return top;
    }

    public int getNodeForwards() { return nodeDataForwards; }
    public String getHostName() { return getHost().toString(); }
    public double getAlpha() { return alpha; }
    public double getBeta()  { return beta; }

    protected PrivateKey getEd25519Private() { return ed25519Private; }
    protected byte[] getEd25519Public() { return ed25519Public; }

    protected static String[] lookupIdentity(String hostName) {
        return ADDRESS_BOOK.get(hostName);
    }

    @Override
    public String toString() {
        return "PoDCRouter@" + getHost();
    }
}
