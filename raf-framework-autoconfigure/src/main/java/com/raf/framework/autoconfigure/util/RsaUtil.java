package com.raf.framework.autoconfigure.util;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import cn.hutool.crypto.asymmetric.Sign;
import cn.hutool.crypto.asymmetric.SignAlgorithm;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.Base64;

/**
 * rsa非对称加解密，签名
 *
 * @author Jerry
 * @date 2023/03/01
 */
public class RsaUtil {

    static String ALGORITHM = "RSA/ECB/PKCS1Padding";
    /**
     * 公私钥对生成
     *
     * @return
     */
    public static KeyPair generate() {
        return SecureUtil.generateKeyPair(ALGORITHM);
    }

    /**
     * 公钥加密
     *
     * @param publicKey
     * @param plain
     * @return
     */
    public static String encrypt(String publicKey, String plain) {
        RSA rsa = new RSA(ALGORITHM,null, publicKey);
        byte[] encrypt = rsa.encrypt(StrUtil.bytes(plain, CharsetUtil.CHARSET_UTF_8), KeyType.PublicKey);
        return bytesToBase64(encrypt);
    }

    /**
     * 私钥解密
     *
     * @param privateKey
     * @param cipher
     * @return
     */
    public static String decrypt(String privateKey, String cipher) {
        RSA rsa = new RSA(ALGORITHM,privateKey, null);
        byte[] decrypt = rsa.decrypt(cipher, KeyType.PrivateKey);
        return new String(decrypt, StandardCharsets.UTF_8);
    }

    /**
     * 密文私钥签名
     *
     * @param privateKey
     * @param cipher
     * @return
     */
    public static String sign(String privateKey, String cipher) {
        Sign sign = SecureUtil.sign(SignAlgorithm.SHA256withRSA, privateKey, null);
        byte[] data = cipher.getBytes(StandardCharsets.UTF_8);
        byte[] signed = sign.sign(data);
        return RsaUtil.bytesToBase64(signed);
    }

    /**
     * 密文公钥验签
     *
     * @param publicKey
     * @param cipher
     * @param originSign
     * @return
     */
    public static Boolean verifySign(String publicKey, String cipher, String originSign) {
        Sign sign = SecureUtil.sign(SignAlgorithm.SHA256withRSA, null, publicKey);
        byte[] data = cipher.getBytes(StandardCharsets.UTF_8);
        return sign.verify(data, RsaUtil.base64ToBytes(originSign));
    }

    /**
     * 字节数组转Base64编码
     *
     * @param bytes 字节数组
     * @return Base64编码
     */
    public static String bytesToBase64(byte[] bytes) {
        byte[] encodedBytes = Base64.getEncoder().encode(bytes);
        return new String(encodedBytes, StandardCharsets.UTF_8);
    }

    /**
     * Base64编码转字节数组
     *
     * @param base64Str Base64编码
     * @return 字节数组
     */
    public static byte[] base64ToBytes(String base64Str) {
        byte[] bytes = base64Str.getBytes(StandardCharsets.UTF_8);
        return Base64.getDecoder().decode(bytes);
    }
}
