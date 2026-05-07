package com.raf.framework.kms.provider;

import com.huaweicloud.sdk.core.auth.BasicCredentials;
import com.huaweicloud.sdk.core.auth.ICredential;
import com.huaweicloud.sdk.core.exception.ConnectionException;
import com.huaweicloud.sdk.core.exception.RequestTimeoutException;
import com.huaweicloud.sdk.core.exception.ServiceResponseException;
import com.huaweicloud.sdk.kms.v2.KmsClient;
import com.huaweicloud.sdk.kms.v2.model.*;
import com.huaweicloud.sdk.kms.v2.region.KmsRegion;
import com.raf.framework.kms.properties.KmsProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 华为云 KMS 服务实现。
 *
 * @author Jerry
 * @since 2026-05-06
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "raf.kms.provider", havingValue = "HUAWEI")
public class HuaweiKmsService implements KmsService {

    private final KmsClient kmsClient;

    public HuaweiKmsService(KmsProperties properties) {
        KmsProperties.HuaweiConfig config = properties.getHuawei();
        if (config.getRegion() == null || config.getAccessKey() == null || config.getSecretKey() == null || config.getProjectId() == null) {
            throw new IllegalArgumentException("Huawei KMS configuration (region, accessKey, secretKey, projectId) is missing");
        }
        log.info("Initializing Huawei KMS Client for region: {}", config.getRegion());
        ICredential auth = new BasicCredentials()
                .withAk(config.getAccessKey())
                .withSk(config.getSecretKey())
                .withProjectId(config.getProjectId());
        this.kmsClient = KmsClient.newBuilder()
                .withCredential(auth)
                .withRegion(KmsRegion.valueOf(config.getRegion()))
                .build();
    }

    @Override
    public Map<String, String> resolveKeys(KmsProperties properties) {
        if (properties.getKeys().isEmpty()) {
            throw new IllegalArgumentException("[Huawei KMS] keys map cannot be empty");
        }
        log.info("Using Huawei KMS Provider.");
        Map<String, String> resolvedKeys = new HashMap<>();
        for (Map.Entry<String, KmsProperties.KeyConfig> entry : properties.getKeys().entrySet()) {
            String alias = entry.getKey();
            KmsProperties.KeyConfig keyConfig = entry.getValue();
            try {
                resolvedKeys.put(alias, decrypt(keyConfig.getKeyId(), keyConfig.getCipherText()));
                log.info("Successfully decrypted key for alias: {}", alias);
            } catch (Exception e) {
                log.error("Failed to decrypt key for alias: {}", alias, e);
                throw new RuntimeException("Huawei KMS decryption failed for alias: " + alias, e);
            }
        }
        return resolvedKeys;
    }

    @Override
    public String decrypt(String keyId, String cipherTextBase64) {
        if (cipherTextBase64 == null || keyId == null) {
            throw new IllegalArgumentException("KeyId and CipherText are required");
        }
        try {
            DecryptDataRequestBody body = new DecryptDataRequestBody();
            body.setKeyId(keyId);
            body.setCipherText(cipherTextBase64);
            DecryptDataRequest request = new DecryptDataRequest();
            request.setBody(body);
            DecryptDataResponse response = kmsClient.decryptData(request);
            if (response == null) {
                throw new RuntimeException("Huawei KMS returned null response");
            }
            return response.getPlainText();
        } catch (ConnectionException e) {
            log.error("Huawei KMS Connection error: {}", e.getMessage());
            throw new RuntimeException("Huawei KMS Connection Error: " + e.getMessage(), e);
        } catch (RequestTimeoutException e) {
            log.error("Huawei KMS Request timeout: {}", e.getMessage());
            throw new RuntimeException("Huawei KMS Request Timeout: " + e.getMessage(), e);
        } catch (ServiceResponseException e) {
            log.error("Huawei KMS Service error. HttpStatusCode: {}, ErrorCode: {}, ErrorMsg: {}",
                    e.getHttpStatusCode(), e.getErrorCode(), e.getErrorMsg());
            throw new RuntimeException("Huawei KMS Service Error: " + e.getErrorMsg(), e);
        }
    }

    @Override
    public String encrypt(String keyId, String plainText) {
        if (keyId == null || plainText == null) {
            throw new IllegalArgumentException("KeyId and PlainText are required");
        }
        try {
            EncryptDataRequestBody body = new EncryptDataRequestBody();
            body.setKeyId(keyId);
            body.setPlainText(plainText);
            EncryptDataRequest request = new EncryptDataRequest();
            request.setBody(body);
            EncryptDataResponse response = kmsClient.encryptData(request);
            if (response == null) {
                throw new RuntimeException("Huawei KMS returned null response");
            }
            return response.getCipherText();
        } catch (ConnectionException e) {
            log.error("Huawei KMS Connection error: {}", e.getMessage());
            throw new RuntimeException("Huawei KMS Connection Error: " + e.getMessage(), e);
        } catch (RequestTimeoutException e) {
            log.error("Huawei KMS Request timeout: {}", e.getMessage());
            throw new RuntimeException("Huawei KMS Request Timeout: " + e.getMessage(), e);
        } catch (ServiceResponseException e) {
            log.error("Huawei KMS Service error. HttpStatusCode: {}, ErrorCode: {}, ErrorMsg: {}",
                    e.getHttpStatusCode(), e.getErrorCode(), e.getErrorMsg());
            throw new RuntimeException("Huawei KMS Service Error: " + e.getErrorMsg(), e);
        }
    }
}
