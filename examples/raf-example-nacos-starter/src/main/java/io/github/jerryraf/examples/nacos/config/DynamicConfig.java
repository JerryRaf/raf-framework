package io.github.jerryraf.examples.nacos.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * Holds properties that are dynamically refreshed from Nacos config center.
 *
 * <p>When the Nacos config is updated, Spring Cloud refreshes this bean
 * automatically without restarting the application.
 *
 * @author Jerry
 */
@Getter
@Component
@RefreshScope
public class DynamicConfig {

    @Value("${app.title:RAF Example (local fallback)}")
    private String title;

    @Value("${app.version:1.0.0}")
    private String version;
}
