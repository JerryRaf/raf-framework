package com.raf.framework.kms.jasypt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Jasypt 加密器配置属性。
 * 从配置中心读取，支持动态刷新。
 *
 * @author Jerry
 * @since 2026-05-06
 */
@Data
@ConfigurationProperties(prefix = "jasypt.encryptor")
public class KmsJasyptProperties {
    private String algorithm = "PBEWithHMACSHA512AndAES_256";
    private String ivGeneratorClassName = "org.jasypt.iv.RandomIvGenerator";
    private Integer keyObtentionIterations = 1000;
    private Integer poolSize = 1;
    private String providerName = "SunJCE";
    private String saltGeneratorClassName = "org.jasypt.salt.RandomSaltGenerator";
    private String stringOutputType = "base64";
}
