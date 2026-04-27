package com.raf.framework.core.util;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * 核心加密/哈希工具类
 * <p>
 * 所有密钥均通过参数传入，支持从外部存储（DB/Nacos/KMS）动态加载
 * 1. AES-GCM (256位推荐): 用于敏感数据加密 (手机号、身份证等),每次加密结果都不一样
 * 2. SHA-256 消息: 用于数据摘要指纹
 * 3. HMAC-SHA256 消息+秘钥: 用于接口签名，敏感-熵低盲索引(防止彩虹表)
 *
 * @author Jerry
 * @date 2023/03/01
 */
public class CryptoUtil {

    /**
     * 私有构造器,防止实例化工具类
     */
    private CryptoUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final String HASH_SHA256 = "SHA-256";
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 使用 AES-GCM 算法加密字符串
     * <p>
     * 该方法会自动生成随机的初始向量（IV），并将其拼接到密文的头部。
     * 最终返回格式为：Base64( IV + CipherText + Tag )
     *
     * @param plainText 待加密的明文
     * @param base64Key Base64编码的AES密钥（建议256位）
     * @return 加密后的Base64字符串
     * @throws IllegalArgumentException 如果密钥为空或格式不正确
     */
    public static String aesEncrypt(String plainText, String base64Key) {
        if (plainText == null || plainText.isEmpty()) {
            return null;
        }
        if (base64Key == null || base64Key.trim().isEmpty()) {
            throw new IllegalArgumentException("AES key cannot be null or empty");
        }

        try {
            SecretKey key = loadKey(base64Key);
            byte[] iv = new byte[GCM_IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // 优化：直接在一次操作中创建最终结果
            byte[] finalMessage = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, finalMessage, 0, iv.length);
            System.arraycopy(cipherText, 0, finalMessage, iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(finalMessage);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new SecurityException("AES Encryption failed", e);
        }
    }

    /**
     * 使用 AES-GCM 算法解密字符串
     * <p>
     * 该方法会自动从密文头部提取 IV，用于解密验证。
     * 如果密钥错误或数据被篡改，将抛出异常。
     *
     * @param base64CipherText 加密后的Base64字符串
     * @param base64Key        Base64编码的AES密钥
     * @return 解密后的明文
     * @throws IllegalArgumentException 如果密文或密钥格式不正确
     */
    public static String aesDecrypt(String base64CipherText, String base64Key) {
        if (base64CipherText == null || base64CipherText.isEmpty()) {
            return null;
        }
        if (base64Key == null || base64Key.trim().isEmpty()) {
            throw new IllegalArgumentException("AES key cannot be null or empty");
        }

        try {
            byte[] decodedBytes = Base64.getDecoder().decode(base64CipherText);

            // GCM_TAG_LENGTH/8 是 Tag 的字节长度 (128/8 = 16)
            if (decodedBytes.length < GCM_IV_LENGTH + (GCM_TAG_LENGTH / 8)) {
                throw new IllegalArgumentException("Invalid ciphertext: too short");
            }

            byte[] iv = Arrays.copyOfRange(decodedBytes, 0, GCM_IV_LENGTH);
            byte[] cipherText = Arrays.copyOfRange(decodedBytes, GCM_IV_LENGTH, decodedBytes.length);

            SecretKey key = loadKey(base64Key);
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);

            byte[] plainTextBytes = cipher.doFinal(cipherText);
            return new String(plainTextBytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new SecurityException("AES Decryption failed: data may be corrupted or key is incorrect", e);
        }
    }

    /**
     * 计算字符串的 SHA-256 摘要
     * <p>
     * 这是一个确定性算法，相同的输入必然产生相同的输出。
     * 适用于盲索引查询或文件完整性校验。
     *
     * @param text 原文
     * @return 16进制表示的摘要字符串
     */
    public static String sha256(String text) {
        try {
            if (text == null) {
                return null;
            }
            MessageDigest digest = MessageDigest.getInstance(HASH_SHA256);
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 calculation failed", e);
        }
    }

    /**
     * 计算 HMAC-SHA256 签名
     * <p>
     * 适用于 API 接口防篡改签名。
     *
     * @param text      需要签名的内容
     * @param base64Key 签名密钥
     * @return Base64编码的签名串
     */
    public static String hmacSha256(String text, String base64Key) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(Base64.getDecoder().decode(base64Key), HMAC_SHA256);
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(secretKey);
            byte[] rawHmac = mac.doFinal(text.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new SecurityException("HMAC calculation failed", e);
        }
    }

    private static SecretKey loadKey(String base64Key) {
        byte[] decodedKey = Base64.getDecoder().decode(base64Key);
        return new SecretKeySpec(decodedKey, "AES");
    }

    /**
     * 字节数组转16进制字符串（性能优化版本）
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}