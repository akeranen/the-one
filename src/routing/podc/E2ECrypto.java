package routing.podc;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

/**
 * End-to-end encryption using X25519 ECDH key agreement + AES-256-GCM.
 *
 * <p>In PoDC the X25519 public key <b>is</b> the node address — every
 * node knows every other node's public key by definition (like a
 * wallet address in crypto).  No key-exchange protocol is needed.</p>
 *
 * <pre>
 *   shared = X25519(privSender, pubRecipient)
 *         == X25519(privRecipient, pubSender)
 *   key   = SHA-256(shared)
 *   ct    = AES-256-GCM(key, iv, plaintext)
 * </pre>
 */
public final class E2ECrypto {

    private static final int GCM_TAG_BITS = 128;
    private static final int IV_BYTES     = 12;

    private E2ECrypto() {}

    public static KeyPair generateKeyPair() {
        try {
            return KeyPairGenerator.getInstance("X25519").generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new AssertionError("X25519 unavailable", e);
        }
    }

    public static SecretKeySpec deriveKey(PrivateKey myPrivate,
                                          byte[] otherPublicEnc) {
        try {
            KeyFactory kf = KeyFactory.getInstance("X25519");
            PublicKey otherPub = kf.generatePublic(
                    new X509EncodedKeySpec(otherPublicEnc));
            KeyAgreement ka = KeyAgreement.getInstance("X25519");
            ka.init(myPrivate);
            ka.doPhase(otherPub, true);
            byte[] shared = ka.generateSecret();
            byte[] aesKey = MessageDigest.getInstance("SHA-256").digest(shared);
            return new SecretKeySpec(aesKey, "AES");
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("ECDH key derivation failed", e);
        }
    }

    /** @return iv (12 B) || ciphertext+GCM-tag */
    public static byte[] encrypt(SecretKeySpec key, byte[] plaintext) {
        try {
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] iv = new byte[IV_BYTES];
            SecureRandom.getInstanceStrong().nextBytes(iv);
            c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ct = c.doFinal(plaintext);
            byte[] out = new byte[IV_BYTES + ct.length];
            System.arraycopy(iv, 0, out, 0, IV_BYTES);
            System.arraycopy(ct, 0, out, IV_BYTES, ct.length);
            return out;
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("AES-GCM encrypt failed", e);
        }
    }

    /** @return plaintext, or null if tag verification fails */
    public static byte[] decrypt(SecretKeySpec key, byte[] packed) {
        try {
            if (packed == null || packed.length < IV_BYTES + GCM_TAG_BITS / 8)
                return null;
            byte[] iv = Arrays.copyOfRange(packed, 0, IV_BYTES);
            byte[] ct = Arrays.copyOfRange(packed, IV_BYTES, packed.length);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return c.doFinal(ct);
        } catch (GeneralSecurityException e) {
            return null;
        }
    }
}
