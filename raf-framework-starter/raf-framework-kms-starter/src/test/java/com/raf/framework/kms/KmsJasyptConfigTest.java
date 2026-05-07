package com.raf.framework.kms;

import com.raf.framework.kms.core.CryptoManager;
import com.raf.framework.kms.jasypt.KmsJasyptConfig;
import com.raf.framework.kms.jasypt.KmsJasyptProperties;
import com.raf.framework.kms.properties.KmsProperties;
import com.raf.framework.kms.provider.LocalKmsService;
import org.jasypt.encryption.StringEncryptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KmsJasyptConfigTest {

    private static final String TEST_ALIAS = "jasypt-key";
    // 32-byte Base64 key for AES-256
    private static final String TEST_KEY_BASE64 = "dGVzdC1rZXktMzItYnl0ZXMtcGFkZGluZzEyMzQ1Njc=";

    private CryptoManager cryptoManager;
    private KmsProperties kmsProperties;

    @BeforeEach
    void setUp() {
        kmsProperties = new KmsProperties();
        kmsProperties.setEnabled(true);
        KmsProperties.KeyConfig kc = new KmsProperties.KeyConfig();
        kc.setPlainText(TEST_KEY_BASE64);
        kmsProperties.getLocalKeys().put(TEST_ALIAS, kc);

        KmsProperties.JasyptConfig jasyptConfig = new KmsProperties.JasyptConfig();
        jasyptConfig.setEnabled(true);
        jasyptConfig.setKeyAlias(TEST_ALIAS);
        kmsProperties.setJasypt(jasyptConfig);

        LocalKmsService localKms = new LocalKmsService();
        cryptoManager = new CryptoManager(localKms, kmsProperties);
        cryptoManager.init();
    }

    @Test
    void kmsStringEncryptorCreatedSuccessfully() {
        KmsJasyptProperties jasyptProperties = new KmsJasyptProperties();
        KmsJasyptConfig config = new KmsJasyptConfig(cryptoManager, kmsProperties, jasyptProperties);

        StringEncryptor encryptor = config.kmsStringEncryptor();

        assertThat(encryptor).isNotNull();
        // 验证加解密可以正常工作
        String plainText = "test-secret-value";
        String encrypted = encryptor.encrypt(plainText);
        assertThat(encrypted).isNotEqualTo(plainText);
        assertThat(encryptor.decrypt(encrypted)).isEqualTo(plainText);
    }

    @Test
    void throwsWhenKeyAliasIsNull() {
        KmsProperties.JasyptConfig jasyptConfig = new KmsProperties.JasyptConfig();
        jasyptConfig.setKeyAlias(null);
        kmsProperties.setJasypt(jasyptConfig);

        KmsJasyptProperties jasyptProperties = new KmsJasyptProperties();
        KmsJasyptConfig config = new KmsJasyptConfig(cryptoManager, kmsProperties, jasyptProperties);

        assertThatThrownBy(config::kmsStringEncryptor)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("key-alias");
    }

    @Test
    void throwsWhenKeyAliasIsBlank() {
        KmsProperties.JasyptConfig jasyptConfig = new KmsProperties.JasyptConfig();
        jasyptConfig.setKeyAlias("   ");
        kmsProperties.setJasypt(jasyptConfig);

        KmsJasyptProperties jasyptProperties = new KmsJasyptProperties();
        KmsJasyptConfig config = new KmsJasyptConfig(cryptoManager, kmsProperties, jasyptProperties);

        assertThatThrownBy(config::kmsStringEncryptor)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("key-alias");
    }
}
