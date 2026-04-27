package com.raf.framework.monitor;

import com.raf.framework.core.spring.async.ThreadPoolHolder;

import java.util.Collections;
import java.util.function.BiConsumer;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @author Jerry
 * @date 2019/01/01
 */
@Slf4j
@Configuration
@ConditionalOnClass(Metrics.class)
@ConditionalOnProperty(value = "raf.executor.metricEnabled")
@EnableScheduling
public class ThreadPoolMetrics {

    @Scheduled(fixedRate = 5000)
    public void threadPoolMetricsRecord() {
        ThreadPoolHolder.threadPools().forEach(threadPoolMonitorConsumer());
    }

    private BiConsumer<String, ThreadPoolTaskExecutor> threadPoolMonitorConsumer() {
        return (name, threadPool) -> {
            log.debug("Thread pool metrics: name={}, core={}, max={}, active={}, pool={}, queue={}, largest={}, completed={}",
                    name,
                    threadPool.getCorePoolSize(),
                    threadPool.getMaxPoolSize(),
                    threadPool.getActiveCount(),
                    threadPool.getPoolSize(),
                    threadPool.getThreadPoolExecutor().getQueue().size(),
                    threadPool.getThreadPoolExecutor().getLargestPoolSize(),
                    threadPool.getThreadPoolExecutor().getCompletedTaskCount());

            Iterable<Tag> tags = Collections.singletonList(Tag.of("thread.pool.name", name));

            Metrics.gauge("thread.pool.core.size", tags, threadPool, ThreadPoolTaskExecutor::getCorePoolSize);
            Metrics.gauge("thread.pool.max.size", tags, threadPool, ThreadPoolTaskExecutor::getMaxPoolSize);
            Metrics.gauge("thread.pool.active.size", tags, threadPool, ThreadPoolTaskExecutor::getActiveCount);
            Metrics.gauge("thread.pool.thread.count", tags, threadPool, ThreadPoolTaskExecutor::getPoolSize);
            Metrics.gauge("thread.pool.queue.size", tags, threadPool,
                    e -> e.getThreadPoolExecutor().getQueue().size());
            Metrics.gauge("thread.pool.largest.size", tags, threadPool,
                    e -> e.getThreadPoolExecutor().getLargestPoolSize());
            Metrics.gauge("thread.pool.completed.count", tags, threadPool,
                    e -> e.getThreadPoolExecutor().getCompletedTaskCount());
        };
    }
}
