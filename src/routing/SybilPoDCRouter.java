package routing;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import core.SimScenario;

import routing.podc.AckMessage;
import routing.podc.ProofEntry;

/**
 * Malicious PoDC node.
 *
 * <ul>
 *   <li><b>blackhole</b>: accepts data messages from neighbours and
 *       silently drops them.</li>
 *   <li><b>self-promotion</b>: every {@code fakeAckInterval} seconds until
 *       {@code attackStop} it fabricates a proof of delivery between two of
 *       its own Sybil identities in which its real identity is the only
 *       relay, so honest verifiers credit it with 1. All signatures are
 *       valid, so the fake ACK passes verification.</li>
 *   <li>while promoting, it announces maximal connectivity in hello.</li>
 *   <li><b>spoofIdentity</b>: the relay entry of the fake ACK claims the
 *       identity of a random honest node but is signed with the attacker's
 *       key (the attack of the first PoDC version).</li>
 * </ul>
 * Settings namespace {@code SybilPoDCRouter}: blackhole (true),
 * fakeAckInterval (0 = off), attackStop (end of simulation),
 * spoofIdentity (false).
 */
public class SybilPoDCRouter extends PoDCRouter {

    private final boolean blackhole, spoofIdentity;
    private final double fakeAckInterval, attackStop;
    private KeyPair sybilA, sybilB;
    private double nextFake;
    private int fakeCount;
    private final Random rng = new Random(7);

    public SybilPoDCRouter(Settings s) {
        super(s);
        Settings ss = new Settings("SybilPoDCRouter");
        blackhole = ss.getBoolean("blackhole", true);
        spoofIdentity = ss.getBoolean("spoofIdentity", false);
        fakeAckInterval = ss.getDouble("fakeAckInterval", 0);
        attackStop = ss.getDouble("attackStop", Double.MAX_VALUE);
    }

    protected SybilPoDCRouter(SybilPoDCRouter proto) {
        super(proto);
        blackhole = proto.blackhole;
        spoofIdentity = proto.spoofIdentity;
        fakeAckInterval = proto.fakeAckInterval;
        attackStop = proto.attackStop;
        sybilA = newEd25519();
        sybilB = newEd25519();
    }

    @Override
    public SybilPoDCRouter replicate() { return new SybilPoDCRouter(this); }

    private boolean promoting() {
        return fakeAckInterval > 0 && SimClock.getTime() < attackStop;
    }

    @Override
    public double advertisedConnectivity() {
        return promoting() ? 1.0 : super.advertisedConnectivity();
    }

    private final Set<String> swallowed = new HashSet<String>();

    @Override
    protected int checkReceiving(Message m, DTNHost from) {
        if (swallowed.contains(m.getId())) return DENIED_OLD;
        return super.checkReceiving(m, from);
    }

    @Override
    public Message messageTransferred(String id, DTNHost from) {
        Message m = super.messageTransferred(id, from);
        if (m != null && blackhole && !isAck(m) && m.getTo() != getHost()
                && hasMessage(id)) {
            swallowed.add(id);
            deleteMessage(id, true);
        }
        return m;
    }

    @Override
    public void update() {
        if (promoting() && SimClock.getTime() >= nextFake) {
            nextFake = SimClock.getTime() + fakeAckInterval;
            injectFakeAck();
        }
        super.update();
    }

    private void injectFakeAck() {
        long ts = (long) SimClock.getTime();
        String msgId = "SYB_" + getHost() + "_" + (fakeCount++);
        byte[] pa = sybilA.getPublic().getEncoded();
        byte[] pb = sybilB.getPublic().getEncoded();
        String ida = ProofEntry.fingerprint(pa);
        String idb = ProofEntry.fingerprint(pb);
        ProofEntry e0 = ProofEntry.create(ts,
                ProofEntry.genesis(msgId, ida, idb), sybilA.getPrivate(), pa);
        ProofEntry e1;
        if (spoofIdentity) {
            String[] victim = lookupIdentity(randomHonestHost().toString());
            e1 = ProofEntry.spoofed(victim[0], ts, e0.computeHash(),
                    getEd25519Private(), getEd25519Public());
        } else {
            e1 = ProofEntry.create(ts, e0.computeHash(),
                    getEd25519Private(), getEd25519Public());
        }
        ProofEntry e2 = ProofEntry.create(ts, e1.computeHash(),
                sybilB.getPrivate(), pb);
        AckMessage ack = new AckMessage(msgId, idb,
                new ArrayList<ProofEntry>(Arrays.asList(e0, e1, e2)));

        DTNHost target = randomHonestHost();
        if (target == null) return;
        Message m = new Message(getHost(), target, ACK_PREFIX + msgId,
                ack.sizeBytes());
        m.updateProperty(PROP_IS_ACK, Boolean.TRUE);
        m.updateProperty(PROP_ACK_PAYLOAD, ack);
        createNewMessage(m);
    }

    private DTNHost randomHonestHost() {
        List<DTNHost> honest = new ArrayList<DTNHost>();
        for (DTNHost h : SimScenario.getInstance().getHosts()) {
            if (!(h.getRouter() instanceof SybilPoDCRouter)) honest.add(h);
        }
        return honest.isEmpty() ? null : honest.get(rng.nextInt(honest.size()));
    }

    @Override
    public String toString() {
        return "SybilPoDCRouter@" + getHost();
    }
}
