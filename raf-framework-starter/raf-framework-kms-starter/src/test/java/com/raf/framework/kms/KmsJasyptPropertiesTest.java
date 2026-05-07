package com.raf.framework.kms;

import com.raf.framework.kms.jasypt.KmsJasyptProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KmsJasyptPropertiesTest {

    @Test
    void defaultValuesAreCorrect() {
        KmsJasyptProperties props = new KmsJasyptProperties();
        assertThat(props.getAlgorithm()).isEqualTo("PBEWithHMACSHA512AndAES_256");
        assertThat(props.getIvGeneratorClassName()).isEqualTo("org.jasypt.iv.RandomIvGenerator");
        assertThat(props.getKeyObtentionIterations()).isEqualTo(1000);
        assertThat(props.getPoolSize()).isEqualTo(1);
        assertThat(props.getProviderName()).isEqualTo("SunJCE");
        assertThat(props.getSaltGeneratorClassName()).isEqualTo("org.jasypt.salt.RandomSaltGenerator");
        assertThat(props.getStringOutputType()).isEqualTo("base64");
    }

    @Test
    void settersAndGettersWork() {
        KmsJasyptProperties props = new KmsJasyptProperties();
        props.setAlgorithm("PBEWithMD5AndDES");
        props.setPoolSize(4);
        props.setKeyObtentionIterations(2000);
        assertThat(props.getAlgorithm()).isEqualTo("PBEWithMD5AndDES");
        assertThat(props.getPoolSize()).isEqualTo(4);
        assertThat(props.getKeyObtentionIterations()).isEqualTo(2000);
    }
}
