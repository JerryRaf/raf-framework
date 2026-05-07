package com.raf.framework.kms.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * KMS 配置属性
 * <p>
 * 所有配置不允许有加密数据，避免循环依赖问题。
 *
 * @author Jerry
 * @since 2026-05-06
 */
@Data
@ConfigurationProperties(prefix = "raf.kms")
public class KmsProperties {

    /** 是否启用 KMS，默认关闭 */
    private boolean enabled = false;

    /** Cloud Provider: LOCAL | ALIYUN | HUAWEI | GOOGLE */
    private String provider = "LOCAL";

    /** 正式通道密钥（ALIYUN/HUAWEI/GOOGLE 使用）alias -> KeyConfig */
    private Map<String, KeyConfig> keys = new HashMap<>();

    /** 本地模式密钥（LOCAL 专用，明文存储）alias -> KeyConfig */
    private Map<String, KeyConfig> localKeys = new HashMap<>();

    private AliyunConfig aliyun = new AliyunConfig();
    private HuaweiConfig huawei = new HuaweiConfig();
    private GoogleConfig google = new GoogleConfig();
    private JasyptConfig jasypt = new JasyptConfig();

    @Data
    public static class KeyConfig {
        /** KMS 密钥 ID（KEK ID） */
        private String keyId;
        /** 使用 KMS 加密后的数据密钥密文（Base64） */
        private String cipherText;
        /** LOCAL 模式下直接明文密钥 */
        private String plainText;
        /** 密钥描述（可选） */
        private String description;
    }

    @Data
    public static class AliyunConfig {
        private String regionId;
        private String accessKeyId;
        private String accessKeySecret;
    }

    @Data
    public static class HuaweiConfig {
        private String region;
        private String accessKey;
        private String secretKey;
        private String projectId;
    }

    @Data
    public static class GoogleConfig {
        /** ADC（云内）| JSON（云外，使用服务账号文件） */
        private String authMode = "ADC";
        /** 服务账号 JSON 文件路径（authMode=JSON 时使用） */
        private String serviceAccountJsonPath;
        private ProxyConfig proxy = new ProxyConfig();
    }

    @Data
    public static class ProxyConfig {
        private boolean enabled = false;
        private String host = "127.0.0.1";
        private int port = 10809;
        private String username;
        private String password;
    }

    @Data
    public static class JasyptConfig {
        /** 是否启用 KMS-Jasypt 集成 */
        private boolean enabled = false;
        /** 用哪个 alias 的密钥作为 Jasypt 密码 */
        private String keyAlias;
    }
}
