package routing;

import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import core.SimScenario;

import routing.podc.ProofEntry;

import java.util.List;
import java.util.Random;

/**
 * A malicious PoDC router that tries to inflate its own {@code work}
 * by injecting forged {@link ProofEntry} records.
 *
 * <h3>Attack vector (with Ed25519)</h3>
 * When forwarding a data message the Sybil node inserts an <b>extra</b>
 * proof entry with a <em>spoofed</em> node id (picked randomly from the
 * scenario's host list).  The entry is signed with <em>this</em> node's
 * Ed25519 private key and carries <em>this</em> node's public key.
 *
 * <p>Because {@link ProofEntry#verify()} only checks that the signature
 * matches the embedded public key (no global PKI), the forged entry
 * passes cryptographic verification.  However, the public key does
 * <b>not</b> belong to the claimed {@code nodeId}, so a system with
 * identity-binding checks would detect the forgery.</p>
 */
public class SybilPoDCRouter extends PoDCRouter {

    private final Random rng = new Random();

    public SybilPoDCRouter(Settings s) { super(s); }

    protected SybilPoDCRouter(SybilPoDCRouter proto) { super(proto); }

    @Override
    public SybilPoDCRouter replicate() {
        return new SybilPoDCRouter(this);
    }

    /**
     * Injects a forged proof entry with a randomly chosen victim node id
     * (signed with this Sybil node's own key) <em>before</em> the
     * legitimate proof entry for this host.
     */
    @Override
    protected void addProofToMessage(Message msg) {
        DTNHost victim = pickRandomVictim();
        if (victim != null) {
            String fakeId = victim.toString();
            long ts = (long) SimClock.getTime();
            String prev = msg.getProofLength() == 0
                    ? "0"
                    : msg.getRouteProof()
                          .get(msg.getProofLength() - 1).computeHash();
            String dataToSign = fakeId + "|" + ts + "|" + prev;
            byte[] sig = sign(dataToSign);
            msg.addProof(new ProofEntry(
                    fakeId, ts, prev, sig, getEd25519PublicEncoded()));
        }
        super.addProofToMessage(msg);
    }

    private DTNHost pickRandomVictim() {
        SimScenario sc = SimScenario.getInstance();
        if (sc == null) return null;
        List<DTNHost> hosts = sc.getHosts();
        if (hosts.size() <= 1) return null;
        DTNHost victim;
        do {
            victim = hosts.get(rng.nextInt(hosts.size()));
        } while (victim == getHost());
        return victim;
    }

    @Override
    public String toString() {
        return "SybilPoDCRouter@" + getHost()
             + " w=" + String.format("%.3f", getWork());
    }
}
