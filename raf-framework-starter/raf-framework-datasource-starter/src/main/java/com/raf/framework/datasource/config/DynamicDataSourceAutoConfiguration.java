package com.raf.framework.datasource.config;

import com.raf.framework.datasource.DsAspect;
import com.raf.framework.datasource.DynamicRoutingDataSource;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Dynamic DataSource Auto Configuration
 *
 * @author Jerry
 * @since 2026-04-20
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "raf.datasource", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(DynamicDataSourceProperties.class)
public class DynamicDataSourceAutoConfiguration {

    /**
     * Create dynamic routing datasource bean
     *
     * @param properties         datasource properties
     * @param applicationContext Spring application context
     * @return dynamic routing datasource
     */
    @Bean
    @Primary
    public DataSource dynamicDataSource(DynamicDataSourceProperties properties,
                                        ApplicationContext applicationContext) {
        Map<Object, Object> targetDataSources = new HashMap<>();

        for (String dsName : properties.getDatasources()) {
            DataSource ds = applicationContext.getBean(dsName, DataSource.class);
            targetDataSources.put(dsName, ds);
            log.info("Register datasource: {}", dsName);
        }

        DynamicRoutingDataSource dynamicDataSource = new DynamicRoutingDataSource();
        dynamicDataSource.setTargetDataSources(targetDataSources);
        dynamicDataSource.setDefaultTargetDataSource(
                applicationContext.getBean(properties.getPrimary(), DataSource.class)
        );
        dynamicDataSource.afterPropertiesSet();

        log.info("Dynamic datasource initialized with primary: {}", properties.getPrimary());
        return dynamicDataSource;
    }

    /**
     * Register DsAspect bean for dynamic datasource switching
     *
     * @return DsAspect instance
     */
    @Bean
    @ConditionalOnMissingBean
    public DsAspect dsAspect() {
        return new DsAspect();
    }
}
