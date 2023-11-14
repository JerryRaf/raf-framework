package com.raf.framework.autoconfigure.sentry;

import io.sentry.Sentry;
import io.sentry.SentryClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Jerry
 * @date 2019/01/01
 */
@Configuration
@ConditionalOnProperty(value = "raf.sentry.enabled")
@EnableConfigurationProperties(SentryProperties.class)
public class SentryConfig {
    public static final String SENTRY_CLIENT = "sentryClient";

    @Autowired
    private SentryProperties sentryProperties;

    @Value("${spring.profiles.active:NA}")
    private String profiles;

    @Value("${spring.application.name:NA}")
    private String applicationName;

    @Bean(name = SENTRY_CLIENT)
    public SentryClient sentryClient() {
        SentryClient sentryClient = Sentry.init(sentryProperties.getDsn());
        sentryClient.setEnvironment(profiles);
        sentryClient.setRelease(applicationName);
        sentryClient.setServerName(applicationName);
        sentryClient.addTag("service", applicationName);
        return sentryClient;
    }
}
