package com.raf.framework.autoconfigure.util;

import cn.hutool.crypto.asymmetric.SM2;
import cn.hutool.crypto.digest.SM3;
import cn.hutool.crypto.symmetric.SM4;

/**
 *
 * @author Jerry
 * @date 2023/10/09
 */
public class SMUtil {
    /**
     * 非对称加解密ECC
     * @param privateKey
     * @param publicKey
     * @return
     */
    public static SM2 sm2(String privateKey, String publicKey) {
        return new SM2(privateKey, publicKey);
    }

    /**
     * 摘要算法256位
     * @param data
     * @return
     */
    public static String sm3(String data) {
        return new SM3().digestHex(data);
    }

    /**
     * 对称加密128位
     * @param secret
     * @return
     */
    public static SM4 sm4(String secret) {
        return new SM4(secret.getBytes());
    }

}
