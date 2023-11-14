package com.raf.framework.autoconfigure.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * @author Jerry
 * @date 2020/08/20
 * 密码随机盐 2a代表算法版本，10是代价因子（默认是 10，代表2的10次方次哈希），22个字符是盐，再后面是摘要
 */
public class CryptPasswordUtil {
    private static final BCryptPasswordEncoder SINGLETON = new BCryptPasswordEncoder();

    private CryptPasswordUtil() {
    }

    public static BCryptPasswordEncoder getSingleton() {
        return SINGLETON;
    }
}
