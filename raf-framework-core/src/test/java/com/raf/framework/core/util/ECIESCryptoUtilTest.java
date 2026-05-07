package com.raf.framework.core.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Tests for ECIESCryptoUtil (ECDH key exchange, AES-GCM, ECDSA signing).
 *
 * @author Jerry
 * @since 2026-04-29
 */
class ECIESCryptoUtilTest {

    private static KeyPair aliceKeyPair;
    private static KeyPair bobKeyPair;

    @BeforeAll
    static void setUp() throws Exception {
        aliceKeyPair = ECIESCryptoUtil.generateKeyPair();
        bobKeyPair = ECIESCryptoUtil.generateKeyPair();
    }

    // ─── generateKeyPair ─────────────────────────────────────────────────────

    @Test
    void generateKeyPair_returnsNonNullKeyPair() throws Exception {
        KeyPair kp = ECIESCryptoUtil.generateKeyPair();
        Assertions.assertNotNull(kp);
        Assertions.assertNotNull(kp.getPublic());
        Assertions.assertNotNull(kp.getPrivate());
    }

    // ─── keyToString / getPublicKey / getPrivateKey ──────────────────────────

    @Test
    void keyToString_returnsBase64String() {
        String pubKeyStr = ECIESCryptoUtil.keyToString(aliceKeyPair.getPublic());
        Assertions.assertNotNull(pubKeyStr);
        Assertions.assertFalse(pubKeyStr.isEmpty());
    }

    @Test
    void getPublicKey_roundTrip() throws Exception {
        String pubKeyStr = ECIESCryptoUtil.keyToString(aliceKeyPair.getPublic());
        PublicKey restored = ECIESCryptoUtil.getPublicKey(pubKeyStr);
        Assertions.assertEquals(aliceKeyPair.getPublic(), restored);
    }

    @Test
    void getPrivateKey_roundTrip() throws Exception {
        String privKeyStr = ECIESCryptoUtil.keyToString(aliceKeyPair.getPrivate());
        PrivateKey restored = ECIESCryptoUtil.getPrivateKey(privKeyStr);
        Assertions.assertEquals(aliceKeyPair.getPrivate(), restored);
    }

    // ─── computeSharedSecret ─────────────────────────────────────────────────

    @Test
    void computeSharedSecret_aliceAndBobDeriveTheSameKey() throws Exception {
        byte[] aliceShared = ECIESCryptoUtil.computeSharedSecret(
                aliceKeyPair.getPrivate(), bobKeyPair.getPublic());
        byte[] bobShared = ECIESCryptoUtil.computeSharedSecret(
                bobKeyPair.getPrivate(), aliceKeyPair.getPublic());

        Assertions.assertArrayEquals(aliceShared, bobShared);
    }

    @Test
    void computeSharedSecret_returns32Bytes() throws Exception {
        byte[] shared = ECIESCryptoUtil.computeSharedSecret(
                aliceKeyPair.getPrivate(), bobKeyPair.getPublic());
        Assertions.assertEquals(32, shared.length);
    }

    // ─── encryptAES / decryptAES ─────────────────────────────────────────────

    @Test
    void encryptDecryptAES_roundTrip() throws Exception {
        byte[] sharedKey = ECIESCryptoUtil.computeSharedSecret(
                aliceKeyPair.getPrivate(), bobKeyPair.getPublic());

        String plainText = "Hello, ECIES!";
        String encrypted = ECIESCryptoUtil.encryptAES(plainText, sharedKey);
        String decrypted = ECIESCryptoUtil.decryptAES(encrypted, sharedKey);

        Assertions.assertEquals(plainText, decrypted);
    }

    @Test
    void encryptAES_returnsDifferentCiphertextEachTime() throws Exception {
        byte[] sharedKey = ECIESCryptoUtil.computeSharedSecret(
                aliceKeyPair.getPrivate(), bobKeyPair.getPublic());

        String enc1 = ECIESCryptoUtil.encryptAES("same", sharedKey);
        String enc2 = ECIESCryptoUtil.encryptAES("same", sharedKey);

        Assertions.assertNotEquals(enc1, enc2);
    }

    // ─── sign / verify ───────────────────────────────────────────────────────

    @Test
    void signAndVerify_validSignatureReturnsTrue() throws Exception {
        String content = "important message";
        String sig = ECIESCryptoUtil.sign(content, aliceKeyPair.getPrivate());

        boolean valid = ECIESCryptoUtil.verify(content, sig, aliceKeyPair.getPublic());

        Assertions.assertTrue(valid);
    }

    @Test
    void verify_returnsFalseForTamperedContent() throws Exception {
        String content = "original message";
        String sig = ECIESCryptoUtil.sign(content, aliceKeyPair.getPrivate());

        boolean valid = ECIESCryptoUtil.verify("tampered message", sig, aliceKeyPair.getPublic());

        Assertions.assertFalse(valid);
    }

    @Test
    void verify_returnsFalseForWrongPublicKey() throws Exception {
        String content = "message";
        String sig = ECIESCryptoUtil.sign(content, aliceKeyPair.getPrivate());

        boolean valid = ECIESCryptoUtil.verify(content, sig, bobKeyPair.getPublic());

        Assertions.assertFalse(valid);
    }

    @Test
    void verify_returnsFalseForInvalidSignature() {
        boolean valid = ECIESCryptoUtil.verify("content", "invalidsig", aliceKeyPair.getPublic());
        Assertions.assertFalse(valid);
    }
}
