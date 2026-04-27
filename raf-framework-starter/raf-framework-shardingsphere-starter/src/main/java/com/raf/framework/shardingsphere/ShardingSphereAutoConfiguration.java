package com.raf.framework.shardingsphere;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.*;
import com.alibaba.druid.pool.DruidDataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.driver.api.ShardingSphereDataSourceFactory;
import org.apache.shardingsphere.infra.config.rule.RuleConfiguration;
import org.apache.shardingsphere.infra.yaml.config.pojo.YamlRootConfiguration;
import org.apache.shardingsphere.infra.yaml.config.swapper.rule.YamlRuleConfigurationSwapperEngine;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.yaml.snakeyaml.Yaml;

/**
 * ShardingSphere 自动配置
 * <p>
 * 与框架现有组件的关系：
 * 1. 与 DataSourceAutoConfig 互斥：启用 ShardingSphere 后，不使用框架的多数据源配置
 * 2. 与 MyBatis 兼容：ShardingSphere 的 DataSource 可以直接被 MyBatis 使用
 * 3. 与 Druid 兼容：ShardingSphere 管理的每个实际数据源可以使用 Druid
 * </p>
 *
 * @author RAF Framework
 */
@Slf4j
@AutoConfiguration(before = DataSourceAutoConfiguration.class)
@ConditionalOnClass(ShardingSphereDataSourceFactory.class)
@ConditionalOnProperty(prefix = "raf.shardingsphere", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(ShardingSphereProperties.class)
public class ShardingSphereAutoConfiguration {

    private final ShardingSphereProperties properties;

    public ShardingSphereAutoConfiguration(ShardingSphereProperties properties) {
        this.properties = properties;
    }

    /**
     * 创建 ShardingSphere 数据源
     * 优先级最高，会覆盖默认的 DataSource
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(DataSource.class)
    public DataSource shardingSphereDataSource() throws SQLException {
        log.info("Initializing ShardingSphere DataSource");

        // 1. 创建实际数据源
        Map<String, DataSource> dataSourceMap = createDataSources();
        log.info("Created {} data sources: {}", dataSourceMap.size(), dataSourceMap.keySet());

        // 2. 解析分片规则
        Collection<RuleConfiguration> ruleConfigs = parseRules();
        log.info("Loaded {} rule configurations", ruleConfigs.size());

        // 3. 配置属性
        Properties props = new Properties();
        props.putAll(properties.getProps());
        props.setProperty("sql-show", String.valueOf(properties.isSqlShow()));

        // 4. 创建 ShardingSphere 数据源
        DataSource dataSource = ShardingSphereDataSourceFactory.createDataSource(
                dataSourceMap,
                ruleConfigs,
                props
        );

        log.info("ShardingSphere DataSource initialized successfully");
        return dataSource;
    }

    /**
     * 创建实际的数据源
     */
    private Map<String, DataSource> createDataSources() {
        Map<String, DataSource> dataSourceMap = new LinkedHashMap<>();

        properties.getDataSources().forEach((name, config) -> {
            try {
                DataSource dataSource = createSingleDataSource(config);
                dataSourceMap.put(name, dataSource);
                log.debug("Created data source: {}", name);
            } catch (Exception e) {
                log.error("Failed to create data source: {}", name, e);
                throw new IllegalStateException("Failed to create data source: " + name, e);
            }
        });

        if (dataSourceMap.isEmpty()) {
            throw new IllegalStateException("No data sources configured for ShardingSphere");
        }

        return dataSourceMap;
    }

    /**
     * 创建单个数据源（默认使用 Druid）
     */
    private DataSource createSingleDataSource(ShardingSphereProperties.DataSourceConfig config) {
        // 默认使用 Druid 数据源
        if (config.getType().contains("DruidDataSource")) {
            return createDruidDataSource(config);
        }

        // 其他数据源类型可以在这里扩展
        throw new IllegalArgumentException("Unsupported data source type: " + config.getType());
    }

    /**
     * 创建 Druid 数据源
     */
    private DataSource createDruidDataSource(ShardingSphereProperties.DataSourceConfig config) {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setDriverClassName(config.getDriverClassName());
        dataSource.setUrl(config.getUrl());
        dataSource.setUsername(config.getUsername());
        dataSource.setPassword(config.getPassword());
        dataSource.setInitialSize(config.getInitialSize());
        dataSource.setMinIdle(config.getMinIdle());
        dataSource.setMaxActive(config.getMaxActive());
        dataSource.setMaxWait(config.getMaxWait());

        // 设置其他连接属性
        if (!config.getProps().isEmpty()) {
            config.getProps().forEach((key, value) -> {
                try {
                    dataSource.addConnectionProperty(key, String.valueOf(value));
                } catch (Exception e) {
                    log.warn("Failed to set connection property {}: {}", key, e.getMessage());
                }
            });
        }

        return dataSource;
    }

    /**
     * 解析分片规则（从 YAML 配置）
     */
    private Collection<RuleConfiguration> parseRules() {
        if (properties.getRules() == null || properties.getRules().trim().isEmpty()) {
            log.warn("No sharding rules configured, returning empty collection");
            return Collections.emptyList();
        }

        try {
            Yaml yaml = new Yaml();
            YamlRootConfiguration rootConfig = yaml.loadAs(properties.getRules(), YamlRootConfiguration.class);

            // 转换为 RuleConfiguration
            YamlRuleConfigurationSwapperEngine swapperEngine = new YamlRuleConfigurationSwapperEngine();
            return swapperEngine.swapToRuleConfigurations(rootConfig.getRules());
        } catch (Exception e) {
            log.error("Failed to parse sharding rules", e);
            throw new IllegalStateException("Failed to parse sharding rules", e);
        }
    }
}
