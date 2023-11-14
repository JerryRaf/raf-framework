package com.raf.framework.autoconfigure.mybatis;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.google.common.base.Strings;
import com.raf.framework.autoconfigure.spring.ConfigUtil;
import com.raf.framework.autoconfigure.spring.bean.BeanUtil;
import com.raf.framework.autoconfigure.spring.condition.ConditionalOnMapProperty;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.mapper.ClassPathMapperScanner;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author Jerry
 * @date 2019/01/01
 */
@Slf4j
@Configuration
@ConditionalOnMapProperty(prefix = "raf.datasource")
@ConditionalOnClass({SqlSessionFactory.class, SqlSessionFactoryBean.class, MybatisSqlSessionFactoryBean.class})
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
public class MybatisConfig implements BeanFactoryPostProcessor,
        ResourceLoaderAware, BeanFactoryAware, EnvironmentAware, Ordered {

    private ConfigurableEnvironment environment;

    private ResourceLoader resourceLoader;

    private BeanFactory beanFactory;

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {
        MybatisProperties mybatisProperties = ConfigUtil.resolveSetting("raf", MybatisProperties.class,
                this.environment);
        mybatisProperties.getDataSource().forEach(
                (name, properties) -> createBean(configurableListableBeanFactory, name, properties));
    }

    private void createBean(ConfigurableListableBeanFactory configurableListableBeanFactory, String prefixName, MybatisProperties.MybatisDataSourceProperties mybatisDataSourceProperties) {
        SqlSessionFactory sqlSessionFactory = createSqlSessionFactory(configurableListableBeanFactory,
                prefixName, mybatisDataSourceProperties);
        if (sqlSessionFactory == null) {
            log.error("-->mybatis {} sqlSessionFactory register failed", prefixName);
            return;
        }

        log.info("-->mybatis {} sqlSessionFactory register success", prefixName);

        createSqlSessionTemplate(configurableListableBeanFactory, prefixName, mybatisDataSourceProperties,
                sqlSessionFactory);

        log.info("-->mybatis {} SqlSessionTemplate register success", prefixName);
    }

    private @Nullable SqlSessionFactory createSqlSessionFactory(ConfigurableListableBeanFactory configurableListableBeanFactory,
                                              String prefixName, MybatisProperties.MybatisDataSourceProperties mybatisDataSourceProperties) {
        DataSource dataSource = configurableListableBeanFactory.getBean(prefixName + "Ds", DataSource.class);

        MybatisSqlSessionFactoryBean sqlSessionFactoryBean = new MybatisSqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(dataSource);
        sqlSessionFactoryBean.setVfs(SpringBootVfs.class);
        Optional.ofNullable(mybatisDataSourceProperties.getConfigLocation()).map(this.resourceLoader::getResource)
                .ifPresent(sqlSessionFactoryBean::setConfigLocation);

        Optional.ofNullable(mybatisDataSourceProperties.getConfigurationProperties())
                .ifPresent(sqlSessionFactoryBean::setConfigurationProperties);
        Optional.ofNullable(mybatisDataSourceProperties.getTypeAliasesPackage())
                .ifPresent(sqlSessionFactoryBean::setTypeAliasesPackage);
        Optional.ofNullable(mybatisDataSourceProperties.getTypeHandlersPackage())
                .ifPresent(sqlSessionFactoryBean::setTypeHandlersPackage);
        if (!ObjectUtils.isEmpty(resolveMapperLocations(mybatisDataSourceProperties.getMapperLocations()))) {
            sqlSessionFactoryBean.setMapperLocations(resolveMapperLocations(mybatisDataSourceProperties.getMapperLocations()));
        }

        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setBanner(false);
        sqlSessionFactoryBean.setGlobalConfig(globalConfig);
        PaginationInterceptor paginationInterceptor = new PaginationInterceptor();
        sqlSessionFactoryBean.setPlugins(new Interceptor[]{paginationInterceptor});

        try {
            SqlSessionFactory sqlSessionFactory = sqlSessionFactoryBean.getObject();
            if (sqlSessionFactory == null) {
                log.error("createSqlSessionFactory sqlSessionFactory is null");
                return null;
            }
            BeanUtil.register(configurableListableBeanFactory, sqlSessionFactory, prefixName + "SessionFactory", prefixName + "Sf");

            if (!Strings.isNullOrEmpty(mybatisDataSourceProperties.getBasePackage())) {
                createBasePackageScanner((BeanDefinitionRegistry) configurableListableBeanFactory, mybatisDataSourceProperties.getBasePackage(), prefixName);
            } else {
                createClassPathMapperScanner((BeanDefinitionRegistry) configurableListableBeanFactory, prefixName);
            }
            return sqlSessionFactory;
        } catch (Exception ex) {
            log.error("createSqlSessionFactory异常", ex);
        }
        return null;
    }

    private void createSqlSessionTemplate(ConfigurableListableBeanFactory configurableListableBeanFactory, String prefixName,
                                          MybatisProperties.MybatisDataSourceProperties mybatisDataSourceProperties, SqlSessionFactory sqlSessionFactory) {
        ExecutorType executorType = mybatisDataSourceProperties.getExecutorType();
        SqlSessionTemplate sqlSessionTemplate;
        if (executorType != null) {
            sqlSessionTemplate = new SqlSessionTemplate(sqlSessionFactory, executorType);
        } else {
            sqlSessionTemplate = new SqlSessionTemplate(sqlSessionFactory);
        }
        BeanUtil.register(configurableListableBeanFactory, sqlSessionTemplate, prefixName + "SessionTemplate",prefixName + "St");
    }

    private void createBasePackageScanner(BeanDefinitionRegistry registry, String basePackage,String prefixName) {
        MapperScannerConfigurer scannerConfigurer = new MapperScannerConfigurer();
        scannerConfigurer.setBasePackage(basePackage);
        scannerConfigurer.setSqlSessionFactoryBeanName(prefixName + "SessionFactory");
        scannerConfigurer.postProcessBeanDefinitionRegistry(registry);
    }

    private void createClassPathMapperScanner(BeanDefinitionRegistry registry, String prefixName) {
        ClassPathMapperScanner scanner = new ClassPathMapperScanner(registry);

        try {
            if (this.resourceLoader != null) {
                scanner.setResourceLoader(this.resourceLoader);
            }

            List<String> packages = AutoConfigurationPackages.get(beanFactory);
            packages.forEach(pkg -> log.info("Using auto-configuration base package '{}'", pkg));

            scanner.setAnnotationClass(Mapper.class);
            scanner.setSqlSessionFactoryBeanName(prefixName + "SessionFactory");
            scanner.registerFilters();
            scanner.doScan(StringUtils.toStringArray(packages));
        } catch (IllegalStateException ex) {
            log.error("createClassPathMapperScanner失败", ex);
        }
    }

    private static final ResourcePatternResolver RESOURCE_PATTERN_RESOLVER = new PathMatchingResourcePatternResolver();



    @NestedConfigurationProperty
    private MybatisConfiguration configuration;

    public Resource[] resolveMapperLocations(String[] mapperLocations) {
        return Stream.of(Optional.ofNullable(mapperLocations).orElse(new String[0]))
                .flatMap(location -> Stream.of(getResources(location)))
                .toArray(Resource[]::new);
    }

    private Resource[] getResources(String location) {
        try {
            return RESOURCE_PATTERN_RESOLVER.getResources(location);
        } catch (IOException ex) {
            return new Resource[0];
        }
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = (ConfigurableEnvironment) environment;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public int getOrder() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
}
