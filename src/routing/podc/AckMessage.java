package routing.podc;

import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Proof of delivery issued by the destination of a data message.
 *
 * The chain is the forward proof chain exactly as it arrived (each entry
 * signed by the node that forwarded the message) followed by one delivery
 * entry signed by the destination. Any node can verify the whole chain
 * from the ACK alone: signatures, identity binding of every entry, hash
 * links, and the genesis value that ties the chain to the message id,
 * the source and the destination.
 */
public class AckMessage {

    private final String originalMessageId;
    private final String destinationId;
    private final List<ProofEntry> chain;

    public AckMessage(String originalMessageId, String destinationId,
                      List<ProofEntry> chain) {
        this.originalMessageId = originalMessageId;
        this.destinationId = destinationId;
        this.chain = new ArrayList<ProofEntry>(chain);
    }

    /** Builds the ACK at the destination from the received forward chain. */
    public static AckMessage issue(String msgId, List<ProofEntry> forward,
                                   long now, PrivateKey dstPriv,
                                   byte[] dstPub) {
        List<ProofEntry> c = new ArrayList<ProofEntry>(forward);
        String prev = c.get(c.size() - 1).computeHash();
        c.add(ProofEntry.create(now, prev, dstPriv, dstPub));
        return new AckMessage(msgId, ProofEntry.fingerprint(dstPub), c);
    }

    public boolean verifyChain() {
        if (chain.size() < 2) return false;
        ProofEntry first = chain.get(0);
        String g = ProofEntry.genesis(originalMessageId,
                first.getNodeId(), destinationId);
        if (!g.equals(first.getPrevHash())) return false;
        if (!destinationId.equals(chain.get(chain.size() - 1).getNodeId()))
            return false;
        for (int i = 0; i < chain.size(); i++) {
            ProofEntry e = chain.get(i);
            if (i > 0 && !e.getPrevHash().equals(chain.get(i - 1).computeHash()))
                return false;
            if (!e.verify()) return false;
        }
        return true;
    }

    /**
     * Relay identifiers ordered from the destination side (index 0 is the
     * node that handed the message to the destination). The source and the
     * destination are excluded, and every identifier appears once.
     */
    public List<String> relaysFromDestination() {
        String src = chain.get(0).getNodeId();
        Set<String> seen = new LinkedHashSet<String>();
        for (int i = chain.size() - 2; i >= 1; i--) {
            String id = chain.get(i).getNodeId();
            if (!id.equals(src) && !id.equals(destinationId)) seen.add(id);
        }
        return new ArrayList<String>(seen);
    }

    /** Unique key of this proof of delivery. */
    public String key() {
        return originalMessageId + ":" + chain.get(chain.size() - 1).computeHash();
    }

    public String getOriginalMessageId() { return originalMessageId; }
    public String getDestinationId()     { return destinationId; }
    public String getSourceId()          { return chain.get(0).getNodeId(); }
    public long   getTimestamp() { return chain.get(chain.size() - 1).getTimestamp(); }
    public List<ProofEntry> getChain() { return Collections.unmodifiableList(chain); }
    public int sizeBytes() { return 64 + 128 * chain.size(); }
}
