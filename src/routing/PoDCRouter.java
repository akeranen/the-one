package routing;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

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
 * Proof-of-Delivery-Chain router — reputation-filtered epidemic
 * with Ed25519-signed proof chains.
 *
 * <h3>Routing strategy</h3>
 * <ul>
 *   <li><b>Data messages</b> use epidemic-style replication with
 *       a lightweight Score filter that excludes only nodes whose
 *       reputation falls significantly below the sender's.</li>
 *   <li><b>ACK messages</b> are forwarded without score filtering
 *       to ensure timely work crediting.</li>
 * </ul>
 *
 * <h3>Score formula</h3>
 * {@code Score(X) = alpha * Work(X) + beta * Connectivity(X)}
 *
 * <h3>Settings (namespace {@code Group.PoDCRouter.*})</h3>
 * <table>
 *   <tr><td>alpha</td>          <td>weight of Work in Score (0.7)</td></tr>
 *   <tr><td>beta</td>           <td>weight of Connectivity (0.3)</td></tr>
 *   <tr><td>decayLambda</td>    <td>exponential decay for work credits (0.01)</td></tr>
 *   <tr><td>dropThreshold</td>  <td>fractional score drop for exclusion (0.5)</td></tr>
 * </table>
 */
public class PoDCRouter extends ActiveRouter {

    // ── static lifecycle ──────────────────────────────────────────────
    static { DTNSim.registerForReset(PoDCRouter.class.getCanonicalName()); }

    private static final Set<String> PROCESSED_ACK_KEYS =
            ConcurrentHashMap.newKeySet();
    private static final AtomicBoolean END_FLUSHED =
            new AtomicBoolean(false);

    public static void reset() {
        PROCESSED_ACK_KEYS.clear();
        END_FLUSHED.set(false);
        ADDRESS_BOOK.clear();
        PoDCMetrics.get().reset();
    }

    // ── constants / setting keys ─────────────────────────────────────
    public static final String SETTINGS_NS       = "PoDCRouter";
    public static final String ALPHA_S           = "alpha";
    public static final String BETA_S            = "beta";
    public static final String DECAY_LAMBDA_S    = "decayLambda";
    public static final String DROP_THRESHOLD_S  = "dropThreshold";

    private static final String ACK_PREFIX = "PoDC_ACK_";
    private static final int    ACK_SIZE   = 64;

    public static final String PROP_IS_ACK      = "podc_isAck";
    public static final String PROP_ACK_PAYLOAD  = "podc_ackPayload";

    // ── per-instance config (immutable) ──────────────────────────────
    private final double alpha, beta, decayLambda;
    private final double dropThreshold;

    // ── Ed25519 key pair (unique per node) ───────────────────────────
    private final PrivateKey ed25519Private;
    private final byte[]     ed25519PublicEncoded;

    // ── X25519 key pair for E2E encryption ─────────────────────────
    private final PrivateKey x25519Private;
    private final byte[]     x25519PublicEncoded;

    /**
     * Address book: host name -> X25519 public key.
     * In PoDC the public key <b>is</b> the node identity/address —
     * every node registers once at init, like a wallet address in crypto.
     * This is NOT a trusted third party; it simply models the fact that
     * public keys are public (derivable from a known address).
     */
    private static final Map<String, byte[]> ADDRESS_BOOK =
            new ConcurrentHashMap<String, byte[]>();

    public static final String PROP_E2E_CIPHERTEXT  = "podc_e2e_ct";
    public static final String PROP_E2E_SENDER_PUB  = "podc_e2e_senderPub";
    public static final String PROP_E2E_DECRYPTED   = "podc_e2e_plain";

    // ── per-instance mutable state ───────────────────────────────────
    private double work;
    private int    maxNeighborsSeen;
    private long   ackTransfersReceived;
    private int    nodeDataForwards;
    private int    deliveryContributions;

