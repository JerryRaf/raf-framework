package com.raf.framework.kms.jasypt;

import com.raf.framework.kms.core.CryptoManager;
import com.raf.framework.kms.properties.KmsProperties;
import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * KMS 与 Jasypt 集成配置。
 * 将 KMS 解密后的指定 alias 密钥作为 Jasypt 的加密密码。
 * 通过 raf.kms.jasypt.enabled=true 显式启用。
 *
 * @author Jerry
 * @since 2026-05-06
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "raf.kms.jasypt.enabled", havingValue = "true")
@EnableEncryptableProperties
@EnableConfigurationProperties(KmsJasyptProperties.class)
@RequiredArgsConstructor
public class KmsJasyptConfig {

    private final CryptoManager cryptoManager;
    private final KmsProperties kmsProperties;
    private final KmsJasyptProperties jasyptProperties;

    @Bean("jasyptStringEncryptor")
    public StringEncryptor kmsStringEncryptor() {
        String keyAlias = kmsProperties.getJasypt().getKeyAlias();
        if (keyAlias == null || keyAlias.isBlank()) {
            throw new IllegalStateException("raf.kms.jasypt.key-alias must be configured when jasypt is enabled");
        }
        log.info("Configuring Jasypt encryptor using KMS key alias: {}", keyAlias);

        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        config.setPassword(cryptoManager.getRawKey(keyAlias));
        config.setAlgorithm(jasyptProperties.getAlgorithm());
        config.setIvGeneratorClassName(jasyptProperties.getIvGeneratorClassName());
        config.setKeyObtentionIterations(String.valueOf(jasyptProperties.getKeyObtentionIterations()));
        config.setPoolSize(String.valueOf(jasyptProperties.getPoolSize()));
        config.setProviderName(jasyptProperties.getProviderName());
        config.setSaltGeneratorClassName(jasyptProperties.getSaltGeneratorClassName());
        config.setStringOutputType(jasyptProperties.getStringOutputType());
        encryptor.setConfig(config);
        return encryptor;
    }
}
