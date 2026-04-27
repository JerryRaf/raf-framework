package io.github.jerryraf.examples.mybatis.config;

import com.alibaba.druid.spring.boot3.autoconfigure.DruidDataSourceBuilder;
import com.alibaba.druid.spring.boot3.autoconfigure.properties.DruidStatProperties;
import com.alibaba.druid.spring.boot3.autoconfigure.stat.DruidFilterConfiguration;
import com.alibaba.druid.spring.boot3.autoconfigure.stat.DruidStatViewServletConfiguration;
import com.alibaba.druid.spring.boot3.autoconfigure.stat.DruidWebStatFilterConfiguration;
import com.raf.framework.datasource.DynamicRoutingDataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Multi-DataSource Configuration with Master-Slave Read-Write Splitting
 *
 * <p>Architecture:
 * <pre>
 * ┌─────────────────────────────────────────┐
 * │  Application Layer                      │
 * │  - @DsSelector(DS_MASTER) for writes   │
 * │  - @DsSelector(DS_SLAVE) for reads     │
 * └─────────────────────────────────────────┘
 *              ↓
 * ┌─────────────────────────────────────────┐
 * │  DynamicRoutingDataSource               │
 * │  (Thread-local based routing)           │
 * └─────────────────────────────────────────┘
 *         ↙                    ↘
 * ┌──────────────┐      ┌──────────────┐
 * │ Master DS    │      │ Slave DS     │
 * │ (Write)      │      │ (Read)       │
 * └──────────────┘      └──────────────┘
 * </pre>
 *
 * @author RAF Framework Team
 * @since 2026-04-20
 */
@Slf4j
@Configuration
@EnableConfigurationProperties({DruidStatProperties.class, DataSourceProperties.class})
@Import({DruidStatViewServletConfiguration.class, DruidWebStatFilterConfiguration.class, DruidFilterConfiguration.class})
public class DataSourceConfig {

    public static final String DS_MASTER = "primaryMaster";
    public static final String DS_SLAVE = "primarySlave";
    public static final String TM_PRIMARY = "primaryTransactionManager";

    @Value("${mybatis.mapper-locations:classpath*:mapper/**/*.xml}")
    private String mapperLocations;

    /**
     * Master DataSource (for write operations)
     */
    @Primary
    @Bean(name = DS_MASTER)
    @ConfigurationProperties(prefix = "spring.datasource.primary-master")
    public DataSource masterDataSource() {
        log.info("Initializing Master DataSource: {}", DS_MASTER);
        return DruidDataSourceBuilder.create().build();
    }

    /**
     * Slave DataSource (for read operations)
     */
    @Bean(name = DS_SLAVE)
    @ConfigurationProperties(prefix = "spring.datasource.primary-slave")
    public DataSource slaveDataSource() {
        log.info("Initializing Slave DataSource: {}", DS_SLAVE);
        return DruidDataSourceBuilder.create().build();
    }

    /**
     * Dynamic Routing DataSource
     * <p>
     * Routes database operations to master or slave based on @DsSelector annotation
     */
    @Primary
    @Bean(name = "routingDataSource")
    public DataSource routingDataSource(
            @Qualifier(DS_MASTER) DataSource masterDataSource,
            @Qualifier(DS_SLAVE) DataSource slaveDataSource) {

        log.info("Configuring Dynamic Routing DataSource...");

        DynamicRoutingDataSource routingDataSource = new DynamicRoutingDataSource();

        // Set default to Master (for safety - writes go to master by default)
        routingDataSource.setDefaultTargetDataSource(masterDataSource);

        // Configure target datasources
        Map<Object, Object> dsMap = new HashMap<>(4);
        dsMap.put(DS_MASTER, masterDataSource);
        dsMap.put(DS_SLAVE, slaveDataSource);
        routingDataSource.setTargetDataSources(dsMap);

        log.info("Routing DataSource configured with {} datasources", dsMap.size());
        return routingDataSource;
    }

    /**
     * SqlSessionFactory Configuration
     * <p>
     * Integrates with MyBatis-Plus for enhanced features:
     * - Automatic pagination
     * - Optimistic locking
     * - Logic delete
     * - Auto-fill fields
     */
    @Primary
    @Bean(name = "sqlSessionFactory")
    public SqlSessionFactory sqlSessionFactory(@Qualifier("routingDataSource") DataSource dataSource) throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(dataSource);

        // Load mapper XML files
        Resource[] resources = new PathMatchingResourcePatternResolver().getResources(mapperLocations);
        bean.setMapperLocations(resources);

        log.info("SqlSessionFactory configured with {} mapper files", resources.length);
        return bean.getObject();
    }

    /**
     * SqlSessionTemplate Configuration
     */
    @Primary
    @Bean(name = "sqlSessionTemplate")
    public SqlSessionTemplate sqlSessionTemplate(@Qualifier("sqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    /**
     * Transaction Manager Configuration
     * <p>
     * Manages transactions across the routing datasource
     */
    @Primary
    @Bean(name = TM_PRIMARY)
    public DataSourceTransactionManager transactionManager(@Qualifier("routingDataSource") DataSource dataSource) {
        log.info("Configuring Transaction Manager: {}", TM_PRIMARY);
        return new DataSourceTransactionManager(dataSource);
    }
}
