package com.raf.framework.kms;

import com.huaweicloud.sdk.kms.v2.KmsClient;
import com.huaweicloud.sdk.kms.v2.model.*;
import com.raf.framework.kms.properties.KmsProperties;
import com.raf.framework.kms.provider.HuaweiKmsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class HuaweiKmsServiceTest {

    private HuaweiKmsService service;
    private KmsClient mockKmsClient;

    /**
     * 通过反射绕过构造器中的云端初始化，直接注入 mock KmsClient。
     */
    @BeforeEach
    void setUp() throws Exception {
        mockKmsClient = Mockito.mock(KmsClient.class);

        // 使用 Objenesis（JUnit5 / Mockito 内置）创建实例，跳过构造器
        Constructor<HuaweiKmsService> ctor = HuaweiKmsService.class.getDeclaredConstructor(KmsProperties.class);
        // 无法跳过构造器，改为 spy 一个已构造好的实例
        // 构造时传入 null region 会抛异常，因此直接用 Mockito.mock 创建代理
        service = Mockito.mock(HuaweiKmsService.class, Mockito.CALLS_REAL_METHODS);

        Field field = HuaweiKmsService.class.getDeclaredField("kmsClient");
        field.setAccessible(true);
        field.set(service, mockKmsClient);
    }

    // ===== 构造参数校验 =====

    @Test
    void constructorThrowsWhenRegionMissing() {
        KmsProperties props = new KmsProperties();
        KmsProperties.HuaweiConfig config = new KmsProperties.HuaweiConfig();
        config.setAccessKey("ak");
        config.setSecretKey("sk");
        config.setProjectId("proj");
        props.setHuawei(config);
        assertThatThrownBy(() -> new HuaweiKmsService(props))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("region");
    }

    @Test
    void constructorThrowsWhenAccessKeyMissing() {
        KmsProperties props = new KmsProperties();
        KmsProperties.HuaweiConfig config = new KmsProperties.HuaweiConfig();
        config.setRegion("cn-north-4");
        config.setSecretKey("sk");
        config.setProjectId("proj");
        props.setHuawei(config);
        assertThatThrownBy(() -> new HuaweiKmsService(props))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ===== decrypt 参数校验 =====

    @Test
    void decryptThrowsWhenCipherTextIsNull() {
        assertThatThrownBy(() -> service.decrypt("key-id", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("required");
    }

    @Test
    void decryptThrowsWhenKeyIdIsNull() {
        assertThatThrownBy(() -> service.decrypt(null, "cipher"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("required");
    }

    @Test
    void decryptSuccessfully() {
        DecryptDataResponse response = new DecryptDataResponse();
        response.setPlainText("plain-value");
        when(mockKmsClient.decryptData(any(DecryptDataRequest.class))).thenReturn(response);

        String result = service.decrypt("key-id", "cipher-base64");
        assertThat(result).isEqualTo("plain-value");
    }

    @Test
    void decryptThrowsWhenResponseIsNull() {
        when(mockKmsClient.decryptData(any(DecryptDataRequest.class))).thenReturn(null);

        assertThatThrownBy(() -> service.decrypt("key-id", "cipher"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("null response");
    }

    // ===== encrypt 参数校验 =====

    @Test
    void encryptThrowsWhenKeyIdIsNull() {
        assertThatThrownBy(() -> service.encrypt(null, "plain"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("required");
    }

    @Test
    void encryptThrowsWhenPlainTextIsNull() {
        assertThatThrownBy(() -> service.encrypt("key-id", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("required");
    }

    @Test
    void encryptSuccessfully() {
        EncryptDataResponse response = new EncryptDataResponse();
        response.setCipherText("cipher-result");
        when(mockKmsClient.encryptData(any(EncryptDataRequest.class))).thenReturn(response);

        String result = service.encrypt("key-id", "plain-text");
        assertThat(result).isEqualTo("cipher-result");
    }

    @Test
    void encryptThrowsWhenResponseIsNull() {
        when(mockKmsClient.encryptData(any(EncryptDataRequest.class))).thenReturn(null);

        assertThatThrownBy(() -> service.encrypt("key-id", "plain"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("null response");
    }

    // ===== resolveKeys =====

    @Test
    void resolveKeysThrowsWhenKeysEmpty() {
        KmsProperties props = new KmsProperties();
        assertThatThrownBy(() -> service.resolveKeys(props))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("keys map cannot be empty");
    }

    @Test
    void resolveKeysSuccessfully() {
        DecryptDataResponse response = new DecryptDataResponse();
        response.setPlainText("resolved-plain");
        when(mockKmsClient.decryptData(any(DecryptDataRequest.class))).thenReturn(response);

        KmsProperties props = new KmsProperties();
        KmsProperties.KeyConfig kc = new KmsProperties.KeyConfig();
        kc.setKeyId("key-001");
        kc.setCipherText("cipher-001");
        props.getKeys().put("biz-key", kc);

        Map<String, String> result = service.resolveKeys(props);
        assertThat(result).containsEntry("biz-key", "resolved-plain");
    }
}
