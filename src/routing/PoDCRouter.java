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

import core.Connection;
import core.DTNHost;
import core.DTNSim;
import core.Message;
import core.MessageListener;
import core.Settings;
import core.SimClock;
import core.SimScenario;

import routing.podc.AckMessage;
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
        boolean ok = super.createNewMessage(m);
        if (ok && !isAck(m)) {
            PoDCMetrics.get().recordCreated();
        }
        return ok;
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
        top.addMoreInfo(new RoutingInfo(String.format(
                "PoDC  work=%.5f  score=%.5f",
                work, getScoreForNode(getHost()))));
        top.addMoreInfo(new RoutingInfo(
                "PoDC  ACK rcvd=" + ackTransfersReceived));
        top.addMoreInfo(new RoutingInfo(
                "PoDC  pending ACKs=" + pendingAcks.size()));
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