    private final Map<String, AckMessage> pendingAcks =
            new ConcurrentHashMap<String, AckMessage>();
    private final Set<String> ackIssuedIds =
            ConcurrentHashMap.newKeySet();

    // ── constructors ─────────────────────────────────────────────────

    public PoDCRouter(Settings s) {
        super(s);
        String p = SETTINGS_NS + ".";
        alpha          = s.getDouble(p + ALPHA_S,          0.7);
        beta           = s.getDouble(p + BETA_S,           0.3);
        decayLambda    = s.getDouble(p + DECAY_LAMBDA_S,   0.01);
        dropThreshold  = s.getDouble(p + DROP_THRESHOLD_S, 0.5);

        KeyPair kp = generateEd25519KeyPair();
        ed25519Private       = kp.getPrivate();
        ed25519PublicEncoded = kp.getPublic().getEncoded();
        KeyPair xkp = E2ECrypto.generateKeyPair();
        x25519Private       = xkp.getPrivate();
        x25519PublicEncoded = xkp.getPublic().getEncoded();
        initState();
    }

    protected PoDCRouter(PoDCRouter proto) {
        super(proto);
        alpha          = proto.alpha;
        beta           = proto.beta;
        decayLambda    = proto.decayLambda;
        dropThreshold  = proto.dropThreshold;

        KeyPair kp = generateEd25519KeyPair();
        ed25519Private       = kp.getPrivate();
        ed25519PublicEncoded = kp.getPublic().getEncoded();
        KeyPair xkp = E2ECrypto.generateKeyPair();
        x25519Private       = xkp.getPrivate();
        x25519PublicEncoded = xkp.getPublic().getEncoded();
        initState();
    }

    private void initState() {
        work = 0;
        maxNeighborsSeen = 0;
        ackTransfersReceived = 0;
        nodeDataForwards = 0;
        deliveryContributions = 0;
    }

    @Override
    public PoDCRouter replicate() { return new PoDCRouter(this); }

    // ── message creation ─────────────────────────────────────────────

    @Override
    public boolean createNewMessage(Message m) {
        if (!isAck(m)) encryptPayload(m);
        boolean ok = super.createNewMessage(m);
        if (ok && !isAck(m)) {
            PoDCMetrics.get().recordCreated();
        }
        return ok;
    }

    // ── E2E encryption (X25519 + AES-256-GCM) ────────────────────────

    private void encryptPayload(Message m) {
        DTNHost recipient = m.getTo();
        if (recipient == null) return;
        byte[] recipientPub = ADDRESS_BOOK.get(recipient.toString());
        if (recipientPub == null) return;

        String plain = "FROM:" + getHost() + "|TO:" + recipient
                + "|ID:" + m.getId()
                + "|T:" + String.format("%.0f", SimClock.getTime());
        SecretKeySpec key = E2ECrypto.deriveKey(x25519Private, recipientPub);
        byte[] ct = E2ECrypto.encrypt(key, plain.getBytes(StandardCharsets.UTF_8));
        m.updateProperty(PROP_E2E_CIPHERTEXT, ct);
        m.updateProperty(PROP_E2E_SENDER_PUB, x25519PublicEncoded);
    }

    private String tryDecrypt(Message m) {
        Object ctObj = m.getProperty(PROP_E2E_CIPHERTEXT);
        Object spObj = m.getProperty(PROP_E2E_SENDER_PUB);
        if (!(ctObj instanceof byte[]) || !(spObj instanceof byte[]))
            return null;
        SecretKeySpec key = E2ECrypto.deriveKey(x25519Private, (byte[]) spObj);
        byte[] plain = E2ECrypto.decrypt(key, (byte[]) ctObj);
        return plain != null ? new String(plain, StandardCharsets.UTF_8) : null;
    }

    // ── Ed25519 utilities ────────────────────────────────────────────

    private static KeyPair generateEd25519KeyPair() {
        try {
            return KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new AssertionError("Ed25519 unavailable", e);
        }
    }

