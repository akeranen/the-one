package routing.podc;

import core.Message;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Acknowledgement payload carried inside a control {@link Message}.
 *
 * When the final recipient accepts a data message it builds an
 * {@code AckMessage} via {@link #fromDeliveredMessage}.  The
 * {@link #confirmationPath} is the delivered message's
 * {@link Message#getRouteProof()} reversed (receiver → source) with
 * a freshly computed hash chain.  Each entry in the reverse chain is
 * signed by the ACK creator (the final recipient) using Ed25519.
 */
public class AckMessage {

    private final String originalMessageId;
    private final List<ProofEntry> confirmationPath;
    private final long timestamp;

    /* ---- factory ---- */

    /**
     * Reverses the forward proof chain and re-links the hashes.
     * Each new entry is signed by the ACK creator's Ed25519 key.
     *
     * @param delivered       the delivered data message
     * @param now             simulation timestamp for the ACK
     * @param signerPrivate   Ed25519 private key of the ACK creator
     * @param signerPubEncoded X.509-encoded public key of the ACK creator
     */
    public static AckMessage fromDeliveredMessage(
            Message delivered, long now,
            PrivateKey signerPrivate, byte[] signerPubEncoded) {
        List<ProofEntry> fwd = delivered.getRouteProof();
        List<ProofEntry> rev = new ArrayList<ProofEntry>();
        String prev = "0";
        for (int i = fwd.size() - 1; i >= 0; i--) {
            ProofEntry e = fwd.get(i);
            String data = e.getNodeId() + "|" + e.getTimestamp() + "|" + prev;
            byte[] sig = signData(signerPrivate, data);
            ProofEntry step = new ProofEntry(
                    e.getNodeId(), e.getTimestamp(), prev,
                    sig, signerPubEncoded);
            rev.add(step);
            prev = step.computeHash();
        }
        return new AckMessage(delivered.getId(), rev, now);
    }

    private static byte[] signData(PrivateKey key, String data) {
        try {
            Signature signer = Signature.getInstance("Ed25519");
            signer.initSign(key);
            signer.update(data.getBytes(StandardCharsets.UTF_8));
            return signer.sign();
        } catch (GeneralSecurityException e) {
            return new byte[0];
        }
    }

    /* ---- constructors ---- */

    public AckMessage(String originalMessageId,
                      List<ProofEntry> confirmationPath, long timestamp) {
        this.originalMessageId = originalMessageId;
        this.confirmationPath  = confirmationPath != null
                ? new ArrayList<ProofEntry>(confirmationPath)
                : new ArrayList<ProofEntry>();
        this.timestamp = timestamp;
    }

    /* ---- verification ---- */

    /**
     * Validates both structural integrity (hash chain) and cryptographic
     * integrity (Ed25519 signature) of every entry in the path.
     *
     * <ol>
     *   <li>First entry's prevHash must be {@code "0"}.</li>
     *   <li>Each subsequent entry's prevHash must equal the preceding
     *       entry's {@link ProofEntry#computeHash()}.</li>
     *   <li>{@link ProofEntry#verify()} must return {@code true} for
     *       every entry (Ed25519 signature check).</li>
     * </ol>
     */
    public boolean verifyChain() {
        List<ProofEntry> path = confirmationPath;
        if (path.isEmpty()) return true;
        for (int i = 0; i < path.size(); i++) {
            ProofEntry e = path.get(i);
            if (i == 0) {
                if (!"0".equals(e.getPrevHash())) return false;
            } else {
                if (!e.getPrevHash().equals(path.get(i - 1).computeHash()))
                    return false;
            }
            if (!e.verify()) return false;
        }
        return true;
    }

    /* ---- accessors ---- */

    public String getOriginalMessageId() { return originalMessageId; }
    public long   getTimestamp()         { return timestamp; }

    public List<ProofEntry> getConfirmationPath() {
        return Collections.unmodifiableList(confirmationPath);
    }

    public int getHopCount() { return confirmationPath.size(); }
}
