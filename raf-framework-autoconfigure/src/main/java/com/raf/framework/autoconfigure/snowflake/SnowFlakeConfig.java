package com.raf.framework.autoconfigure.snowflake;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Jerry
 * @date 2018/10/27 14:34
 */
@Configuration
@ConditionalOnProperty(value = "raf.snow-flake.enabled")
@EnableConfigurationProperties(SnowFlakeProperties.class)
public class SnowFlakeConfig {
    @Bean
    public Snowflake snowflake(SnowFlakeProperties snowFlakeProperties) {
        return IdUtil.getSnowflake(snowFlakeProperties.getWorkerId(), snowFlakeProperties.getDataCenterId());
    }
}
