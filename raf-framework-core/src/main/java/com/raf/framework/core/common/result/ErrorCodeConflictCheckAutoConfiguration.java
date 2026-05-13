package com.raf.framework.core.common.result;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 错误码冲突检测自动配置。
 *
 * @author Jerry
 */
@Configuration
@ConditionalOnProperty(prefix = "raf.error-code.conflict-check", name = "enabled",
        havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ErrorCodeConflictCheckProperties.class)
public class ErrorCodeConflictCheckAutoConfiguration {

    @Bean
    public ErrorCodeConflictDetector errorCodeConflictDetector(
            ErrorCodeConflictCheckProperties properties) {
        return new ErrorCodeConflictDetector(properties);
    }
}
