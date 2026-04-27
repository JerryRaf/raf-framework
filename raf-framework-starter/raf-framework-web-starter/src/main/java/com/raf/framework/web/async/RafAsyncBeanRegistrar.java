package com.raf.framework.web.async;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 动态 Bean 注册器
 * 读取 raf.async.pools 下的配置，动态注册 ThreadPoolTaskExecutor Bean
 */
public class RafAsyncBeanRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        // 1. 利用 Binder 读取属性（因为此时 ConfigurationProperties 可能还没生效，需手动绑定）
        BindResult<RafAsyncProperties> bindResult = Binder.get(environment).bind("raf.async", RafAsyncProperties.class);

        // 如果没配置或没开启，直接返回
        if (!bindResult.isBound() || !bindResult.get().isEnabled()) {
            return;
        }

        RafAsyncProperties properties = bindResult.get();
        Map<String, RafAsyncProperties.PoolConfig> pools = properties.getPools();

        if (pools == null || pools.isEmpty()) {
            return;
        }

        // 2. 遍历配置，动态注册 Bean
        pools.forEach((beanName, config) -> {
            // 创建 Bean 定义
            BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ThreadPoolTaskExecutor.class);

            // 设置属性
            builder.addPropertyValue("corePoolSize", config.getCoreSize());
            builder.addPropertyValue("maxPoolSize", config.getMaxSize());
            builder.addPropertyValue("queueCapacity", config.getQueueCapacity());
            builder.addPropertyValue("keepAliveSeconds", config.getKeepAliveSeconds());
            builder.addPropertyValue("threadNamePrefix", config.getThreadNamePrefix());

            // 设置拒绝策略
            builder.addPropertyValue("rejectedExecutionHandler", new ThreadPoolExecutor.CallerRunsPolicy());

            // 设置优雅关闭
            builder.addPropertyValue("waitForTasksToCompleteOnShutdown", true);
            builder.addPropertyValue("awaitTerminationSeconds", 60);

            // 设置上下文装饰器 (关键：传递上下文)
            builder.addPropertyValue("taskDecorator", new RafContextTaskDecorator());

            // 3. 注册 Bean，Bean 的名称就是配置文件里的 Key (如 "order-pool")
            registry.registerBeanDefinition(beanName, builder.getBeanDefinition());
        });
    }
}