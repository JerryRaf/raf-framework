package com.raf.framework.redis.redisson;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redisson 配置属性类
 * <p>
 * 说明：此类中所有字段均为包装类型且无默认值（null）。
 * 最终的默认策略由 RedissonConfig 类中的常量决定。
 *
 * @author Jerry
 */
@Data
@ConfigurationProperties(prefix = "raf.redisson")
public class RedissonProperties {

    /**
     * 是否开启 Redisson 自动装配
     */
    private boolean enabled;

    /**
     * Redisson 内部线程池数量
     * Redisson 官方默认值：当前 CPU 核心数 * 2
     */
    private Integer threads;

    /**
     * Netty 线程池数量
     * Redisson 官方默认值：当前 CPU 核心数 * 2
     */
    private Integer nettyThreads;

    /**
     * 连接空闲检测时间 (单位：毫秒)
     * Redisson 官方默认值：10000
     */
    private Integer idleConnectionTimeout;

    /**
     * 连接建立超时时间 (单位：毫秒)
     * Redisson 官方默认值：10000
     */
    private Integer connectTimeout;

    /**
     * 命令等待超时时间 (单位：毫秒)
     * Redisson 官方默认值：3000
     */
    private Integer timeout;

    /**
     * 心跳检测间隔 (单位：毫秒)
     * Redisson 官方默认值：30000
     */
    private Integer pingInterval;

    /**
     * 命令失败重试次数
     * Redisson 官方默认值：3
     */
    private Integer retryAttempts;

    /**
     * 命令重试发送时间间隔 (单位：毫秒)
     * Redisson 官方默认值：1500
     */
    private Integer retryInterval;

    /**
     * 每个连接的最大订阅数量
     * Redisson 官方默认值：5
     */
    private Integer subscriptionsPerConnection;

    /**
     * 客户端名称
     */
    private String clientName;

    /**
     * SSL 端点校验（验证服务端证书 CN/SAN），默认开启。
     * 仅在使用自签名证书的内网环境中设为 false。
     */
    private boolean sslEndpointIdentification = true;

    /**
     * 单机模式配置
     */
    private RedissonPropertiesSingle single;

    /**
     * 集群模式配置
     */
    private RedissonPropertiesCluster cluster;

    @Data
    public static class RedissonPropertiesSingle {
        private String host;
        private Integer port;
        private String password;
        private Integer database;

        /**
         * 是否开启 SSL
         */
        private Boolean ssl;
    }

    @Data
    public static class RedissonPropertiesCluster {
        /**
         * 集群节点地址，多个用逗号分隔
         */
        private String nodes;

        private String password;

        /**
         * 是否开启 SSL
         */
        private Boolean ssl;

        /**
         * 集群拓扑扫描间隔 (单位：毫秒)
         * Redisson 官方默认值：1000
         */
        private Integer scanInterval;

        /**
         * 主节点连接池最小空闲连接数
         * Redisson 官方默认值：24
         */
        private Integer masterConnectionPoolMinSize;

        /**
         * 主节点连接池最大连接数
         * Redisson 官方默认值：64
         */
        private Integer masterConnectionPoolMaxSize;
    }
}