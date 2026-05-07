package com.raf.framework.kms.provider;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.kms.v1.DecryptResponse;
import com.google.cloud.kms.v1.EncryptResponse;
import com.google.cloud.kms.v1.KeyManagementServiceClient;
import com.google.cloud.kms.v1.KeyManagementServiceSettings;
import com.google.protobuf.ByteString;
import com.raf.framework.kms.properties.KmsProperties;
import io.grpc.HttpConnectProxiedSocketAddress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Google Cloud KMS 服务实现
 * <p>
 * 支持两种认证模式：
 * 1. ADC（Application Default Credentials）：云内模式，适用于在 GCP 环境中运行
 * 2. JSON：云外模式，使用服务账号 JSON 文件，适用于本地测试环境
 *
 * @author Jerry
 * @since 2026-02-06
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "raf.kms.provider", havingValue = "GOOGLE", matchIfMissing = false)
public class GoogleKmsService implements KmsService {

    private volatile KeyManagementServiceClient client;
    private final KmsProperties properties;

    public GoogleKmsService(KmsProperties properties) {
        this.properties = properties;
        // 构造时不强制立即连接，改为"懒加载"，在第一次实际需要加解密时才初始化，加快 Spring 启动速度。
    }

    /**
     * 使用应用默认凭证（ADC）创建 KMS 客户端
     * <p>
     * 适用于在 GCP 环境中运行，自动从环境中获取凭证
     *
     * @return KMS 客户端
     * @throws IOException 创建客户端失败
     */
    private KeyManagementServiceClient createClientWithAdc() throws IOException {
        log.info("Creating Google KMS client using Application Default Credentials (ADC)");
        return KeyManagementServiceClient.create();
    }

    /**
     * 使用服务账号 JSON 文件创建 KMS 客户端
     * <p>
     * 适用于本地测试环境，需要提供服务账号 JSON 文件路径
     *
     * @param jsonFilePath 服务账号 JSON 文件路径
     * @return KMS 客户端
     * @throws Exception 创建客户端失败
     */
    private KeyManagementServiceClient createClientWithJsonFile(String jsonFilePath) throws Exception {
        if (jsonFilePath == null || jsonFilePath.isBlank()) {
            throw new IllegalArgumentException("Service account JSON file path cannot be empty when using JSON auth mode");
        }

        log.info("Creating Google KMS client using service account JSON file: {}", jsonFilePath);

        try (FileInputStream fis = new FileInputStream(jsonFilePath)) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(fis);

            KeyManagementServiceSettings.Builder settingsBuilder = KeyManagementServiceSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(credentials));

            // 获取代理配置
            KmsProperties.ProxyConfig proxyConfig = properties.getGoogle().getProxy();
            if (proxyConfig != null && proxyConfig.isEnabled()) {
                log.info("Applying local gRPC proxy {}:{} to KMS Client ONLY", proxyConfig.getHost(), proxyConfig.getPort());

                // 核心：构建一个仅对当前 KMS 客户端生效的 gRPC 代理拦截器
                TransportChannelProvider transportChannelProvider = KeyManagementServiceSettings.defaultGrpcTransportProviderBuilder()
                        .setChannelConfigurator(managedChannelBuilder -> managedChannelBuilder.proxyDetector(socketAddress -> {
                            // 安全检查：只有 IP/域名 地址才走代理
                            if (!(socketAddress instanceof InetSocketAddress)) {
                                return null;
                            }

                            HttpConnectProxiedSocketAddress.Builder proxyBuilder = HttpConnectProxiedSocketAddress.newBuilder()
                                    .setProxyAddress(new InetSocketAddress(proxyConfig.getHost(), proxyConfig.getPort()))
                                    .setTargetAddress((InetSocketAddress) socketAddress);

                            // 如果代理需要账号密码认证
                            if (proxyConfig.getUsername() != null && !proxyConfig.getUsername().isBlank()) {
                                proxyBuilder.setUsername(proxyConfig.getUsername());
                                proxyBuilder.setPassword(proxyConfig.getPassword() != null ? proxyConfig.getPassword() : "");
                            }

                            return proxyBuilder.build();
                        }))
                        .build();

                // 将拦截器注入到客户端设置中
                settingsBuilder.setTransportChannelProvider(transportChannelProvider);
            }

