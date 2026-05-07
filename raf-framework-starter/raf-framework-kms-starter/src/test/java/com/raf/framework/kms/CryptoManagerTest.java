package com.raf.framework.kms;

import com.raf.framework.kms.core.CryptoManager;
import com.raf.framework.kms.properties.KmsProperties;
import com.raf.framework.kms.provider.LocalKmsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CryptoManagerTest {

    private CryptoManager cryptoManager;
    private static final String TEST_ALIAS = "aes-biz-key";
    // AES-256 需要 32 字节密钥，Base64 编码
    private static final String TEST_KEY_BASE64 = "dGVzdC1rZXktMzItYnl0ZXMtcGFkZGluZzEyMzQ1Njc=";

    @BeforeEach
    void setUp() {
        KmsProperties props = new KmsProperties();
        props.setEnabled(true);
        KmsProperties.KeyConfig kc = new KmsProperties.KeyConfig();
        kc.setPlainText(TEST_KEY_BASE64);
        props.getLocalKeys().put(TEST_ALIAS, kc);
        LocalKmsService localKms = new LocalKmsService();
        cryptoManager = new CryptoManager(localKms, props);
        cryptoManager.init();
    }

    @Test
    void encryptAndDecryptRoundTrip() {
        String plainText = "hello-world";
        String cipher = cryptoManager.encrypt(plainText, TEST_ALIAS);
        assertThat(cipher).isNotEqualTo(plainText);
        String decrypted = cryptoManager.decrypt(cipher, TEST_ALIAS);
        assertThat(decrypted).isEqualTo(plainText);
    }

    @Test
    void generateIndexIsDeterministic() {
        String index1 = cryptoManager.generateIndex("13800138000", TEST_ALIAS);
        String index2 = cryptoManager.generateIndex("13800138000", TEST_ALIAS);
        assertThat(index1).isEqualTo(index2);
    }

    @Test
    void encryptNullOrEmptyReturnsInput() {
        assertThat(cryptoManager.encrypt(null, TEST_ALIAS)).isNull();
        assertThat(cryptoManager.encrypt("", TEST_ALIAS)).isEmpty();
    }

    @Test
    void decryptNullOrEmptyReturnsInput() {
        assertThat(cryptoManager.decrypt(null, TEST_ALIAS)).isNull();
        assertThat(cryptoManager.decrypt("", TEST_ALIAS)).isEmpty();
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("FORCE_EMERGENCY_MODE");
    }

    @Test
    void emergencyModeLoadsFromLocalKeys() {
        System.setProperty("FORCE_EMERGENCY_MODE", "true");

        KmsProperties props = new KmsProperties();
        props.setEnabled(true);
        KmsProperties.KeyConfig kc = new KmsProperties.KeyConfig();
        kc.setPlainText(TEST_KEY_BASE64);
        props.getLocalKeys().put(TEST_ALIAS, kc);

        LocalKmsService localKms = new LocalKmsService();
        CryptoManager manager = new CryptoManager(localKms, props);
        manager.init();

        // 验证 emergency 模式下密钥可用
        String cipher = manager.encrypt("hello", TEST_ALIAS);
        assertThat(manager.decrypt(cipher, TEST_ALIAS)).isEqualTo("hello");
    }

    @Test
    void emergencyModeThrowsWhenLocalKeysEmpty() {
        System.setProperty("FORCE_EMERGENCY_MODE", "true");

        KmsProperties props = new KmsProperties();
        props.setEnabled(true);
        // localKeys 为空

        LocalKmsService localKms = new LocalKmsService();
        CryptoManager manager = new CryptoManager(localKms, props);

        assertThatThrownBy(manager::init)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("FORCE_EMERGENCY_MODE");
    }

    @Test
    void generateIndexReturnsNullForEmpty() {
        assertThat(cryptoManager.generateIndex("", TEST_ALIAS)).isNull();
        assertThat(cryptoManager.generateIndex(null, TEST_ALIAS)).isNull();
    }

    @Test
    void getRawKeyReturnsKey() {
        String key = cryptoManager.getRawKey(TEST_ALIAS);
        assertThat(key).isNotBlank();
    }

    @Test
    void getEnvironmentVariableReadsSystemProperty() {
        System.setProperty("TEST_ENV_KEY", "test-value");
        try {
            assertThat(CryptoManager.getEnvironmentVariable("TEST_ENV_KEY")).isEqualTo("test-value");
        } finally {
            System.clearProperty("TEST_ENV_KEY");
        }
    }
}
