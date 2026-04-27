package com.raf.framework.core.util;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Arrays;
import java.util.Base64;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * 国密(GM) 核心加密/哈希工具类
 * <p>
 * 依赖: Bouncy Castle (bcprov-jdk15to18)
 * 1. SM4-GCM (128位密钥): 用于敏感数据加密，包含 IV 和 Tag，防篡改。
 * 2. SM3 消息: 用于数据摘要 (类似 SHA-256)。
 * 3. HMAC-SM3: 用于接口签名。
 *
 * @author Jerry
 */
public class SmCryptoUtil {

    /**
     * 私有构造器，防止实例化工具类
     */
    private SmCryptoUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    // 注册 BouncyCastle Provider
    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private static final String PROVIDER_NAME = "BC";

    // SM4 GCM 模式: 需要 BC 库支持
    private static final String SM4_ALGORITHM = "SM4/GCM/NoPadding";
    private static final String KEY_ALGORITHM = "SM4";

    // GCM 推荐 IV 长度 12字节
    private static final int GCM_IV_LENGTH = 12;
    // GCM Tag 长度 128位
    private static final int GCM_TAG_LENGTH = 128;

    private static final String HASH_SM3 = "SM3";
    private static final String HMAC_SM3 = "HmacSM3";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 使用 SM4-GCM 算法加密字符串
     * <p>
     * 格式: Base64( IV(12byte) + CipherText + Tag )
     *
     * @param plainText 待加密的明文
     * @param base64Key Base64编码的SM4密钥（必须是 128位 / 16字节）
     * @return 加密后的Base64字符串
     */
    public static String sm4Encrypt(String plainText, String base64Key) {
        if (plainText == null || plainText.isEmpty()) {
            return null;
        }

        // 先验证密钥，IllegalArgumentException 直接传播不被包装
        SecretKey key = loadKey(base64Key);

        try {
            // 2. 生成随机 IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);

            // 3. 初始化 Cipher (指定 BC Provider)
            Cipher cipher = Cipher.getInstance(SM4_ALGORITHM, PROVIDER_NAME);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);

            // 4. 执行加密
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // 5. 拼接: IV + 密文(包含Tag)
            byte[] finalMessage = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, finalMessage, 0, iv.length);
            System.arraycopy(cipherText, 0, finalMessage, iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(finalMessage);
        } catch (Exception e) {
            throw new SecurityException("SM4 Encryption failed", e);
        }
    }

    /**
     * 使用 SM4-GCM 算法解密字符串
     *
     * @param base64CipherText 加密后的Base64字符串
     * @param base64Key        Base64编码的SM4密钥
     * @return 解密后的明文
     */
    public static String sm4Decrypt(String base64CipherText, String base64Key) {
        try {
            if (base64CipherText == null || base64CipherText.isEmpty()) {
                return null;
            }

            byte[] decodedBytes = Base64.getDecoder().decode(base64CipherText);

            if (decodedBytes.length < GCM_IV_LENGTH) {
                throw new IllegalArgumentException("Invalid ciphertext format");
            }

            // 1. 提取 IV 和 密文
            byte[] iv = Arrays.copyOfRange(decodedBytes, 0, GCM_IV_LENGTH);
            byte[] cipherText = Arrays.copyOfRange(decodedBytes, GCM_IV_LENGTH, decodedBytes.length);

            // 2. 准备密钥
            SecretKey key = loadKey(base64Key);

            // 3. 初始化 Cipher
            Cipher cipher = Cipher.getInstance(SM4_ALGORITHM, PROVIDER_NAME);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);

            // 4. 解密
            byte[] plainTextBytes = cipher.doFinal(cipherText);
            return new String(plainTextBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new SecurityException("SM4 Decryption failed", e);
        }
    }

    /**
     * 计算字符串的 SM3 摘要 (256位)
     *
     * @param text 原文
     * @return 16进制表示的摘要字符串
     */
    public static String sm3(String text) {
        try {
            if (text == null) {
                return null;
            }
            MessageDigest digest = MessageDigest.getInstance(HASH_SM3, PROVIDER_NAME);
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("SM3 calculation failed", e);
        }
    }

    /**
     * 计算 HMAC-SM3 签名
     *
     * @param text      需要签名的内容
     * @param base64Key 签名密钥
     * @return Base64编码的签名串
     */
    public static String hmacSm3(String text, String base64Key) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64Key);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, HMAC_SM3);

            Mac mac = Mac.getInstance(HMAC_SM3, PROVIDER_NAME);
            mac.init(secretKey);

            byte[] rawHmac = mac.doFinal(text.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new SecurityException("HMAC-SM3 calculation failed", e);
        }
    }

    private static SecretKey loadKey(String base64Key) {
        byte[] decodedKey = Base64.getDecoder().decode(base64Key);
        // SM4 密钥必须是 128位 (16字节)
        if (decodedKey.length != 16) {
            throw new IllegalArgumentException("SM4 key must be 128 bits (16 bytes)");
        }
        return new SecretKeySpec(decodedKey, KEY_ALGORITHM);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 辅助工具：生成一个随机的 SM4 密钥 (Base64格式)
     * 128位
     */
    public static String generateKey() {
        byte[] key = new byte[16];
        SECURE_RANDOM.nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
}