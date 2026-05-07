package com.raf.framework.kms;

import com.raf.framework.kms.core.CryptoManager;
import com.raf.framework.kms.properties.KmsProperties;
import com.raf.framework.kms.provider.AliyunKmsService;
import com.raf.framework.kms.provider.GoogleKmsService;
import com.raf.framework.kms.provider.HuaweiKmsService;
import com.raf.framework.kms.provider.LocalKmsService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 * KMS Starter 主自动配置入口。
 * 通过 raf.kms.enabled=true 启用，默认关闭。
 * Provider 由各自的 @ConditionalOnProperty 条件装配。
 *
 * @author Jerry
 * @since 2026-05-06
 */
@AutoConfiguration
@ConditionalOnProperty(name = "raf.kms.enabled", havingValue = "true")
@EnableConfigurationProperties(KmsProperties.class)
@Import({
        LocalKmsService.class,
        AliyunKmsService.class,
        HuaweiKmsService.class,
        GoogleKmsService.class,
        CryptoManager.class
})
public class KmsAutoConfiguration {
}
