package com.raf.framework.monitor;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import com.alibaba.druid.pool.DruidDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Druid datasource metrics configuration.
 * Registers Druid connection pool metrics to Micrometer registry.
 *
 * @author Jerry
 * @since 2024-08-01
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "raf.monitor.druid.enabled", havingValue = "true")
@ConditionalOnClass({DruidDataSource.class, MeterRegistry.class})
public class DruidMetricsConfig {

    private final MeterRegistry registry;

    public DruidMetricsConfig(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * Binds all Druid datasources to the Micrometer registry.
     *
     * @param dataSources all DataSource beans in the context
     * @throws SQLException if unwrapping a datasource fails
     */
    @Autowired
    public void bindMetricsRegistryToDruidDataSources(Collection<DataSource> dataSources) throws SQLException {
        List<DruidDataSource> druidDataSources = new ArrayList<>(dataSources.size());
        for (DataSource dataSource : dataSources) {
            DruidDataSource druidDataSource = dataSource.unwrap(DruidDataSource.class);
            if (druidDataSource != null) {
                druidDataSources.add(druidDataSource);
            }
        }
        DruidCollector druidCollector = new DruidCollector(druidDataSources, registry);
        druidCollector.register();
        log.info("Druid datasource metrics registered to Micrometer, count={}", druidDataSources.size());
    }
}
