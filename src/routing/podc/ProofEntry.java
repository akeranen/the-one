package routing.podc;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;

/**
 * A single record in a message's proof-of-delivery chain.
 *
 * The node identifier is self-certifying: it must equal
 * {@link #fingerprint(byte[])} of the embedded Ed25519 public key, so an
 * entry can only be produced by the holder of the matching private key.
 * Entries are linked via {@link #prevHash}; the first entry of a chain
 * links to the genesis value {@link #genesis(String, String, String)}
 * that binds the message id, the source and the destination.
 */
public class ProofEntry {

    private final String nodeId;
    private final long   timestamp;
    private final String prevHash;
    private final byte[] signature;
    private final byte[] publicKey;
    private transient String cachedHash;
    private transient Boolean cachedValid;

    public ProofEntry(String nodeId, long timestamp, String prevHash,
                      byte[] signature, byte[] publicKey) {
        this.nodeId    = nodeId;
        this.timestamp = timestamp;
        this.prevHash  = prevHash;
        this.signature = signature != null ? signature.clone() : new byte[0];
        this.publicKey = publicKey != null ? publicKey.clone() : new byte[0];
    }

    /** Creates and signs an entry for the holder of {@code priv}. */
    public static ProofEntry create(long timestamp, String prevHash,
                                    PrivateKey priv, byte[] pubEncoded) {
        String id = fingerprint(pubEncoded);
        byte[] sig = sign(priv, canonical(id, timestamp, prevHash));
        return new ProofEntry(id, timestamp, prevHash, sig, pubEncoded);
    }

    private static String canonical(String id, long ts, String prev) {
        return id + "|" + ts + "|" + prev;
    }

    private String canonicalData() {
        return canonical(nodeId, timestamp, prevHash);
    }

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    public static String sha256Hex(String s) {
        return sha256Hex(s.getBytes(StandardCharsets.UTF_8));
    }

    public static String sha256Hex(byte[] data) {
        try {
            byte[] dig = MessageDigest.getInstance("SHA-256").digest(data);
            char[] hex = new char[dig.length * 2];
            for (int i = 0; i < dig.length; i++) {
                int v = dig[i] & 0xFF;
                hex[i * 2]     = HEX[v >>> 4];
                hex[i * 2 + 1] = HEX[v & 0x0F];
            }
            return new String(hex);
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("SHA-256 unavailable", e);
        }
    }

    /** Node identifier derived from an X.509-encoded Ed25519 public key. */
    public static String fingerprint(byte[] pubEncoded) {
        return sha256Hex(pubEncoded).substring(0, 32);
    }

    /** Value the first entry of a chain must link to. */
    public static String genesis(String msgId, String srcId, String dstId) {
        return "G" + sha256Hex(msgId + "|" + srcId + "|" + dstId);
    }

    public String computeHash() {
        if (cachedHash == null) cachedHash = sha256Hex(canonicalData());
        return cachedHash;
    }

    /**
     * Checks the identity binding and the Ed25519 signature.
     * Verification is deterministic; the result is memoized because in
     * the simulator all copies of a message share this object.
     */
    public boolean verify() {
        if (cachedValid != null) return cachedValid;
        boolean ok = nodeId.equals(fingerprint(publicKey));
        if (ok) {
            try {
                PublicKey pk = KeyFactory.getInstance("Ed25519")
                        .generatePublic(new X509EncodedKeySpec(publicKey));
                Signature v = Signature.getInstance("Ed25519");
                v.initVerify(pk);
                v.update(canonicalData().getBytes(StandardCharsets.UTF_8));
                ok = v.verify(signature);
            } catch (GeneralSecurityException e) {
                ok = false;
            }
        }
        cachedValid = ok;
        return ok;
    }

    /** Entry that claims {@code claimedId} but is signed with another key. */
    public static ProofEntry spoofed(String claimedId, long timestamp,
                                     String prevHash, PrivateKey priv,
                                     byte[] pubEncoded) {
        byte[] sig = sign(priv, canonical(claimedId, timestamp, prevHash));
        return new ProofEntry(claimedId, timestamp, prevHash, sig, pubEncoded);
    }

    static byte[] sign(PrivateKey key, String data) {
        try {
            Signature s = Signature.getInstance("Ed25519");
            s.initSign(key);
            s.update(data.getBytes(StandardCharsets.UTF_8));
            return s.sign();
        } catch (GeneralSecurityException e) {
            throw new AssertionError("Ed25519 unavailable", e);
        }
    }

    public String getNodeId()    { return nodeId; }
    public long   getTimestamp() { return timestamp; }
    public String getPrevHash()  { return prevHash; }
    public byte[] getSignature() { return signature.clone(); }
    public byte[] getPublicKey() { return publicKey.clone(); }
}
