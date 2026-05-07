package com.raf.framework.core.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Base64;

/**
 * Tests for CryptoUtil (AES-GCM, SHA-256, HMAC-SHA256).
 *
 * @author Jerry
 * @since 2026-04-29
 */
class CryptoUtilTest {

    /** 256-bit AES key (32 bytes) encoded as Base64 */
    private static final String TEST_KEY_256 =
            Base64.getEncoder().encodeToString(new byte[32]); // 32 zero bytes

    /** A valid HMAC key */
    private static final String HMAC_KEY =
            Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());

    // ─── aesEncrypt / aesDecrypt ─────────────────────────────────────────────

    @Test
    void aesEncryptDecrypt_roundTrip() {
        String plainText = "Hello, World!";
        String encrypted = CryptoUtil.aesEncrypt(plainText, TEST_KEY_256);

        Assertions.assertNotNull(encrypted);
        Assertions.assertNotEquals(plainText, encrypted);

        String decrypted = CryptoUtil.aesDecrypt(encrypted, TEST_KEY_256);
        Assertions.assertEquals(plainText, decrypted);
    }

    @Test
    void aesEncrypt_returnsDifferentCiphertextEachTime() {
        String plainText = "same input";
        String enc1 = CryptoUtil.aesEncrypt(plainText, TEST_KEY_256);
        String enc2 = CryptoUtil.aesEncrypt(plainText, TEST_KEY_256);
        // GCM uses random IV, so ciphertexts should differ
        Assertions.assertNotEquals(enc1, enc2);
    }

    @Test
    void aesEncrypt_returnsNullForNullInput() {
        Assertions.assertNull(CryptoUtil.aesEncrypt(null, TEST_KEY_256));
    }

    @Test
    void aesEncrypt_returnsNullForEmptyInput() {
        Assertions.assertNull(CryptoUtil.aesEncrypt("", TEST_KEY_256));
    }

    @Test
    void aesEncrypt_throwsForNullKey() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> CryptoUtil.aesEncrypt("text", null));
    }

    @Test
    void aesEncrypt_throwsForEmptyKey() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> CryptoUtil.aesEncrypt("text", ""));
    }

    @Test
    void aesDecrypt_returnsNullForNullInput() {
        Assertions.assertNull(CryptoUtil.aesDecrypt(null, TEST_KEY_256));
    }

    @Test
    void aesDecrypt_returnsNullForEmptyInput() {
        Assertions.assertNull(CryptoUtil.aesDecrypt("", TEST_KEY_256));
    }

    @Test
    void aesDecrypt_throwsForNullKey() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> CryptoUtil.aesDecrypt("somecipher", null));
    }

    @Test
    void aesDecrypt_throwsForTooShortCiphertext() {
        // Base64 of 5 bytes (less than IV(12) + Tag(16) = 28 bytes minimum)
        String shortCipher = Base64.getEncoder().encodeToString(new byte[5]);
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> CryptoUtil.aesDecrypt(shortCipher, TEST_KEY_256));
    }

    @Test
    void aesDecrypt_throwsForWrongKey() {
        String encrypted = CryptoUtil.aesEncrypt("secret", TEST_KEY_256);
        String wrongKey = Base64.getEncoder().encodeToString(new byte[32]); // same length but different
        // Actually same bytes here, use a different key
        byte[] wrongKeyBytes = new byte[32];
        wrongKeyBytes[0] = 1;
        String differentKey = Base64.getEncoder().encodeToString(wrongKeyBytes);

        Assertions.assertThrows(SecurityException.class,
                () -> CryptoUtil.aesDecrypt(encrypted, differentKey));
    }

    // ─── sha256 ──────────────────────────────────────────────────────────────

    @Test
    void sha256_returnsConsistentHash() {
        String hash1 = CryptoUtil.sha256("hello");
        String hash2 = CryptoUtil.sha256("hello");
        Assertions.assertEquals(hash1, hash2);
    }

    @Test
    void sha256_returnsDifferentHashForDifferentInput() {
        Assertions.assertNotEquals(CryptoUtil.sha256("hello"), CryptoUtil.sha256("world"));
    }

    @Test
    void sha256_returnsNullForNull() {
        Assertions.assertNull(CryptoUtil.sha256(null));
    }

    @Test
    void sha256_returns64CharHexString() {
        String hash = CryptoUtil.sha256("test");
        Assertions.assertNotNull(hash);
        Assertions.assertEquals(64, hash.length());
        Assertions.assertTrue(hash.matches("[0-9a-f]+"));
    }

    // ─── hmacSha256 ──────────────────────────────────────────────────────────

    @Test
    void hmacSha256_returnsConsistentSignature() {
        String sig1 = CryptoUtil.hmacSha256("message", HMAC_KEY);
        String sig2 = CryptoUtil.hmacSha256("message", HMAC_KEY);
        Assertions.assertEquals(sig1, sig2);
    }

    @Test
    void hmacSha256_returnsDifferentSignatureForDifferentMessage() {
        String sig1 = CryptoUtil.hmacSha256("msg1", HMAC_KEY);
        String sig2 = CryptoUtil.hmacSha256("msg2", HMAC_KEY);
        Assertions.assertNotEquals(sig1, sig2);
    }

    @Test
    void hmacSha256_returnsDifferentSignatureForDifferentKey() {
        String key2 = Base64.getEncoder().encodeToString("differentkey1234differentkey1234".getBytes());
        String sig1 = CryptoUtil.hmacSha256("message", HMAC_KEY);
        String sig2 = CryptoUtil.hmacSha256("message", key2);
        Assertions.assertNotEquals(sig1, sig2);
    }

    @Test
    void hmacSha256_returnsNonNullBase64String() {
        String sig = CryptoUtil.hmacSha256("data", HMAC_KEY);
        Assertions.assertNotNull(sig);
        // Should be valid Base64
        Assertions.assertDoesNotThrow(() -> Base64.getDecoder().decode(sig));
    }
}
