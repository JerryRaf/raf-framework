package com.raf.framework.kms;

import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.kms.model.v20160120.DecryptResponse;
import com.aliyuncs.kms.model.v20160120.EncryptResponse;
import com.raf.framework.kms.properties.KmsProperties;
import com.raf.framework.kms.provider.AliyunKmsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AliyunKmsServiceTest {

    private AliyunKmsService service;
    private IAcsClient mockAcsClient;

    @BeforeEach
    void setUp() throws Exception {
        // 构造时需要合法配置，但不会真正连接（DefaultAcsClient 是懒加载）
        KmsProperties props = buildProps();
        service = new AliyunKmsService(props);

        // 注入 mock client，避免真实网络调用
        mockAcsClient = mock(IAcsClient.class);
        Field field = AliyunKmsService.class.getDeclaredField("acsClient");
        field.setAccessible(true);
        field.set(service, mockAcsClient);
    }

    private KmsProperties buildProps() {
        KmsProperties props = new KmsProperties();
        KmsProperties.AliyunConfig config = new KmsProperties.AliyunConfig();
        config.setRegionId("cn-hangzhou");
        config.setAccessKeyId("test-ak");
        config.setAccessKeySecret("test-sk");
        props.setAliyun(config);
        return props;
    }

    // ===== 构造参数校验 =====

    @Test
    void constructorThrowsWhenRegionIdMissing() {
        KmsProperties props = buildProps();
        props.getAliyun().setRegionId(null);
        assertThatThrownBy(() -> new AliyunKmsService(props))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("regionId");
    }

    @Test
    void constructorThrowsWhenAccessKeyIdMissing() {
        KmsProperties props = buildProps();
        props.getAliyun().setAccessKeyId(null);
        assertThatThrownBy(() -> new AliyunKmsService(props))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructorThrowsWhenAccessKeySecretMissing() {
        KmsProperties props = buildProps();
        props.getAliyun().setAccessKeySecret(null);
        assertThatThrownBy(() -> new AliyunKmsService(props))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ===== decrypt 参数校验 =====

    @Test
    void decryptThrowsWhenCipherTextIsNull() {
        assertThatThrownBy(() -> service.decrypt("key-id", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cipher text is null");
    }

    @Test
    void decryptSuccessfully() throws Exception {
        DecryptResponse response = new DecryptResponse();
        response.setPlaintext("plain-value");
        when(mockAcsClient.getAcsResponse(any())).thenReturn(response);

        String result = service.decrypt("key-id", "cipher-base64");
        assertThat(result).isEqualTo("plain-value");
    }

    @Test
    void decryptWrapsClientException() throws Exception {
        when(mockAcsClient.getAcsResponse(any())).thenThrow(new ClientException("SDK.InvalidRegionId", "invalid"));

        assertThatThrownBy(() -> service.decrypt("key-id", "cipher"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Aliyun KMS Decrypt Error");
    }

    // ===== encrypt 参数校验 =====

    @Test
    void encryptThrowsWhenKeyIdIsNull() {
        assertThatThrownBy(() -> service.encrypt(null, "plain"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("KeyId and PlainText are required");
    }

    @Test
    void encryptThrowsWhenPlainTextIsNull() {
        assertThatThrownBy(() -> service.encrypt("key-id", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("KeyId and PlainText are required");
    }

    @Test
    void encryptSuccessfully() throws Exception {
        EncryptResponse response = new EncryptResponse();
        response.setCiphertextBlob("cipher-blob");
        when(mockAcsClient.getAcsResponse(any())).thenReturn(response);

        String result = service.encrypt("key-id", "plain-text");
        assertThat(result).isEqualTo("cipher-blob");
    }

    @Test
    void encryptWrapsClientException() throws Exception {
        when(mockAcsClient.getAcsResponse(any())).thenThrow(new ClientException("SDK.InvalidRegionId", "invalid"));

        assertThatThrownBy(() -> service.encrypt("key-id", "plain"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Aliyun KMS Encrypt Error");
    }

    // ===== resolveKeys =====

    @Test
    void resolveKeysThrowsWhenKeysEmpty() {
        KmsProperties props = buildProps();
        assertThatThrownBy(() -> service.resolveKeys(props))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("keys map cannot be empty");
    }

    @Test
    void resolveKeysSuccessfully() throws Exception {
        DecryptResponse response = new DecryptResponse();
        response.setPlaintext("resolved-plain");
        when(mockAcsClient.getAcsResponse(any())).thenReturn(response);

        KmsProperties props = buildProps();
        KmsProperties.KeyConfig kc = new KmsProperties.KeyConfig();
        kc.setKeyId("key-001");
        kc.setCipherText("cipher-001");
        props.getKeys().put("biz-key", kc);

        Map<String, String> result = service.resolveKeys(props);
        assertThat(result).containsEntry("biz-key", "resolved-plain");
    }
}
