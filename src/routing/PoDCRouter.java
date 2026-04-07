package routing;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.ArrayList;
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
 * Proof-of-Delivery-Chain router (Phases 1-3).
 *
 * <h3>Core mechanisms</h3>
 * <ol>
 *   <li><b>Proof append</b> — each outbound transfer (except ACKs) appends a
 *       {@link ProofEntry} signed with this node's Ed25519 private key.</li>
 *   <li><b>ACK generation</b> — the final recipient creates an
 *       {@link AckMessage} with the reversed proof chain (re-signed by the
 *       recipient) and puts it into the network as a small control message.</li>
 *   <li><b>Work crediting</b> — the first router to process a given ACK
 *       credits every node in the confirmation path with
 *       {@code pathWeight * decay}.</li>
 *   <li><b>Score-based forwarding</b> — data messages are relayed to a
 *       neighbor only if {@code Score(neighbor) >= Score(self)}.
 *       ACK messages bypass the score gate.</li>
 *   <li><b>Energy</b> — simple counter decremented on each successful
 *       transfer; at zero the router still receives but stops initiating.</li>
 * </ol>
 *
 * <h3>Settings (namespace {@code Group.PoDCRouter.*})</h3>
 * <table>
 *   <tr><td>alpha</td>        <td>weight of Work in Score (default 0.6)</td></tr>
 *   <tr><td>beta</td>         <td>weight of Connectivity (default 0.3)</td></tr>
 *   <tr><td>gamma</td>        <td>weight of EnergyUsed penalty (default 0.1)</td></tr>
 *   <tr><td>decayLambda</td>  <td>exponential decay rate for work credits (default 0.01)</td></tr>
 *   <tr><td>initialEnergy</td><td>starting energy units (default 100)</td></tr>
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

    // ── constants ─────────────────────────────────────────────────────
    public static final String SETTINGS_NS      = "PoDCRouter";
    public static final String ALPHA_S          = "alpha";
    public static final String BETA_S           = "beta";
    public static final String GAMMA_S          = "gamma";
    public static final String DECAY_LAMBDA_S   = "decayLambda";
    public static final String INITIAL_ENERGY_S = "initialEnergy";

    private static final String ACK_PREFIX = "PoDC_ACK_";
    private static final int    ACK_SIZE   = 64;

    public static final String PROP_IS_ACK      = "podc_isAck";
    public static final String PROP_ACK_PAYLOAD  = "podc_ackPayload";

    // ── per-instance config (immutable after construction) ────────────
    private final double alpha, beta, gamma, decayLambda, initialEnergy;

    // ── Ed25519 key pair (unique per node instance) ──────────────────
    private final PrivateKey ed25519Private;
    private final byte[]     ed25519PublicEncoded;

    // ── per-instance mutable state ───────────────────────────────────
    private double work;
    private double energy;
    private int    maxNeighborsSeen;
    private long   ackTransfersReceived;

    private final Map<String, AckMessage> pendingAcks  =
            new ConcurrentHashMap<String, AckMessage>();
    private final Set<String>             ackIssuedIds  =
            ConcurrentHashMap.newKeySet();

    // ── constructors ─────────────────────────────────────────────────

    public PoDCRouter(Settings s) {
        super(s);
        String p = SETTINGS_NS + ".";
        alpha         = s.getDouble(p + ALPHA_S,          0.6);
        beta          = s.getDouble(p + BETA_S,           0.3);
        gamma         = s.getDouble(p + GAMMA_S,          0.1);
        decayLambda   = s.getDouble(p + DECAY_LAMBDA_S,   0.01);
        initialEnergy = s.getDouble(p + INITIAL_ENERGY_S, 100.0);

        KeyPair kp = generateEd25519KeyPair();
        ed25519Private       = kp.getPrivate();
        ed25519PublicEncoded = kp.getPublic().getEncoded();

        initState();
    }

    protected PoDCRouter(PoDCRouter proto) {
        super(proto);
        alpha         = proto.alpha;
        beta          = proto.beta;
        gamma         = proto.gamma;
        decayLambda   = proto.decayLambda;
        initialEnergy = proto.initialEnergy;

        KeyPair kp = generateEd25519KeyPair();
        ed25519Private       = kp.getPrivate();
        ed25519PublicEncoded = kp.getPublic().getEncoded();

        initState();
    }

    private void initState() {
        work   = 0;
        energy = initialEnergy;
        maxNeighborsSeen     = 0;
        ackTransfersReceived = 0;
    }

    @Override
    public PoDCRouter replicate() { return new PoDCRouter(this); }

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
            KeyPairGenerator gen = KeyPairGenerator.getInstance("Ed25519");
            return gen.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new AssertionError("Ed25519 unavailable", e);
        }
    }

    /**
     * Signs {@code data} with this node's Ed25519 private key.
     * Returns the 64-byte signature, or an empty array on failure.
     */
    protected byte[] sign(String data) {
        try {
            Signature signer = Signature.getInstance("Ed25519");
            signer.initSign(ed25519Private);
            signer.update(data.getBytes(StandardCharsets.UTF_8));
            return signer.sign();
        } catch (GeneralSecurityException e) {
            System.err.println("PoDCRouter: Ed25519 sign failed — " + e);
            return new byte[0];
        }
    }

    /** X.509-encoded public key of this node (for embedding in ProofEntry). */
    protected byte[] getEd25519PublicEncoded() {
        return ed25519PublicEncoded;
    }

    // ── proof chain ──────────────────────────────────────────────────

    /** Append a proof entry for this host, signed with Ed25519. */
    protected void addProofToMessage(Message msg) {
        String id  = getHost().toString();
        long   ts  = (long) SimClock.getTime();
        String prev = msg.getProofLength() == 0
                ? "0"
                : msg.getRouteProof().get(msg.getProofLength() - 1)
                      .computeHash();
        String dataToSign = id + "|" + ts + "|" + prev;
        byte[] sig = sign(dataToSign);
        msg.addProof(new ProofEntry(id, ts, prev, sig, ed25519PublicEncoded));
    }

    // ── transfer (energy + proof) ────────────────────────────────────

    @Override
    protected int startTransfer(Message m, Connection con) {
        if (energy <= 0 && !isAck(m)) {
            return DENIED_LOW_RESOURCES;
        }
        if (!isAck(m)) {
            addProofToMessage(m);
        }
        int ret = super.startTransfer(m, con);
        if (ret == RCV_OK) {
            energy = Math.max(0, energy - 1);
            PoDCMetrics.get().recordForwarded();
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
                        latency,
                        m.getProofLength(),
                        m.getProofSizeEstimate());
                sendAck(m);
            }
        }
        return m;
    }

    // ── ACK processing & work credits ────────────────────────────────

    /** Validate ACK chain + Ed25519 signatures, credit work once globally. */
    protected void processAck(Message ackMsg) {
        Object raw = ackMsg.getProperty(PROP_ACK_PAYLOAD);
        if (!(raw instanceof AckMessage)) return;
        AckMessage ack = (AckMessage) raw;

        if (!ack.verifyChain()) {
            PoDCMetrics.get().recordInvalidAck();
            return;
        }
        String key = ack.getOriginalMessageId() + ":" + ack.getTimestamp();
        if (!PROCESSED_ACK_KEYS.add(key)) return;

        PoDCMetrics.get().recordAckProcessed();
        pendingAcks.remove(ack.getOriginalMessageId());

        List<ProofEntry> path = ack.getConfirmationPath();
        for (int i = 0; i < path.size(); i++) {
            DTNHost host = resolveHost(path.get(i).getNodeId());
            if (host == null) continue;
            double w = computeContribution(i, ack.getTimestamp());
            applyWork(host, w);
        }
    }

    /**
     * {@code pathWeight * decay}.
     * <ul>
     *   <li>pathWeight = 1 / (indexFromReceiver + 1)</li>
     *   <li>decay = exp(-lambda * (now - ackTime))</li>
     * </ul>
     */
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
     * {@code Score(X) = α·Work + β·Connectivity − γ·EnergyUsed}.
     */
    public double getScoreForNode(DTNHost node) {
        if (node == null) return 0;
        MessageRouter r = node.getRouter();
        if (r instanceof PoDCRouter) {
            PoDCRouter pr = (PoDCRouter) r;
            return alpha * pr.work
                 + beta  * pr.connectivity()
                 - gamma * pr.energyUsedFraction();
        }
        return beta * Math.min(1.0, node.getConnections().size() / 10.0);
    }

    double connectivity() {
        int n = getHost().getConnections().size();
        return maxNeighborsSeen > 0
                ? Math.min(1.0, (double) n / maxNeighborsSeen)
                : Math.min(1.0, n / 10.0);
    }

    double energyUsedFraction() {
        return initialEnergy > 0 ? (initialEnergy - energy) / initialEnergy : 0;
    }

    /**
     * Forward data if neighbor is at least as good; always forward ACKs.
     */
    protected boolean shouldForward(Message msg, DTNHost neighbor) {
        if (isAck(msg)) return true;
        return getScoreForNode(neighbor) >= getScoreForNode(getHost());
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
        tryScoreBasedForwarding();
    }

    /**
     * Score-gated forwarding.  ACKs bypass score; data messages need
     * {@link #shouldForward} approval.
     */
    protected Connection tryScoreBasedForwarding() {
        List<Connection> conns = getConnections();
        if (conns.isEmpty() || getNrofMessages() == 0) return null;

        List<Message> msgs = new ArrayList<Message>(getMessageCollection());
        sortByQueueMode(msgs);

        for (Message m : msgs) {
            if (isSending(m.getId())) continue;
            for (Connection con : conns) {
                if (!con.isReadyForTransfer()) continue;
                DTNHost nb = con.getOtherNode(getHost());

                if (isAck(m)) {
                    if (!nb.getRouter().hasMessage(m.getId())
                            && (m.getTtl() > 0 || m.getTo() == nb)) {
                        if (startTransfer(m, con) == RCV_OK) return con;
                    }
                    continue;
                }

                if (!shouldForward(m, nb)) {
                    PoDCMetrics.get().recordRejected();
                    continue;
                }
                if (nb.getRouter().hasMessage(m.getId())) continue;
                if (m.getTtl() <= 0 && m.getTo() != nb) continue;
                if (startTransfer(m, con) == RCV_OK) return con;
            }
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
                "PoDC  work=%.5f  score=%.5f  energy=%.0f/%.0f",
                work, getScoreForNode(getHost()), energy, initialEnergy)));
        top.addMoreInfo(new RoutingInfo(
                "PoDC  ACK rcvd=" + ackTransfersReceived
              + "  global verified=" + PoDCMetrics.get().avgHops()));
        top.addMoreInfo(new RoutingInfo(
                "PoDC  pending ACKs=" + pendingAcks.size()));
        return top;
    }

    // ── accessors (for subclasses and reports) ───────────────────────

    public double getWork()       { return work; }
    public double getPoDcEnergy() { return energy; }
    public double getAlpha()      { return alpha; }
    public double getBeta()       { return beta; }
    public double getGamma()      { return gamma; }

    @Override
    public String toString() {
        return "PoDCRouter@" + getHost()
             + " w=" + String.format("%.3f", work)
             + " e=" + String.format("%.0f", energy);
    }
}
