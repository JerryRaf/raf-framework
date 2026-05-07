package com.raf.framework.kms.provider;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.kms.model.v20160120.DecryptRequest;
import com.aliyuncs.kms.model.v20160120.DecryptResponse;
import com.aliyuncs.kms.model.v20160120.EncryptRequest;
import com.aliyuncs.kms.model.v20160120.EncryptResponse;
import com.aliyuncs.profile.DefaultProfile;
import com.raf.framework.kms.properties.KmsProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 阿里云 KMS 服务实现。
 *
 * @author Jerry
 * @since 2026-05-06
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "raf.kms.provider", havingValue = "ALIYUN")
public class AliyunKmsService implements KmsService {

    private final IAcsClient acsClient;

    public AliyunKmsService(KmsProperties properties) {
        KmsProperties.AliyunConfig config = properties.getAliyun();
        if (config.getRegionId() == null || config.getAccessKeyId() == null || config.getAccessKeySecret() == null) {
            throw new IllegalArgumentException("Aliyun KMS configuration (regionId, accessKeyId, accessKeySecret) is missing");
        }
        log.info("Initializing Aliyun KMS Client for region: {}", config.getRegionId());
        DefaultProfile profile = DefaultProfile.getProfile(
                config.getRegionId(), config.getAccessKeyId(), config.getAccessKeySecret());
        this.acsClient = new DefaultAcsClient(profile);
    }

    @Override
    public Map<String, String> resolveKeys(KmsProperties properties) {
        if (properties.getKeys().isEmpty()) {
            throw new IllegalArgumentException("[Aliyun KMS] keys map cannot be empty");
        }
        log.info("Using Aliyun KMS Provider.");
        Map<String, String> resolvedKeys = new HashMap<>();
        for (Map.Entry<String, KmsProperties.KeyConfig> entry : properties.getKeys().entrySet()) {
            String alias = entry.getKey();
            KmsProperties.KeyConfig keyConfig = entry.getValue();
            try {
                resolvedKeys.put(alias, decrypt(keyConfig.getKeyId(), keyConfig.getCipherText()));
                log.info("Successfully decrypted key for alias: {}", alias);
            } catch (Exception e) {
                log.error("Failed to decrypt key for alias: {}", alias, e);
                throw new RuntimeException("Aliyun KMS decryption failed for alias: " + alias, e);
            }
        }
        return resolvedKeys;
    }

    @Override
    public String decrypt(String keyId, String cipherTextBase64) {
        if (cipherTextBase64 == null) {
            throw new IllegalArgumentException("Cipher text is null");
        }
        DecryptRequest request = new DecryptRequest();
        request.setCiphertextBlob(cipherTextBase64);
        try {
            DecryptResponse response = acsClient.getAcsResponse(request);
            return response.getPlaintext();
        } catch (ClientException e) {
            log.error("Aliyun KMS Decryption failed. Code: {}, Msg: {}", e.getErrCode(), e.getErrMsg());
            throw new RuntimeException("Aliyun KMS Decrypt Error: " + e.getErrMsg(), e);
        }
    }

    @Override
    public String encrypt(String keyId, String plainText) {
        if (keyId == null || plainText == null) {
            throw new IllegalArgumentException("KeyId and PlainText are required");
        }
        EncryptRequest request = new EncryptRequest();
        request.setKeyId(keyId);
        request.setPlaintext(plainText);
        try {
            EncryptResponse response = acsClient.getAcsResponse(request);
            return response.getCiphertextBlob();
        } catch (ClientException e) {
            log.error("Aliyun KMS Encryption failed. Code: {}, Msg: {}", e.getErrCode(), e.getErrMsg());
            throw new RuntimeException("Aliyun KMS Encrypt Error: " + e.getErrMsg(), e);
        }
    }
}
