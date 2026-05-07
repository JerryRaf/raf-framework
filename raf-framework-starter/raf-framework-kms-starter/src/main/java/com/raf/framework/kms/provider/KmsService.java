package com.raf.framework.kms.provider;

import com.raf.framework.kms.properties.KmsProperties;
import java.util.Map;

/**
 * 统一 KMS 接口，支持多云提供商。
 *
 * @author Jerry
 * @since 2026-05-06
 */
public interface KmsService {

    /**
     * 根据配置校验并加载/解密所有密钥。
     *
     * @param properties KMS 配置
     * @return alias -> plainText 映射
     */
    Map<String, String> resolveKeys(KmsProperties properties);

    /**
     * 解密密文。
     *
     * @param keyId      KMS 密钥 ID
     * @param cipherText Base64 编码密文
     * @return 明文
     */
    String decrypt(String keyId, String cipherText);

    /**
     * 加密明文。
     *
     * @param keyId     KMS 密钥 ID
     * @param plainText 明文
     * @return Base64 编码密文
     */
    String encrypt(String keyId, String plainText);
}
