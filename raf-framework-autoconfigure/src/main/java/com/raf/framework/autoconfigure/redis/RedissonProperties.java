package com.raf.framework.autoconfigure.redis;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Jerry
 * @date 2021/09/23
 */
@Data
@ConfigurationProperties(prefix = "raf.redisson")
public class RedissonProperties {
    private boolean enabled = false;
    private int threads;
    private int nettyThreads;

    private RedissonPropertiesSingle single;

    private RedissonPropertiesCluster cluster;

    @Data
    public static class RedissonPropertiesSingle {
        private String host;
        private String port;
        private String password;
        private Integer database = 0;

        private Integer idleConnectionTimeout;
        private Integer connectTimeout;
        private Integer timeout;

        private Integer retryAttempts;
        private Integer retryInterval;

        private Integer subscriptionsPerConnection;
        private String clientName;
    }

    @Data
    public static class RedissonPropertiesCluster {
        private String nodes;
        private String password;

        private Integer idleConnectionTimeout;
        private Integer connectTimeout;
        private Integer timeout;

        private Integer retryAttempts;
        private Integer retryInterval;

        private Integer subscriptionsPerConnection;
        private String clientName;
    }
}