    private static final boolean FAST_CRYPTO =
            Boolean.getBoolean("podc.fastCrypto");

    protected byte[] sign(String data) {
        if (FAST_CRYPTO) return new byte[64];
        try {
            Signature signer = Signature.getInstance("Ed25519");
            signer.initSign(ed25519Private);
            signer.update(data.getBytes(StandardCharsets.UTF_8));
            return signer.sign();
        } catch (GeneralSecurityException e) {
            return new byte[0];
        }
    }

    protected byte[] getEd25519PublicEncoded() {
        return ed25519PublicEncoded;
    }

    // ── proof chain ──────────────────────────────────────────────────

    protected void addProofToMessage(Message msg) {
        String id = getHost().toString();
        if (msg.getProofLength() > 0
                && msg.getRouteProof().get(msg.getProofLength() - 1)
                       .getNodeId().equals(id)) {
            return;
        }
        long   ts  = (long) SimClock.getTime();
        String prev = msg.getProofLength() == 0
                ? "0"
                : msg.getRouteProof().get(msg.getProofLength() - 1)
                      .computeHash();
        String dataToSign = id + "|" + ts + "|" + prev;
        byte[] sig = sign(dataToSign);
        msg.addProof(new ProofEntry(id, ts, prev, sig, ed25519PublicEncoded));
    }

    // ── transfer ─────────────────────────────────────────────────────

    @Override
    protected int startTransfer(Message m, Connection con) {
        if (!isAck(m)) addProofToMessage(m);
        int ret = super.startTransfer(m, con);
        if (ret == RCV_OK) {
            PoDCMetrics.get().recordForwarded();
            if (!isAck(m)) {
                nodeDataForwards++;
                PoDCMetrics.get().recordForwardEvent(
                        SimClock.getTime(), work);
            }
        }
        return ret;
    }

    // ── ACK creation ─────────────────────────────────────────────────

    private void sendAck(Message delivered) {
        long now = (long) SimClock.getTime();
        AckMessage ack = AckMessage.fromDeliveredMessage(
                delivered, now, ed25519Private, ed25519PublicEncoded);
        pendingAcks.put(ack.getOriginalMessageId(), ack);

        Message m = new Message(getHost(), delivered.getFrom(),
                ACK_PREFIX + delivered.getId(), ACK_SIZE);
        m.updateProperty(PROP_IS_ACK,      Boolean.TRUE);
        m.updateProperty(PROP_ACK_PAYLOAD,  ack);
        createNewMessage(m);
    }

    // ── message received ─────────────────────────────────────────────

    @Override
    public Message messageTransferred(String id, DTNHost from) {
        Message m = super.messageTransferred(id, from);
        if (m == null) return null;

        if (isAck(m)) {
            ackTransfersReceived++;
            processAck(m);
        } else if (m.getTo() == getHost()) {
            if (ackIssuedIds.add(m.getId())) {
                String decrypted = tryDecrypt(m);
                if (decrypted != null) {
                    m.updateProperty(PROP_E2E_DECRYPTED, decrypted);
                }
                double latency = SimClock.getTime() - m.getCreationTime();
                PoDCMetrics.get().recordDelivered(
                        latency, m.getProofLength(), m.getProofSizeEstimate());
                for (routing.podc.ProofEntry pe : m.getRouteProof()) {
                    DTNHost relay = resolveHost(pe.getNodeId());
                    if (relay != null) {
                        MessageRouter rr = relay.getRouter();
                        if (rr instanceof PoDCRouter) {
                            ((PoDCRouter) rr).deliveryContributions++;
                        }
                    }
                }
                sendAck(m);
            }
        }
        return m;
    }

    // ── ACK processing & work credits ────────────────────────────────

