package com.raf.framework.sentry;

import com.raf.framework.sentry.processor.RafSentryEventProcessor;
import com.raf.framework.sentry.trace.ApmTraceIdProvider;
import com.raf.framework.sentry.trace.FrameworkTraceIdProvider;
import com.raf.framework.sentry.trace.TraceIdProvider;

import io.sentry.EventProcessor;
import io.sentry.Sentry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Sentry auto-configuration.
 * Enhances Sentry Spring Boot Starter integration with framework-level defaults.
 *
 * @author Jerry
 * @since 2026-04-20
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(Sentry.class)
@ConditionalOnProperty(prefix = "raf.sentry", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(RafSentryProperties.class)
public class SentryAutoConfiguration {

    private final RafSentryProperties properties;

    @Value("${spring.profiles.active:default}")
    private String profiles;

    @Value("${spring.application.name:unknown}")
    private String applicationName;

    public SentryAutoConfiguration(RafSentryProperties properties) {
        this.properties = properties;
    }

    /**
     * Configure TraceId provider.
     */
    @Bean
    public TraceIdProvider traceIdProvider() {
        TraceIdProvider provider = properties.getTraceIdSource() == RafSentryProperties.TraceIdSource.APM
                ? new ApmTraceIdProvider()
                : new FrameworkTraceIdProvider();

        log.info("Sentry TraceId provider initialized: {}", provider.getProviderName());
        return provider;
    }

    /**
     * Configure Sentry event processor.
     */
    @Bean
    public EventProcessor rafSentryEventProcessor(TraceIdProvider traceIdProvider) {
        return new RafSentryEventProcessor(traceIdProvider, properties);
    }

    /**
     * Initialize Sentry-related framework logging context.
     * Note: Sentry Spring Boot Starter still reads sentry.* from application config.
     */
    @PostConstruct
    public void initSentry() {
        try {
            String environment = StringUtils.isNotBlank(properties.getEnvironment())
                    ? properties.getEnvironment()
                    : profiles;

            String release = StringUtils.isNotBlank(properties.getRelease())
                    ? properties.getRelease()
                    : applicationName;

            log.info("Sentry configuration initialized: dsn={}, environment={}, release={}, traceIdSource={}",
                    maskDsn(properties.getDsn()),
                    environment,
                    release,
                    properties.getTraceIdSource());

            log.info("Sentry exception filter: enabled={}, reportBusinessException={}",
                    properties.getExceptionFilter().getEnabled(),
                    properties.getExceptionFilter().getReportBusinessException());

            if (properties.getEnablePerformance()) {
                log.info("Sentry performance monitoring enabled: tracesSampleRate={}",
                        properties.getTracesSampleRate());
            }
        } catch (Exception e) {
            log.error("Failed to initialize Sentry configuration", e);
        }
    }

    /**
     * Mask DSN value and keep only a short prefix.
     */
    private String maskDsn(String dsn) {
        if (StringUtils.isBlank(dsn)) {
            return "null";
        }
        if (dsn.length() <= 20) {
            return dsn.substring(0, Math.min(10, dsn.length())) + "***";
        }
        return dsn.substring(0, 20) + "***";
    }
}
