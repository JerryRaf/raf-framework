package com.raf.framework.autoconfigure.sentry;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Jerry
 * @date 2019/01/01
 */
@Data
@ConfigurationProperties(prefix = "raf.sentry")
public class SentryProperties {
    private Boolean enabled = false;
    private String dsn;
}