    protected void processAck(Message ackMsg) {
        Object raw = ackMsg.getProperty(PROP_ACK_PAYLOAD);
        if (!(raw instanceof AckMessage)) return;
        AckMessage ack = (AckMessage) raw;

        String key = ack.getOriginalMessageId() + ":" + ack.getTimestamp();
        if (!PROCESSED_ACK_KEYS.add(key)) return;

        if (!ack.verifyChain()) {
            PoDCMetrics.get().recordInvalidAck();
            PROCESSED_ACK_KEYS.remove(key);
            return;
        }

        PoDCMetrics.get().recordAckProcessed();
        pendingAcks.remove(ack.getOriginalMessageId());

        List<ProofEntry> path = ack.getConfirmationPath();
        for (int i = 0; i < path.size(); i++) {
            DTNHost host = resolveHost(path.get(i).getNodeId());
            if (host == null) continue;
            applyWork(host, computeContribution(i, ack.getTimestamp()));
        }
    }

    public double computeContribution(int indexFromReceiver, long ackTs) {
        double pw    = 1.0 / (indexFromReceiver + 1);
        double delta = SimClock.getTime() - ackTs;
        return pw * Math.exp(-decayLambda * delta);
    }

    private void applyWork(DTNHost host, double delta) {
        MessageRouter r = host.getRouter();
        if (r instanceof PoDCRouter && delta > 0 && !Double.isNaN(delta)) {
            ((PoDCRouter) r).work += delta;
        }
    }

    // ── score ────────────────────────────────────────────────────────

    /**
     * {@code Score(X) = alpha * Work(X) + beta * Connectivity(X)}.
     */
    public double getScoreForNode(DTNHost node) {
        if (node == null) return 0;
        MessageRouter r = node.getRouter();
        if (r instanceof PoDCRouter) {
            PoDCRouter pr = (PoDCRouter) r;
            return alpha * pr.work + beta * pr.connectivity();
        }
        return 0;
    }

    public double getConnectivity() {
        return connectivity();
    }

    double connectivity() {
        int n = getHost().getConnections().size();
        return maxNeighborsSeen > 0
                ? Math.min(1.0, (double) n / maxNeighborsSeen)
                : Math.min(1.0, n / 10.0);
    }

    // ── forwarding decision ──────────────────────────────────────────

    protected boolean shouldForward(Message msg, DTNHost neighbor) {
        double myScore = getScoreForNode(getHost());
        if (myScore <= 0) return true;
        return getScoreForNode(neighbor) >= myScore * (1.0 - dropThreshold);
    }

    // ── main update loop ─────────────────────────────────────────────

    @Override
    public void changedConnection(Connection con) {
        super.changedConnection(con);
        int n = getConnections().size();
        if (n > maxNeighborsSeen) maxNeighborsSeen = n;
    }

    @Override
    public void init(DTNHost host, List<MessageListener> mListeners) {
        super.init(host, mListeners);
        int n = getConnections().size();
        if (n > maxNeighborsSeen) maxNeighborsSeen = n;
        ADDRESS_BOOK.put(host.toString(), x25519PublicEncoded);
    }

    @Override
    public void update() {
        super.update();
        flushMetricsOnceAtEnd();

        if (isTransferring() || !canStartTransfer()) return;
        if (exchangeDeliverableMessages() != null)   return;
        tryAllMessagesToAllConnections();
    }

    @Override
    protected Message tryAllMessages(Connection con, List<Message> messages) {
        DTNHost nb = con.getOtherNode(getHost());
        boolean nbBlocked = false;
        double myScore = getScoreForNode(getHost());
        if (myScore > 0) {
            double nbScore = getScoreForNode(nb);
            nbBlocked = nbScore < myScore * (1.0 - dropThreshold);
        }
        for (Message m : messages) {
            if (!isAck(m) && nbBlocked) continue;
            int retVal = startTransfer(m, con);
            if (retVal == RCV_OK) return m;
            else if (retVal > 0) return null;
        }
        return null;
    }

    // ── helpers ───────────────────────────────────────────────────────

    static boolean isAck(Message m) {
        return Boolean.TRUE.equals(m.getProperty(PROP_IS_ACK));
    }

