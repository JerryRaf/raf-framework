package com.raf.framework.datasource.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Dynamic DataSource Properties
 *
 * @author Jerry
 * @since 2026-04-20
 */
@Data
@ConfigurationProperties(prefix = "raf.datasource")
public class DynamicDataSourceProperties {

    /**
     * Enable multi-datasource routing
     */
    private boolean enabled = false;

    /**
     * Primary datasource name
     */
    private String primary = "master";

    /**
     * Datasource names list
     */
    private List<String> datasources = new ArrayList<>();

    /**
     * 读写分离配置
     */
    private ReadWriteSplitting readWriteSplitting = new ReadWriteSplitting();

    @Data
    public static class ReadWriteSplitting {

        /**
         * 是否启用读写分离自动路由。
         * 启用后 SELECT 自动路由到从库，INSERT/UPDATE/DELETE 路由到主库。
         * 默认关闭，向后兼容。
         */
        private boolean enabled = false;

        /**
         * 从库数据源 key，需与 raf.datasource.datasources 中的名称一致。
         */
        private String slaveKey = "slave";
    }
}
