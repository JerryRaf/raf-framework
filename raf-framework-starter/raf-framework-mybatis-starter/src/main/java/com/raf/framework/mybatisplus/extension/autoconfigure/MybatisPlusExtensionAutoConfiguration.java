package com.raf.framework.mybatisplus.extension.autoconfigure;

import com.raf.framework.mybatisplus.extension.interceptor.SlowSqlInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for MyBatis-Plus extension
 *
 * @author Jerry
 * @since 2026-04-20
 */
@AutoConfiguration
@EnableConfigurationProperties({PageHelperProperties.class, SlowSqlProperties.class})
public class MybatisPlusExtensionAutoConfiguration {

    /**
     * 慢 SQL 监控拦截器。
     * <p>MyBatis-Plus 会自动检测 Spring 容器中的 {@link org.apache.ibatis.plugin.Interceptor} Bean 并注册。
     */
    @Bean
    @ConditionalOnProperty(prefix = "raf.mybatis.slow-sql", name = "enabled", havingValue = "true")
    public SlowSqlInterceptor slowSqlInterceptor(SlowSqlProperties slowSqlProperties) {
        return new SlowSqlInterceptor(slowSqlProperties);
    }
}
