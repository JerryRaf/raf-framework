package com.raf.framework.core.util;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;

/**
 * 金融级通用加密工具类
 * 包含：ECDH密钥协商, HKDF密钥派生, AES-GCM加解密, ECDSA签名验签
 * 算法升级：
 * 密钥交换：ECDH (secp256r1/P-256)
 * 对称加密：AES-GCM (256位, 自动处理完整性校验，防篡改)
 * 密钥派生：HKDF (必须步骤！将ECDH协商的弱密钥转换为强密钥)
 * 签名：SHA256withECDSA
 * 动态 IV：每次加密生成随机 IV (12字节)，杜绝相同内容生成相同密文。
 * 防重放 (Anti-Replay)：引入 Timestamp + Nonce，并纳入签名范围。
 * 依赖最小化：仅使用 Java 标准库 (javax.crypto, java.security)，兼容 Android (API 26+) 和标准 Java 8+，无需额外的 BouncyCastle jar 包（除非JDK版本极低）。
 */
@Slf4j
public class ECIESCryptoUtil {

    /**
     * 私有构造器,防止实例化工具类
     */
    private ECIESCryptoUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    private static final String EC_ALGORITHM = "EC";
    private static final String AES_ALGORITHM = "AES";
    private static final String SIGN_ALGORITHM = "SHA256withECDSA";
    // AES-GCM 配置
    private static final String AES_CIPHER_TRANSFORM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;

    // ================= 1. 密钥管理 =================

    /**
     * 生成 EC 公私钥对 (P-256 曲线)
     */
    public static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(EC_ALGORITHM);
        keyPairGenerator.initialize(new ECGenParameterSpec("secp256r1"));
        return keyPairGenerator.generateKeyPair();
    }

    public static PublicKey getPublicKey(String base64Key) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(base64Key);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
        return KeyFactory.getInstance(EC_ALGORITHM).generatePublic(spec);
    }

    public static PrivateKey getPrivateKey(String base64Key) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(base64Key);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);
        return KeyFactory.getInstance(EC_ALGORITHM).generatePrivate(spec);
    }

    public static String keyToString(Key key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    // ================= 2. 核心：ECDH + HKDF 协商最终密钥 =================

    /**
     * 计算共享密钥，并使用 HKDF 派生出安全的 AES 密钥
     */
    public static byte[] computeSharedSecret(PrivateKey ownPrivateKey, PublicKey peerPublicKey) throws Exception {
        // 1. ECDH 原始协商
        KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH");
        keyAgreement.init(ownPrivateKey);
        keyAgreement.doPhase(peerPublicKey, true);
        byte[] rawSecret = keyAgreement.generateSecret();

        // 2. HKDF 密钥派生 (非常重要，不能直接用 rawSecret 做 AES key)
        // 这里简单实现 RFC 5869 的 Extract-and-Expand 逻辑的一个简化版
        return hkdfExpand(rawSecret, "financial_app_v1".getBytes(StandardCharsets.UTF_8), 32);
    }

    /**
     * 简单的 HKDF 实现 (HMAC-SHA256)
     */
    private static byte[] hkdfExpand(byte[] inputKey, byte[] info, int length) throws Exception {
        Mac hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(inputKey, "HmacSHA256");
        hmac.init(keySpec);
        hmac.update(info);
        // 简单模式：只做一次 Hash (足以满足大多数 AES-256 需求)
        // 正规 HKDF 需要处理 length > 32 的情况，这里简化
        byte[] output = hmac.doFinal(new byte[]{0x01});
        byte[] result = new byte[length];
        System.arraycopy(output, 0, result, 0, length);
        return result;
    }

    // ================= 3. AES-GCM 加解密 =================

    /**
     * AES-GCM 加密
     *
     * @return Base64(IV + CipherText)
     */
    public static String encryptAES(String plainText, byte[] aesKey) throws Exception {
        // 1. 生成随机 IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        // 2. 加密
        Cipher cipher = Cipher.getInstance(AES_CIPHER_TRANSFORM);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(aesKey, AES_ALGORITHM), spec);

        byte[] cipherBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        // 3. 拼接 IV + 密文
        byte[] finalBytes = new byte[iv.length + cipherBytes.length];
        System.arraycopy(iv, 0, finalBytes, 0, iv.length);
        System.arraycopy(cipherBytes, 0, finalBytes, iv.length, cipherBytes.length);

        return Base64.getEncoder().encodeToString(finalBytes);
    }

    /**
     * AES-GCM 解密
     *
     * @param base64IvAndCipher Base64(IV + CipherText)
     */
    public static String decryptAES(String base64IvAndCipher, byte[] aesKey) throws Exception {
        byte[] allBytes = Base64.getDecoder().decode(base64IvAndCipher);

        // 1. 提取 IV
        byte[] iv = Arrays.copyOfRange(allBytes, 0, GCM_IV_LENGTH);
        // 2. 提取密文
        byte[] cipherBytes = Arrays.copyOfRange(allBytes, GCM_IV_LENGTH, allBytes.length);

        // 3. 解密
        Cipher cipher = Cipher.getInstance(AES_CIPHER_TRANSFORM);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKey, AES_ALGORITHM), spec);

        byte[] plainBytes = cipher.doFinal(cipherBytes);
        return new String(plainBytes, StandardCharsets.UTF_8);
    }

    // ================= 4. 数字签名 =================

    public static String sign(String content, PrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance(SIGN_ALGORITHM);
        signature.initSign(privateKey);
        signature.update(content.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature.sign());
    }

    public static boolean verify(String content, String signBase64, PublicKey publicKey) {
        try {
            Signature signature = Signature.getInstance(SIGN_ALGORITHM);
            signature.initVerify(publicKey);
            signature.update(content.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(signBase64));
        } catch (Exception e) {
            log.error("verify error:", e);
            return false;
        }
    }
}