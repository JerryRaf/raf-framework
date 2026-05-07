package com.raf.framework.kms.provider;

import com.raf.framework.kms.properties.KmsProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 本地 KMS 服务实现。
 * <p>
 * 适用于开发、测试或无法访问云 KMS 的环境。
 * 本地模式使用独立的 localKeys 配置，直接返回明文，不进行实际加解密。
 *
 * @author Jerry
 * @since 2026-05-06
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "raf.kms.provider", havingValue = "LOCAL", matchIfMissing = true)
public class LocalKmsService implements KmsService {

    @Override
    public Map<String, String> resolveKeys(KmsProperties properties) {
        if (properties.getLocalKeys().isEmpty()) {
            throw new IllegalArgumentException("[Local KMS] No localKeys found in configuration.");
        }
        log.info("Using LOCAL KMS Provider.");
        Map<String, String> resolvedKeys = new HashMap<>();
        for (Map.Entry<String, KmsProperties.KeyConfig> entry : properties.getLocalKeys().entrySet()) {
            String alias = entry.getKey();
            KmsProperties.KeyConfig keyConfig = entry.getValue();
            resolvedKeys.put(alias, keyConfig.getPlainText());
            log.info("Successfully loaded key for alias: {}", alias);
        }
        return resolvedKeys;
    }

    @Override
    public String decrypt(String keyId, String cipherText) {
        return cipherText;
    }

    @Override
    public String encrypt(String keyId, String plainText) {
        return plainText;
    }
}
