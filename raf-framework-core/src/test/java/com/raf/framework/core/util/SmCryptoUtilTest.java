package com.raf.framework.core.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Base64;

/**
 * Tests for SmCryptoUtil (SM4-GCM, SM3, HMAC-SM3).
 * Requires BouncyCastle provider (bcprov-jdk18on, optional dependency).
 *
 * @author Jerry
 * @since 2026-04-29
 */
class SmCryptoUtilTest {

    /** Valid 128-bit SM4 key (16 bytes) encoded as Base64 */
    private static final String TEST_SM4_KEY =
            Base64.getEncoder().encodeToString(new byte[16]);

    /** HMAC key (any length is fine for HMAC-SM3) */
    private static final String HMAC_KEY =
            Base64.getEncoder().encodeToString("0123456789abcdef".getBytes());

    // ─── generateKey ─────────────────────────────────────────────────────────

    @Test
    void generateKey_returns16ByteBase64Key() {
        String key = SmCryptoUtil.generateKey();
        Assertions.assertNotNull(key);
        byte[] decoded = Base64.getDecoder().decode(key);
        Assertions.assertEquals(16, decoded.length);
    }

    // ─── sm4Encrypt / sm4Decrypt ─────────────────────────────────────────────

    @Test
    void sm4EncryptDecrypt_roundTrip() {
        String plainText = "Hello, SM4!";
        String encrypted = SmCryptoUtil.sm4Encrypt(plainText, TEST_SM4_KEY);

        Assertions.assertNotNull(encrypted);
        Assertions.assertNotEquals(plainText, encrypted);

        String decrypted = SmCryptoUtil.sm4Decrypt(encrypted, TEST_SM4_KEY);
        Assertions.assertEquals(plainText, decrypted);
    }

    @Test
    void sm4Encrypt_returnsDifferentCiphertextEachTime() {
        String enc1 = SmCryptoUtil.sm4Encrypt("same", TEST_SM4_KEY);
        String enc2 = SmCryptoUtil.sm4Encrypt("same", TEST_SM4_KEY);
        Assertions.assertNotEquals(enc1, enc2);
    }

    @Test
    void sm4Encrypt_returnsNullForNullInput() {
        Assertions.assertNull(SmCryptoUtil.sm4Encrypt(null, TEST_SM4_KEY));
    }

    @Test
    void sm4Encrypt_returnsNullForEmptyInput() {
        Assertions.assertNull(SmCryptoUtil.sm4Encrypt("", TEST_SM4_KEY));
    }

    @Test
    void sm4Encrypt_throwsForInvalidKeyLength() {
        // 8 bytes key (not 16)
        String shortKey = Base64.getEncoder().encodeToString(new byte[8]);
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> SmCryptoUtil.sm4Encrypt("text", shortKey));
    }

    @Test
    void sm4Decrypt_returnsNullForNullInput() {
        Assertions.assertNull(SmCryptoUtil.sm4Decrypt(null, TEST_SM4_KEY));
    }

    @Test
    void sm4Decrypt_returnsNullForEmptyInput() {
        Assertions.assertNull(SmCryptoUtil.sm4Decrypt("", TEST_SM4_KEY));
    }

    @Test
    void sm4Decrypt_throwsForWrongKey() {
        String encrypted = SmCryptoUtil.sm4Encrypt("secret", TEST_SM4_KEY);
        byte[] wrongKeyBytes = new byte[16];
        wrongKeyBytes[0] = 1;
        String wrongKey = Base64.getEncoder().encodeToString(wrongKeyBytes);

        Assertions.assertThrows(SecurityException.class,
                () -> SmCryptoUtil.sm4Decrypt(encrypted, wrongKey));
    }

    // ─── sm3 ─────────────────────────────────────────────────────────────────

    @Test
    void sm3_returnsConsistentHash() {
        String h1 = SmCryptoUtil.sm3("hello");
        String h2 = SmCryptoUtil.sm3("hello");
        Assertions.assertEquals(h1, h2);
    }

    @Test
    void sm3_returnsDifferentHashForDifferentInput() {
        Assertions.assertNotEquals(SmCryptoUtil.sm3("hello"), SmCryptoUtil.sm3("world"));
    }

    @Test
    void sm3_returnsNullForNull() {
        Assertions.assertNull(SmCryptoUtil.sm3(null));
    }

    @Test
    void sm3_returns64CharHexString() {
        String hash = SmCryptoUtil.sm3("test");
        Assertions.assertNotNull(hash);
        Assertions.assertEquals(64, hash.length());
        Assertions.assertTrue(hash.matches("[0-9a-f]+"));
    }

    // ─── hmacSm3 ─────────────────────────────────────────────────────────────

    @Test
    void hmacSm3_returnsConsistentSignature() {
        String sig1 = SmCryptoUtil.hmacSm3("message", HMAC_KEY);
        String sig2 = SmCryptoUtil.hmacSm3("message", HMAC_KEY);
        Assertions.assertEquals(sig1, sig2);
    }

    @Test
    void hmacSm3_returnsDifferentSignatureForDifferentMessage() {
        String sig1 = SmCryptoUtil.hmacSm3("msg1", HMAC_KEY);
        String sig2 = SmCryptoUtil.hmacSm3("msg2", HMAC_KEY);
        Assertions.assertNotEquals(sig1, sig2);
    }

    @Test
    void hmacSm3_returnsNonNullBase64String() {
        String sig = SmCryptoUtil.hmacSm3("data", HMAC_KEY);
        Assertions.assertNotNull(sig);
        Assertions.assertDoesNotThrow(() -> Base64.getDecoder().decode(sig));
    }
}
