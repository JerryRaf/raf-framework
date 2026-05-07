package com.raf.framework.kms;

import com.raf.framework.kms.properties.KmsProperties;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class KmsPropertiesTest {

    @Test
    void defaultProviderIsLocal() {
        KmsProperties props = new KmsProperties();
        assertThat(props.getProvider()).isEqualTo("LOCAL");
        assertThat(props.isEnabled()).isFalse();
    }

    @Test
    void keyConfigHoldsAllFields() {
        KmsProperties.KeyConfig kc = new KmsProperties.KeyConfig();
        kc.setKeyId("key-001");
        kc.setCipherText("abc==");
        kc.setPlainText("secret");
        kc.setDescription("test key");
        assertThat(kc.getKeyId()).isEqualTo("key-001");
        assertThat(kc.getCipherText()).isEqualTo("abc==");
        assertThat(kc.getPlainText()).isEqualTo("secret");
    }

    @Test
    void jasyptConfigDefaultKeyAliasIsEmpty() {
        KmsProperties.JasyptConfig jc = new KmsProperties.JasyptConfig();
        assertThat(jc.isEnabled()).isFalse();
        assertThat(jc.getKeyAlias()).isNull();
    }
}
