package com.raf.framework.redis;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
     * 默认缓存TTL（Time To Live）
     * 默认1小时
     */
    private Duration defaultTtl = Duration.ofHours(1);

    /**
     * 自定义缓存配置
     */
    private Map<String, CustomCacheConfig> customCache;

    /**
     * Redis JSON 多态反序列化白名单包前缀。
     * <p>默认只允许框架业务包和 JDK 基础类型，业务方如需缓存自定义包类型应显式配置。</p>
     */
    private List<String> trustedPackages = new ArrayList<>(Arrays.asList(
            "com.raf.",
            "java.util.",
            "java.lang.",
            "java.time."
    ));

    @Data
    public static class CustomCacheConfig {
        private Duration timeToLive;
        private boolean cacheNullValues = true;
        private String keyPrefix;
        private boolean useKeyPrefix = true;
    }
}
