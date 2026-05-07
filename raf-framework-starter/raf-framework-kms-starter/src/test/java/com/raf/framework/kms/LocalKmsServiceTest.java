package com.raf.framework.kms;

import com.raf.framework.kms.properties.KmsProperties;
import com.raf.framework.kms.provider.LocalKmsService;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalKmsServiceTest {

    private final LocalKmsService service = new LocalKmsService();

    @Test
    void resolveKeysReturnsPlainTextByAlias() {
        KmsProperties props = new KmsProperties();
        KmsProperties.KeyConfig kc = new KmsProperties.KeyConfig();
        kc.setPlainText("my-secret-key-32bytes-padding123");
        props.getLocalKeys().put("aes-biz-key", kc);

        Map<String, String> result = service.resolveKeys(props);

        assertThat(result).containsEntry("aes-biz-key", "my-secret-key-32bytes-padding123");
    }

    @Test
    void resolveKeysThrowsWhenLocalKeysEmpty() {
        KmsProperties props = new KmsProperties();
        assertThatThrownBy(() -> service.resolveKeys(props))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("localKeys");
    }

    @Test
    void decryptReturnsInputAsIs() {
        assertThat(service.decrypt("key-id", "cipher")).isEqualTo("cipher");
    }

    @Test
    void encryptReturnsInputAsIs() {
        assertThat(service.encrypt("key-id", "plain")).isEqualTo("plain");
    }
}
