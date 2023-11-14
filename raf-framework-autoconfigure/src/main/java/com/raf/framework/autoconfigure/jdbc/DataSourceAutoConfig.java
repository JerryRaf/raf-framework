package com.raf.framework.autoconfigure.jdbc;

import com.google.common.base.Strings;
import com.raf.framework.autoconfigure.spring.ConfigUtil;
import com.raf.framework.autoconfigure.spring.bean.BeanUtil;
import com.raf.framework.autoconfigure.spring.condition.ConditionalOnMapProperty;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.aspectj.AnnotationTransactionAspect;

import javax.sql.DataSource;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * @author Jerry
 * @date 2019/01/01
 */
@Configuration
@ConditionalOnMapProperty(prefix = "raf.datasource")
@ConditionalOnClass({DataSource.class, HikariDataSource.class})
public class DataSourceAutoConfig implements BeanFactoryPostProcessor, EnvironmentAware, Ordered {

    private ConfigurableEnvironment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = (ConfigurableEnvironment) environment;
    }

    @Override
    public int getOrder() {
        return Integer.MAX_VALUE - 1;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {
        MultiDsProperties multiDsProperties = ConfigUtil.resolveSetting("raf", MultiDsProperties.class, environment);
        multiDsProperties.getDataSource().forEach((name, properties) -> createBean(configurableListableBeanFactory, name, properties));
    }

    private void createBean(ConfigurableListableBeanFactory configurableListableBeanFactory,
                            String prefixName, MultiDsProperties.JdbcProperties jdbcProperties) {
        String jdbcUrl = jdbcProperties.getUrl();
        checkArgument(!Strings.isNullOrEmpty(jdbcUrl), prefixName + " url is null or empty");

        HikariDataSource hikariDataSource = createHikariDataSource(jdbcProperties);

        DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(hikariDataSource);
        AnnotationTransactionAspect.aspectOf().setTransactionManager(transactionManager);

        JdbcTemplate jdbcTemplate = new JdbcTemplate(hikariDataSource);

        BeanUtil.register(configurableListableBeanFactory, hikariDataSource, prefixName + "DataSource", prefixName + "Ds");
        BeanUtil.register(configurableListableBeanFactory, jdbcTemplate, prefixName + "JdbcTemplate", prefixName + "Jt");
        BeanUtil.register(configurableListableBeanFactory, transactionManager, prefixName + "TransactionManager", prefixName + "Tx");
    }

    private HikariDataSource createHikariDataSource(MultiDsProperties.JdbcProperties jdbcProperties) {
        HikariDataSource hikariDataSource = new HikariDataSource();
        hikariDataSource.setJdbcUrl(jdbcProperties.getUrl());
        hikariDataSource.setUsername(jdbcProperties.getUsername());
        hikariDataSource.setPassword(jdbcProperties.getPassword());

        MultiDsProperties.JdbcPoolProperties jdbcPoolProperties = jdbcProperties.getPool();
        hikariDataSource.setAutoCommit(jdbcPoolProperties.isAutoCommit());
        hikariDataSource.setConnectionTimeout(jdbcPoolProperties.getConnectionTimeout());
        hikariDataSource.setIdleTimeout(jdbcPoolProperties.getIdleTimeout());
        hikariDataSource.setMaxLifetime(jdbcPoolProperties.getMaxLifetime());
        hikariDataSource.setMaximumPoolSize(jdbcPoolProperties.getMaximumPoolSize());
        hikariDataSource.setMinimumIdle(jdbcPoolProperties.getMinimumIdle());
        hikariDataSource.setInitializationFailTimeout(jdbcPoolProperties.getInitializationFailTimeout());
        hikariDataSource.setIsolateInternalQueries(jdbcPoolProperties.isIsolateInternalQueries());
        hikariDataSource.setReadOnly(jdbcPoolProperties.isReadOnly());
        Optional.ofNullable(jdbcProperties.getDriverClassName())
                .ifPresent(hikariDataSource::setDriverClassName);
        hikariDataSource.setRegisterMbeans(jdbcPoolProperties.isRegisterMbeans());
        hikariDataSource.setValidationTimeout(jdbcPoolProperties.getValidationTimeout());
        hikariDataSource.setLeakDetectionThreshold(jdbcPoolProperties.getLeakDetectionThreshold());
        return hikariDataSource;
    }

}
