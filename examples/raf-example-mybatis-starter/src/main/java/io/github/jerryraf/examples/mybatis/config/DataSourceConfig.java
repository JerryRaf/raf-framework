package io.github.jerryraf.examples.mybatis.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;

/**
 * DataSource configuration for the MyBatis example.
 *
 * <p>Uses a simple H2 in-memory database so the example runs without
 * any external infrastructure. Master and slave both point to the same
 * H2 instance to keep the setup self-contained.
 *
 * @author Jerry
 */
@Slf4j
@Configuration
public class DataSourceConfig {

    public static final String DS_MASTER = "masterDataSource";
    public static final String DS_SLAVE  = "slaveDataSource";
    public static final String TM_PRIMARY = "primaryTransactionManager";

    @Value("${mybatis.mapper-locations:classpath*:mapper/**/*.xml}")
    private String mapperLocations;

    @Primary
    @Bean(DS_MASTER)
    @ConfigurationProperties(prefix = "spring.datasource.primary-master")
    public DataSource masterDataSource() {
        log.info("Initializing master DataSource");
        return DataSourceBuilder.create().build();
    }

    @Bean(DS_SLAVE)
    @ConfigurationProperties(prefix = "spring.datasource.primary-slave")
    public DataSource slaveDataSource() {
        log.info("Initializing slave DataSource");
        return DataSourceBuilder.create().build();
    }

    @Primary
    @Bean("sqlSessionFactory")
    public SqlSessionFactory sqlSessionFactory(@Qualifier(DS_MASTER) DataSource dataSource) throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(dataSource);
        Resource[] resources = new PathMatchingResourcePatternResolver().getResources(mapperLocations);
        bean.setMapperLocations(resources);
        log.info("SqlSessionFactory configured with {} mapper file(s)", resources.length);
        return bean.getObject();
    }

    @Primary
    @Bean("sqlSessionTemplate")
    public SqlSessionTemplate sqlSessionTemplate(@Qualifier("sqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    @Primary
    @Bean(TM_PRIMARY)
    public DataSourceTransactionManager transactionManager(@Qualifier(DS_MASTER) DataSource dataSource) {
        log.info("Configuring transaction manager: {}", TM_PRIMARY);
        return new DataSourceTransactionManager(dataSource);
    }
}
