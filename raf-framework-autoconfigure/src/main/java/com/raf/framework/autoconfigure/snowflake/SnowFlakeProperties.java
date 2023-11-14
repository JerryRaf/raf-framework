package com.raf.framework.autoconfigure.snowflake;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Jerry
 * @date 2021/09/24
 */
@Data
@ConfigurationProperties(prefix = "raf.snow-flake")
public class SnowFlakeProperties {
    private boolean enabled = false;
    private long workerId = 1;
    private long dataCenterId = 1;
}
