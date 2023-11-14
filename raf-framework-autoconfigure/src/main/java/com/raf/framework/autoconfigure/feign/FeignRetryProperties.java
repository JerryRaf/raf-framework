package com.raf.framework.autoconfigure.feign;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Jerry
 * @date 2019/01/01
 */
@Data
@ConfigurationProperties(prefix = "raf.feign-retry")
public class FeignRetryProperties {
    private boolean enabled = false;
    private long period = 100;
    private long maxPeriod = 1000;
    private int maxAttempts = 0;
}
