package com.raf.framework.kms.core;

import com.raf.framework.core.util.CryptoUtil;
import com.raf.framework.kms.properties.KmsProperties;
import com.raf.framework.kms.provider.KmsService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 核心加密管理器。
 * <p>
 * 职责：
 * 1. 协调 KMS 服务获取密钥
 * 2. 提供高性能的本地密钥缓存（ConcurrentHashMap，alias -> plainText）
 * 3. 对外提供统一的加解密 API（通过 String alias 操作，无业务枚举耦合）
 *
 * @author Jerry
 * @since 2026-05-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CryptoManager {

    private final KmsService kmsService;
    private final KmsProperties properties;

    /** 密钥缓存：alias -> plainText */
    private final Map<String, String> keyCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("Starting initialization of security keys...");
        String forceEmergency = getEnvironmentVariable("FORCE_EMERGENCY_MODE");
        if ("true".equalsIgnoreCase(forceEmergency)) {
            log.warn("!!! FORCE_EMERGENCY_MODE is enabled, loading keys from local-keys config !!!");
            loadFromLocalKeys();
            return;
        }
        loadFromKms();
    }

    private void loadFromLocalKeys() {
        if (properties.getLocalKeys().isEmpty()) {
            throw new RuntimeException("FORCE_EMERGENCY_MODE is enabled but local-keys is empty");
        }
        for (Map.Entry<String, KmsProperties.KeyConfig> entry : properties.getLocalKeys().entrySet()) {
            String alias = entry.getKey();
            String plainText = entry.getValue().getPlainText();
            if (StringUtils.isEmpty(plainText)) {
                throw new RuntimeException("Emergency mode: plainText is empty for alias: " + alias);
            }
            keyCache.put(alias, plainText);
        }
        log.info("Emergency mode: loaded {} keys from local-keys config", keyCache.size());
    }

    private void loadFromKms() {
        RetryTemplate retryTemplate = RetryTemplate.builder()
                .maxAttempts(2)
                .fixedBackoff(500)
                .retryOn(RuntimeException.class)
                .build();
        try {
            retryTemplate.execute(context -> {
                log.info("Attempting to resolve keys using provider: {} (Attempt: {})",
                        kmsService.getClass().getSimpleName(), context.getRetryCount() + 1);
                Map<String, String> resolvedKeys = kmsService.resolveKeys(properties);
                resolvedKeys.forEach((alias, plainText) -> {
                    if (StringUtils.isEmpty(plainText)) {
                        throw new IllegalStateException("Missing plainText for alias: " + alias);
                    }
                    keyCache.put(alias, plainText);
                });
                return null;
            });
            log.info("Successfully loaded and cached {} security keys.", keyCache.size());
        } catch (Exception e) {
            log.error("Critical failure: Unable to retrieve keys after retries.", e);
            throw new RuntimeException("KMS Unavailable", e);
        }
    }

    // ===== Business API =====

    public String encrypt(String plainText, String keyAlias) {
        if (StringUtils.isEmpty(plainText)) {
            return plainText;
        }
        return CryptoUtil.aesEncrypt(plainText, getKey(keyAlias));
    }

    public String decrypt(String cipherText, String keyAlias) {
        if (StringUtils.isEmpty(cipherText)) {
            return cipherText;
        }
        return CryptoUtil.aesDecrypt(cipherText, getKey(keyAlias));
    }

    public String generateIndex(String plainText, String keyAlias) {
        if (StringUtils.isEmpty(plainText)) {
            return null;
        }
        return CryptoUtil.hmacSha256(plainText, getKey(keyAlias));
    }

    public String getRawKey(String keyAlias) {
        return getKey(keyAlias);
    }

    private String getKey(String keyAlias) {
        String key = keyCache.get(keyAlias);
        if (key == null) {
            throw new IllegalStateException("Security Error: Key not initialized for alias: " + keyAlias);
        }
        return key;
    }

    public static String getEnvironmentVariable(String key) {
        String value = System.getProperty(key);
        if (StringUtils.isEmpty(value)) {
            value = System.getenv(key);
        }
        return value;
    }
}
