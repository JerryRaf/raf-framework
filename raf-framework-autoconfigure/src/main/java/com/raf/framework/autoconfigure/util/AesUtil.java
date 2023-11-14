package com.raf.framework.autoconfigure.util;

import cn.hutool.crypto.symmetric.AES;

/**
 * 对称算法AES-PKCS7Padding（默认jdk不支持需要引入bcprov-jdk18on）
 *
 * @author Jerry
 * @date 2023/03/01
 */
public class AesUtil {
    public static AES Aes(String secret, String iv) {
        return new AES("CBC", "PKCS7Padding",
                secret.getBytes(),
                iv.getBytes());
    }
}
