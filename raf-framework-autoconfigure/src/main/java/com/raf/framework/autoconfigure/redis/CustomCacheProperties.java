package com.raf.framework.autoconfigure.redis;

import lombok.Data;

import java.time.Duration;
import java.util.Map;

/**
 * @author Jerry
 * @date 2019/01/01
 */
@Data
public class CustomCacheProperties {

    private Map<String, CacheProperties> customCache;

    @Data
    public static class CacheProperties {
        private Duration timeToLive;
        private boolean cacheNullValues = true;
        private String keyPrefix;
        private boolean useKeyPrefix = true;
    }
}
