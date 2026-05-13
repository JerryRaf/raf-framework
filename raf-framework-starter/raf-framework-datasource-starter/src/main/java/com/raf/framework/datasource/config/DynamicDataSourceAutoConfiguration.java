package com.raf.framework.datasource.config;

import com.raf.framework.datasource.DsAspect;
import com.raf.framework.datasource.DynamicRoutingDataSource;
import com.raf.framework.datasource.aspect.ForceMasterAspect;
import com.raf.framework.datasource.interceptor.ReadWriteRoutingInterceptor;

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
     * Register DsAspect bean for dynamic datasource switching via @DsSelector
     */
    @Bean
    @ConditionalOnMissingBean
    public DsAspect dsAspect() {
        return new DsAspect();
    }

    /**
     * Register ForceMasterAspect bean for @ForceMaster annotation support
     */
    @Bean
    @ConditionalOnMissingBean
    public ForceMasterAspect forceMasterAspect() {
        return new ForceMasterAspect();
    }

    /**
     * Register ReadWriteRoutingInterceptor when read-write splitting is enabled.
     * MyBatis-Plus auto-detects Interceptor beans in the Spring context.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "raf.datasource.read-write-splitting", name = "enabled", havingValue = "true")
    public ReadWriteRoutingInterceptor readWriteRoutingInterceptor(DynamicDataSourceProperties properties) {
        String slaveKey = properties.getReadWriteSplitting().getSlaveKey();
        log.info("ReadWriteRouting enabled: SELECT → {}, DML → master", slaveKey);
        return new ReadWriteRoutingInterceptor(slaveKey);
    }
}
