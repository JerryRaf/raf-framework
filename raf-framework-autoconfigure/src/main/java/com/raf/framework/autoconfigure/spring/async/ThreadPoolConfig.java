package com.raf.framework.autoconfigure.spring.async;

import com.raf.framework.autoconfigure.spring.ConfigUtil;
import com.raf.framework.autoconfigure.trace.CustomThreadPoolTaskExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * @author Jerry
 * @date 2019/01/01
 */
@Slf4j
@Configuration
@ConditionalOnProperty(value = "raf.executor.enabled")
@EnableAsync
public class ThreadPoolConfig implements AsyncConfigurer, EnvironmentAware {
    public static final String ASYNC_EXECUTOR_NAME = "rafAsyncExecutor";

    private ConfigurableEnvironment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = (ConfigurableEnvironment) environment;
    }

    @Override
    @Bean(name = ASYNC_EXECUTOR_NAME)
    public Executor getAsyncExecutor() {
        ThreadPoolProperties threadPoolProperties = ConfigUtil.resolveSetting("raf.executor", ThreadPoolProperties.class, this.environment);

        ThreadPoolTaskExecutor taskExecutor = new CustomThreadPoolTaskExecutor();
        taskExecutor.initialize();

        // for passing in request scope context
        taskExecutor.setTaskDecorator(new ContextCopyingDecorator());
        taskExecutor.setCorePoolSize(threadPoolProperties.getCorePoolSize());
        taskExecutor.setQueueCapacity(threadPoolProperties.getQueueCapacity());
        taskExecutor.setMaxPoolSize(threadPoolProperties.getMaxPoolSize());
        taskExecutor.setKeepAliveSeconds(threadPoolProperties.getKeepAliveSeconds());
        taskExecutor.setThreadNamePrefix(threadPoolProperties.getThreadNamePrefix());

        // 线程池关闭的时候等待所有任务都完成再继续销毁其他的Bean
        taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
        // 设置线程池中任务的等待时间，如果超过这个时候还没有销毁就强制销毁，以确保应用最后能够被关闭，而不是阻塞住
        taskExecutor.setAwaitTerminationSeconds(60);
        //rejection-policy：当pool已经达到max size的时候，如何处理新任务CALLER_RUNS：不在新线程中执行任务，而是由调用者所在的线程来执行
        taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());

        ThreadPoolHolder.register(ASYNC_EXECUTOR_NAME, taskExecutor);
        return taskExecutor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new ThreadExceptionHandler();
    }

    /**
     * 自定义异常处理类
     */
    private static class ThreadExceptionHandler implements AsyncUncaughtExceptionHandler {
        @Override
        public void handleUncaughtException(Throwable throwable, Method method, Object... obj) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("异步线程异常，");
            if (obj.length > 0) {
                stringBuilder.append("参数:");
                String msg = Arrays.stream(obj).map(c -> null == c ? "null" : c.toString()).collect(Collectors.joining(",", "[", "]"));
                stringBuilder.append(msg);
            }
            log.error(stringBuilder.toString(), throwable);
        }
    }
}
