package com.raf.framework.monitor;

import com.raf.framework.core.common.RafConstant;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Mock thread pool configuration for dev-mock profile.
 * Provides minimal functional thread pool beans to satisfy dependencies.
 *
 * @author Jerry
 * @since 2019-01-01
 */
@Configuration
@Profile("dev-mock")
public class DtMockConfig {

    @Bean(name = RafConstant.SHARED_POOL_TP)
    public ThreadPoolExecutor fastTpMock() {
        return new ThreadPoolExecutor(
                1, 2, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100),
                r -> new Thread(r, "mock-shared-pool"),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    @Bean(name = RafConstant.SLOW_TP)
    public ThreadPoolExecutor slowTpMock() {
        return new ThreadPoolExecutor(
                1, 2, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100),
                r -> new Thread(r, "mock-external-pool"),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }
}
