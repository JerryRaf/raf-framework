package com.raf.framework.redis;

import java.time.Duration;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "raf.redis")
public class RedisEnhanceProperties {

    /**
     * 是否开启 Redis 功能，默认为 false
     */
    private boolean enabled = false;

    /**
     * 自定义缓存配置
     */
    private Map<String, CustomCacheConfig> customCache;

    @Data
    public static class CustomCacheConfig {
        private Duration timeToLive;
        private boolean cacheNullValues = true;
        private String keyPrefix;
        private boolean useKeyPrefix = true;
    }
}

