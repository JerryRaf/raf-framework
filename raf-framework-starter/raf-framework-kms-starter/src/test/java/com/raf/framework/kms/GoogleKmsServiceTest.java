package com.raf.framework.kms;

import com.google.cloud.kms.v1.DecryptResponse;
import com.google.cloud.kms.v1.EncryptResponse;
import com.google.cloud.kms.v1.KeyManagementServiceClient;
import com.google.protobuf.ByteString;
import com.raf.framework.kms.properties.KmsProperties;
import com.raf.framework.kms.provider.GoogleKmsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GoogleKmsServiceTest {

    private GoogleKmsService service;
    private KeyManagementServiceClient mockClient;
    private KmsProperties kmsProperties;

    @BeforeEach
    void setUp() throws Exception {
        kmsProperties = new KmsProperties();
        kmsProperties.setEnabled(true);
        KmsProperties.GoogleConfig googleConfig = new KmsProperties.GoogleConfig();
        googleConfig.setAuthMode("ADC");
        kmsProperties.setGoogle(googleConfig);

        // GoogleKmsService 构造器是懒加载，不会立即连接，可以直接实例化
        service = new GoogleKmsService(kmsProperties);

        // 注入 mock client，模拟已初始化状态
        mockClient = mock(KeyManagementServiceClient.class);
        Field clientField = GoogleKmsService.class.getDeclaredField("client");
        clientField.setAccessible(true);
        clientField.set(service, mockClient);

        // mock isShutdown() 返回 false，使 ensureInitialized 认为 client 已就绪
        when(mockClient.isShutdown()).thenReturn(false);
    }

    // ===== decrypt 参数校验 =====

    @Test
    void decryptThrowsWhenCipherTextIsNull() {
        assertThatThrownBy(() -> service.decrypt("key-id", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cipher text cannot be empty");
    }

    @Test
    void decryptThrowsWhenCipherTextIsBlank() {
        assertThatThrownBy(() -> service.decrypt("key-id", "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cipher text cannot be empty");
    }

    @Test
    void decryptThrowsWhenKeyIdIsNull() {
        String validBase64 = Base64.getEncoder().encodeToString("test".getBytes());
        assertThatThrownBy(() -> service.decrypt(null, validBase64))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Key ID cannot be empty");
    }

    @Test
    void decryptSuccessfully() {
        String plaintext = "decrypted-value";
        DecryptResponse response = DecryptResponse.newBuilder()
                .setPlaintext(ByteString.copyFromUtf8(plaintext))
                .build();
        when(mockClient.decrypt(anyString(), any(ByteString.class))).thenReturn(response);

        String validBase64 = Base64.getEncoder().encodeToString("cipher-bytes".getBytes());
        String result = service.decrypt("projects/p/locations/l/keyRings/r/cryptoKeys/k", validBase64);
        assertThat(result).isEqualTo(plaintext);
    }

    @Test
    void decryptThrowsOnInvalidBase64() {
        assertThatThrownBy(() -> service.decrypt("key-id", "not-valid-base64!!!"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Google KMS Decrypt Error");
    }

    // ===== encrypt 参数校验 =====

    @Test
    void encryptThrowsWhenPlainTextIsNull() {
        assertThatThrownBy(() -> service.encrypt("key-id", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Plain text cannot be null");
    }

    @Test
    void encryptThrowsWhenKeyIdIsNull() {
        assertThatThrownBy(() -> service.encrypt(null, "plain"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Key ID cannot be empty");
    }

    @Test
    void encryptThrowsWhenKeyIdIsBlank() {
        assertThatThrownBy(() -> service.encrypt("  ", "plain"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Key ID cannot be empty");
    }

    @Test
    void encryptSuccessfully() {
        byte[] cipherBytes = "cipher-bytes".getBytes();
        EncryptResponse response = EncryptResponse.newBuilder()
                .setCiphertext(ByteString.copyFrom(cipherBytes))
                .build();
        when(mockClient.encrypt(anyString(), any(ByteString.class))).thenReturn(response);

        String result = service.encrypt("projects/p/locations/l/keyRings/r/cryptoKeys/k", "plain-text");
        assertThat(result).isEqualTo(Base64.getEncoder().encodeToString(cipherBytes));
    }

    @Test
    void encryptWrapsException() {
        when(mockClient.encrypt(anyString(), any(ByteString.class)))
                .thenThrow(new RuntimeException("network error"));

        assertThatThrownBy(() -> service.encrypt("key-id", "plain"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Google KMS Encrypt Error");
    }

    // ===== resolveKeys =====

    @Test
    void resolveKeysThrowsWhenKeysEmpty() {
        KmsProperties props = new KmsProperties();
        assertThatThrownBy(() -> service.resolveKeys(props))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Keys map cannot be empty");
    }

    @Test
    void resolveKeysSuccessfully() {
        String plaintext = "resolved-key";
        DecryptResponse response = DecryptResponse.newBuilder()
                .setPlaintext(ByteString.copyFromUtf8(plaintext))
                .build();
        when(mockClient.decrypt(anyString(), any(ByteString.class))).thenReturn(response);

        KmsProperties props = new KmsProperties();
        KmsProperties.KeyConfig kc = new KmsProperties.KeyConfig();
        kc.setKeyId("projects/p/locations/l/keyRings/r/cryptoKeys/k");
        kc.setCipherText(Base64.getEncoder().encodeToString("cipher".getBytes()));
        props.getKeys().put("biz-key", kc);

        Map<String, String> result = service.resolveKeys(props);
        assertThat(result).containsEntry("biz-key", plaintext);
    }

    @Test
    void resolveKeysWrapsDecryptException() {
        when(mockClient.decrypt(anyString(), any(ByteString.class)))
                .thenThrow(new RuntimeException("kms error"));

        KmsProperties props = new KmsProperties();
        KmsProperties.KeyConfig kc = new KmsProperties.KeyConfig();
        kc.setKeyId("key-id");
        kc.setCipherText(Base64.getEncoder().encodeToString("cipher".getBytes()));
        props.getKeys().put("biz-key", kc);

        assertThatThrownBy(() -> service.resolveKeys(props))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Google KMS decryption failed for alias: biz-key");
    }
}