            return KeyManagementServiceClient.create(settingsBuilder.build());
        }
    }

    /**
     * 解析并解密所有配置的密钥
     *
     * @param properties KMS 配置属性
     * @return 解密后的密钥映射（别名 -> 明文密钥）
     */
    @Override
    public Map<String, String> resolveKeys(KmsProperties properties) {
        ensureInitialized();

        // 校验密钥配置
        if (properties.getKeys().isEmpty()) {
            throw new IllegalArgumentException("[Google KMS] Keys map cannot be empty");
        }

        log.info("Using Google KMS Provider to resolve {} keys", properties.getKeys().size());

        // 解密所有密钥
        Map<String, String> resolvedKeys = new HashMap<>();
        for (Map.Entry<String, KmsProperties.KeyConfig> entry : properties.getKeys().entrySet()) {
            String alias = entry.getKey();
            KmsProperties.KeyConfig keyConfig = entry.getValue();

            try {
                // 调用 Google KMS 解密数据密钥
                String plainKey = decrypt(keyConfig.getKeyId(), keyConfig.getCipherText());
                resolvedKeys.put(alias, plainKey);

                log.info("Successfully decrypted key for alias: {}", alias);
            } catch (Exception e) {
                log.error("Failed to decrypt key for alias: {}", alias, e);
                throw new RuntimeException("Google KMS decryption failed for alias: " + alias, e);
            }
        }

        return resolvedKeys;
    }

    /**
     * 解密密文
     *
     * @param keyId      KMS 密钥 ID（格式：projects/{project}/locations/{location}/keyRings/{keyRing}/cryptoKeys/{cryptoKey}）
     * @param cipherText Base64 编码的密文
     * @return 解密后的明文
     */
    @Override
    public String decrypt(String keyId, String cipherText) {
        ensureInitialized();

        if (cipherText == null || cipherText.isBlank()) {
            throw new IllegalArgumentException("Cipher text cannot be empty");
        }

        if (keyId == null || keyId.isBlank()) {
            throw new IllegalArgumentException("Key ID cannot be empty");
        }

        try {
            // 调试日志：打印密文信息
            log.debug("Decrypting with keyId: {}", keyId);
            log.debug("CipherText original length: {}", cipherText.length());

            // 清理密文：移除所有空白字符（换行符、空格、制表符等）
            // gcloud CLI 输出的密文可能包含换行符，需要清理
            String cleanedCipherText = cipherText.replaceAll("\\s+", "");
            log.debug("CipherText cleaned length: {}", cleanedCipherText.length());

            // 将 Base64 编码的密文解码为字节数组
            byte[] cipherBytes = Base64.getDecoder().decode(cleanedCipherText);
            ByteString byteString = ByteString.copyFrom(cipherBytes);

            // 调用 Google KMS 解密
            DecryptResponse response = client.decrypt(keyId, byteString);
            String plaintext = response.getPlaintext().toStringUtf8();

            log.info("Successfully decrypted data using key: {}", keyId);
            return plaintext;

        } catch (IllegalArgumentException e) {
            log.error("Base64 decode error for key: {}, cipherText: {}", keyId, cipherText, e);
            throw new RuntimeException("Google KMS Decrypt Error: Invalid Base64 format", e);
        } catch (Exception e) {
            log.error("Google KMS decryption failed for key: {}", keyId, e);
            throw new RuntimeException("Google KMS Decrypt Error", e);
        }
    }

    /**
     * 加密明文
     *
     * @param keyId     KMS 密钥 ID（格式：projects/{project}/locations/{location}/keyRings/{keyRing}/cryptoKeys/{cryptoKey}）
     * @param plainText 待加密的明文
     * @return Base64 编码的密文
     */
    @Override
    public String encrypt(String keyId, String plainText) {
        ensureInitialized();

        if (plainText == null) {
            throw new IllegalArgumentException("Plain text cannot be null");
        }

        if (keyId == null || keyId.isBlank()) {
            throw new IllegalArgumentException("Key ID cannot be empty");
        }

        try {
            // 将明文转换为 ByteString
            ByteString ptext = ByteString.copyFromUtf8(plainText);

            // 调用 Google KMS 加密
            EncryptResponse response = client.encrypt(keyId, ptext);
            String ciphertext = Base64.getEncoder().encodeToString(response.getCiphertext().toByteArray());

            log.info("Successfully encrypted data using key: {}", keyId);
            return ciphertext;

        } catch (Exception e) {
            log.error("Google KMS encryption failed for key: {}", keyId, e);
            throw new RuntimeException("Google KMS Encrypt Error", e);
        }
    }

    /**
     * 确保客户端已初始化（懒加载 + 线程安全）
     */
    private void ensureInitialized() {
        if (client != null && !client.isShutdown()) {
            return;
        }

        synchronized (this) {
            // 双重检查锁定
            if (client != null && !client.isShutdown()) {
                return;
            }

            String authMode = properties.getGoogle().getAuthMode();
            log.info("Initializing Google KMS Client with auth mode: {}", authMode);
            long startTime = System.currentTimeMillis();

            try {
                if ("JSON".equalsIgnoreCase(authMode)) {
                    this.client = createClientWithJsonFile(properties.getGoogle().getServiceAccountJsonPath());
                } else {
                    this.client = createClientWithAdc();
                }

                log.info("Google KMS client initialized successfully in {} ms", System.currentTimeMillis() - startTime);
            } catch (Exception e) {
                log.error("Failed to initialize Google KMS Client", e);
                throw new RuntimeException("KMS Initialization failed", e);
            }
        }
    }
}