    private DTNHost resolveHost(String nodeId) {
        SimScenario sc = SimScenario.getInstance();
        if (sc == null || nodeId == null) return null;
        for (DTNHost h : sc.getHosts()) {
            if (h.toString().equals(nodeId)) return h;
        }
        return null;
    }

    // ── end-of-sim metrics ───────────────────────────────────────────

    private void flushMetricsOnceAtEnd() {
        SimScenario sc = SimScenario.getInstance();
        if (sc == null) return;
        if (SimClock.getTime() + 0.001 < sc.getEndTime()) return;
        if (END_FLUSHED.compareAndSet(false, true)) {
            PoDCMetrics.get().printSummary();
            PoDCMetrics.get().flush(sc.getName());
        }
    }

    // ── GUI routing info ─────────────────────────────────────────────

    @Override
    public RoutingInfo getRoutingInfo() {
        RoutingInfo top = super.getRoutingInfo();

        RoutingInfo podc = new RoutingInfo("--- PoDC Status ---");
        podc.addMoreInfo(new RoutingInfo(String.format(
                "Work = %.5f", work)));
        podc.addMoreInfo(new RoutingInfo(String.format(
                "Score = %.5f  (alpha=%.1f, beta=%.1f)",
                getScoreForNode(getHost()), alpha, beta)));
        podc.addMoreInfo(new RoutingInfo(String.format(
                "Connectivity = %.3f  (neighbors=%d, max=%d)",
                connectivity(),
                getHost().getConnections().size(), maxNeighborsSeen)));
        podc.addMoreInfo(new RoutingInfo(
                "ACKs received = " + ackTransfersReceived
              + "  |  pending = " + pendingAcks.size()));
        podc.addMoreInfo(new RoutingInfo(
                "Forwards = " + nodeDataForwards
              + "  |  delivery contributions = " + deliveryContributions));
        top.addMoreInfo(podc);

        RoutingInfo msgs = new RoutingInfo(
                "--- Messages (" + getNrofMessages() + ") ---");
        for (Message m : getMessageCollection()) {
            if (isAck(m)) continue;
            StringBuilder sb = new StringBuilder();
            sb.append(m.getId());

            int pl = m.getProofLength();
            if (pl > 0) {
                sb.append("  [").append(pl).append(" hops: ");
                for (int i = 0; i < pl; i++) {
                    if (i > 0) sb.append("->");
                    sb.append(m.getRouteProof().get(i).getNodeId());
                }
                sb.append("]");
            }

            Object ct = m.getProperty(PROP_E2E_CIPHERTEXT);
            if (ct instanceof byte[]) {
                String plain = tryDecrypt(m);
                if (plain != null) {
                    sb.append("  E2E:OPEN \"").append(plain).append("\"");
                } else {
                    sb.append("  E2E:LOCKED (").append(((byte[]) ct).length).append("B)");
                }
            }
            msgs.addMoreInfo(new RoutingInfo(sb.toString()));
        }
        top.addMoreInfo(msgs);

        RoutingInfo neighbors = new RoutingInfo(
                "--- Neighbor Scores ---");
        for (Connection c : getConnections()) {
            DTNHost nb = c.getOtherNode(getHost());
            neighbors.addMoreInfo(new RoutingInfo(String.format(
                    "%s  score=%.4f%s",
                    nb, getScoreForNode(nb),
                    shouldForward(null, nb) ? "" : "  [BLOCKED]")));
        }
        top.addMoreInfo(neighbors);

        return top;
    }

    // ── accessors ────────────────────────────────────────────────────

    public double getWork()  { return work; }
    public double getAlpha() { return alpha; }
    public double getBeta()  { return beta; }
    public int getNodeForwards()          { return nodeDataForwards; }
    public int getDeliveryContributions() { return deliveryContributions; }

    @Override
    public String toString() {
        return "PoDCRouter@" + getHost()
             + " w=" + String.format("%.3f", work);
    }
}
