package com.raf.framework.web.async;

import com.raf.framework.core.spring.ConfigUtil;
import com.raf.framework.core.spring.async.ThreadPoolHolder;
import com.raf.framework.core.trace.CustomThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;
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

/**
 * @author Jerry
 * @date 2019/01/01
 */
@Slf4j
@Configuration
@ConditionalOnProperty(value = "raf.executor.enabled", havingValue = "true")
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
        ThreadPoolProperties threadPoolProperties =
                ConfigUtil.resolveSetting("raf.executor", ThreadPoolProperties.class, this.environment);

        ThreadPoolTaskExecutor taskExecutor = new CustomThreadPoolTaskExecutor();

        // for passing in request scope context
        taskExecutor.setTaskDecorator(new ContextCopyingDecorator());
        taskExecutor.setCorePoolSize(threadPoolProperties.getCorePoolSize());
        taskExecutor.setQueueCapacity(threadPoolProperties.getQueueCapacity());
        taskExecutor.setMaxPoolSize(threadPoolProperties.getMaxPoolSize());
        taskExecutor.setKeepAliveSeconds(threadPoolProperties.getKeepAliveSeconds());
        taskExecutor.setThreadNamePrefix(threadPoolProperties.getThreadNamePrefix());

        taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
        taskExecutor.setAwaitTerminationSeconds(threadPoolProperties.getAwaitTerminationSeconds());
        // rejection policy: CallerRunsPolicy when pool is at max capacity
        taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        taskExecutor.initialize();

        ThreadPoolHolder.register(ASYNC_EXECUTOR_NAME, taskExecutor);
        return taskExecutor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new ThreadExceptionHandler();
    }

    /**
     * Async uncaught exception handler.
     */
    private static class ThreadExceptionHandler implements AsyncUncaughtExceptionHandler {
        @Override
        public void handleUncaughtException(Throwable throwable, Method method, Object... obj) {
            String args = obj.length > 0
                    ? Arrays.stream(obj).map(c -> c == null ? "null" : c.toString())
                            .collect(Collectors.joining(",", "[", "]"))
                    : "";
            log.error("Async task uncaught exception, method={}, args={}", method.getName(), args, throwable);
        }
    }
}
