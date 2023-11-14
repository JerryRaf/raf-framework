package com.raf.framework.autoconfigure.feign;

import feign.Feign;
import feign.Logger;
import feign.Retryer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * @author Jerry
 * @date 2019/01/01
 */
@Slf4j
@Configuration
@ConditionalOnProperty(value = "raf.sentry.enabled")
@ConditionalOnClass({FeignClientsConfiguration.class, Feign.class})
@AutoConfigureBefore(FeignClientsConfiguration.class)
@EnableConfigurationProperties(FeignRetryProperties.class)
public class FeignRetryConfig {
    @Bean
    @ConditionalOnProperty(value = "raf.feign-retry.enabled")
    public Retryer feignRetry(FeignRetryProperties feignRetryProperties) {
        return new Retryer.Default(feignRetryProperties.getPeriod(),
                TimeUnit.MILLISECONDS.toMillis(feignRetryProperties.getMaxPeriod()),
                feignRetryProperties.getMaxAttempts());
    }

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
}
