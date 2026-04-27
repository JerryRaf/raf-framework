package com.raf.framework.mongodb;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MongoDB 配置属性
 *
 * @author Jerry
 * @since 3.0.1
 */
@Data
@ConfigurationProperties(prefix = "raf.mongodb")
public class MongodbProperties {

    /**
     * 是否启用 MongoDB 自动配置
     */
    private boolean enabled = false;

    /**
     * MongoDB 连接 URI
     * 格式: mongodb://[username:password@]host[:port]/database[?options]
     * 例如: mongodb://localhost:27017/mydb
     * 例如: mongodb://user:pass@localhost:27017/mydb?authSource=admin
     */
    private String uri;

    /**
     * 数据库名称（如果 URI 中未指定）
     */
    private String database;

    /**
     * 连接池配置
     */
    private PoolConfig pool = new PoolConfig();

    /**
     * 多数据源配置
     * key: 数据源名称
     * value: 数据源配置
     */
    private Map<String, DataSourceConfig> dataSources = new HashMap<>();

    /**
     * 是否启用链路追踪
     */
    private boolean traceEnabled = true;

    /**
     * 连接池配置
     */
    @Data
    public static class PoolConfig {
        /**
         * 最大连接数
         */
        private int maxSize = 100;

        /**
         * 最小连接数
         */
        private int minSize = 10;

        /**
         * 获取连接的最大等待时间（毫秒）
         */
        private long maxWaitTime = 2000;

        /**
         * 连接的最大空闲时间（毫秒）
         * 0 表示不限制
         */
        private long maxConnectionIdleTime = 0;

        /**
         * 连接的最大生命周期（毫秒）
         * 0 表示不限制
         */
        private long maxConnectionLifeTime = 0;

        /**
         * 连接超时时间（毫秒）
         */
        private int connectTimeout = 10000;

        /**
         * Socket 超时时间（毫秒）
         */
        private int socketTimeout = 0;
    }

    /**
     * 数据源配置
     */
    @Data
    public static class DataSourceConfig {
        /**
         * MongoDB 连接 URI
         */
        private String uri;

        /**
         * 数据库名称
         */
        private String database;

        /**
         * 连接池配置（可选，不配置则使用全局配置）
         */
        private PoolConfig pool;
    }
}
