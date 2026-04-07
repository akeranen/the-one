package routing.podc;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;

/**
 * A single record in a message's proof-of-delivery chain.
 *
 * Each forwarding hop appends one {@code ProofEntry} to the message.
 * The chain is linked via {@link #prevHash}: the first entry uses
 * {@code "0"}, each subsequent entry stores the SHA-256 hash of
 * its predecessor.
 *
 * <p>The {@link #signature} is a 64-byte Ed25519 digital signature over
 * the canonical data string {@code nodeId|timestamp|prevHash}.
 * {@link #publicKey} carries the signer's X.509-encoded Ed25519 public
 * key so that any node can call {@link #verify()} without a global PKI.</p>
 */
public class ProofEntry {

    private final String nodeId;
    private final long   timestamp;
    private final String prevHash;
    private final byte[] signature;
    private final byte[] publicKey;

    /**
     * @param nodeId    {@code DTNHost.toString()} of the forwarding node
     * @param timestamp simulation second (truncated from {@code SimClock})
     * @param prevHash  {@link #computeHash()} of the previous entry, or
     *                  {@code "0"} for the very first entry
     * @param signature Ed25519 signature (64 bytes) over
     *                  {@code nodeId|timestamp|prevHash}
     * @param publicKey X.509-encoded Ed25519 public key of the signer
     */
    public ProofEntry(String nodeId, long timestamp, String prevHash,
                      byte[] signature, byte[] publicKey) {
        this.nodeId    = nodeId;
        this.timestamp = timestamp;
        this.prevHash  = prevHash;
        this.signature = signature != null ? signature.clone() : new byte[0];
        this.publicKey = publicKey != null ? publicKey.clone() : new byte[0];
    }

    /** Canonical data string used for both signing and hashing. */
    private String canonicalData() {
        return nodeId + "|" + timestamp + "|" + prevHash;
    }

    /** SHA-256 hex digest of {@link #canonicalData()}. */
    public String computeHash() {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(
                    canonicalData().getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(dig.length * 2);
            for (byte b : dig) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("SHA-256 unavailable", e);
        }
    }

    /**
     * Verifies the Ed25519 signature against this entry's
     * {@link #publicKey} and {@link #canonicalData()}.
     *
     * @return {@code true} only if the signature is cryptographically valid
     */
    public boolean verify() {
        try {
            KeyFactory kf = KeyFactory.getInstance("Ed25519");
            PublicKey pk = kf.generatePublic(
                    new X509EncodedKeySpec(publicKey));
            Signature verifier = Signature.getInstance("Ed25519");
            verifier.initVerify(pk);
            verifier.update(canonicalData().getBytes(StandardCharsets.UTF_8));
            return verifier.verify(signature);
        } catch (GeneralSecurityException e) {
            return false;
        }
    }

    public String getNodeId()    { return nodeId; }
    public long   getTimestamp() { return timestamp; }
    public String getPrevHash()  { return prevHash; }
    public byte[] getSignature() { return signature.clone(); }
    public byte[] getPublicKey() { return publicKey.clone(); }
}
