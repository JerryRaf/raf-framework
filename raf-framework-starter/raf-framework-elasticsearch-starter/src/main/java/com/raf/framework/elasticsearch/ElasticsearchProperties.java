package com.raf.framework.elasticsearch;

import java.time.Duration;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Elasticsearch 配置属性
 *
 * @author RAF Framework
 */
@Data
@ConfigurationProperties(prefix = "raf.elasticsearch")
public class ElasticsearchProperties {

    /**
     * 是否启用 Elasticsearch
     */
    private boolean enabled = false;

    /**
     * Elasticsearch 集群节点地址列表
     * 格式: host:port
     * 例如: ["localhost:9200", "localhost:9201"]
     */
    private List<String> hosts;

    /**
     * 用户名（如果启用了安全认证）
     */
    private String username;

    /**
     * 密码（如果启用了安全认证）
     */
    private String password;

    /**
     * 连接超时时间
     */
    private Duration connectTimeout = Duration.ofSeconds(10);

    /**
     * Socket 超时时间
     */
    private Duration socketTimeout = Duration.ofSeconds(30);

    /**
     * 连接请求超时时间
     */
    private Duration connectionRequestTimeout = Duration.ofSeconds(5);

    /**
     * 最大连接数
     */
    private int maxConnections = 100;

    /**
     * 每个路由的最大连接数
     */
    private int maxConnectionsPerRoute = 50;

    /**
     * 是否启用链路追踪
     */
    private boolean traceEnabled = true;

    /**
     * 索引配置
     */
    private IndexConfig index = new IndexConfig();

    @Data
    public static class IndexConfig {
        /**
         * 默认分片数
         */
        private int numberOfShards = 3;

        /**
         * 默认副本数
         */
        private int numberOfReplicas = 1;

        /**
         * 是否自动创建索引
         */
        private boolean autoCreate = false;

        /**
         * 索引刷新间隔
         */
        private String refreshInterval = "1s";
    }
}
