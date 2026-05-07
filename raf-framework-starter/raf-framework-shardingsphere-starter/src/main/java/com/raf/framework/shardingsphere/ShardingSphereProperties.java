package com.raf.framework.shardingsphere;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * ShardingSphere 配置属性
 *
 * @author RAF Framework
 */
@Data
@ConfigurationProperties(prefix = "raf.shardingsphere")
public class ShardingSphereProperties {

    /**
     * 是否启用 ShardingSphere
     */
    private boolean enabled = false;

    /**
     * 数据源配置
     * key: 数据源名称
     * value: 数据源配置
     */
    private Map<String, DataSourceConfig> dataSources = new LinkedHashMap<>();

    /**
     * 分片规则配置（YAML 格式）
     * 支持：分表规则、分库规则、读写分离、数据加密等
     */
    private String rules;

    /**
     * 属性配置
     */
    private Map<String, String> props = new LinkedHashMap<>();

    /**
     * 是否打印 SQL
     */
    private boolean sqlShow = false;

    /**
     * 数据源配置
     */
    @Data
    public static class DataSourceConfig {
        /**
         * 数据源类型（默认 Druid）
         */
        private String type = "com.alibaba.druid.pool.DruidDataSource";

        /**
         * 驱动类名
         */
        private String driverClassName;

        /**
         * JDBC URL
         */
        private String url;

        /**
         * 用户名
         */
        private String username;

        /**
         * 密码
         */
        private String password;

        /**
         * 初始化大小
         */
        private Integer initialSize = 5;

        /**
         * 最小空闲连接
         */
        private Integer minIdle = 5;

        /**
         * 最大活跃连接
         */
        private Integer maxActive = 20;

        /**
         * 最大等待时间（毫秒）
         */
        private Long maxWait = 60000L;

        /**
         * 其他配置属性
         */
        private Map<String, Object> props = new LinkedHashMap<>();
    }
}
