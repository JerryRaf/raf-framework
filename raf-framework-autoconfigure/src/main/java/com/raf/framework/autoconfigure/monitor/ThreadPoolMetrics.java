package com.raf.framework.autoconfigure.monitor;

import com.raf.framework.autoconfigure.spring.async.ThreadPoolHolder;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Collections;
import java.util.function.BiConsumer;

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
            log.info("{},{},{},{},{},{},{}", threadPool.getCorePoolSize(), threadPool.getMaxPoolSize(),
                    threadPool.getActiveCount(), threadPool.getPoolSize(),
                    threadPool.getThreadPoolExecutor().getQueue().size(), threadPool.getThreadPoolExecutor().getLargestPoolSize(),
                    threadPool.getThreadPoolExecutor().getCompletedTaskCount());

            Iterable<Tag> tags = Collections.singletonList(Tag.of("thread.pool.name", name));

            //核心线程数
            Metrics.gauge("thread.pool.core.size", tags, threadPool, ThreadPoolTaskExecutor::getCorePoolSize);
            //线程池容量
            Metrics.gauge("thread.pool.max.size", tags, threadPool, ThreadPoolTaskExecutor::getMaxPoolSize);

            //当前活跃线程数
            Metrics.gauge("thread.pool.active.size", tags, threadPool, ThreadPoolTaskExecutor::getActiveCount);
            //当前线程池中运行的线程总数(包括核心线程和非核心线程)
            Metrics.gauge("thread.pool.thread.count", tags, threadPool, ThreadPoolTaskExecutor::getPoolSize);
            //线程池积压任务数
            Metrics.gauge("thread.pool.queue.size", tags, threadPool, e -> e.getThreadPoolExecutor().getQueue().size());
            //线程池历史峰值线程数
            Metrics.gauge("thread.pool.largest.size", tags, threadPool, e -> e.getThreadPoolExecutor().getLargestPoolSize());
            //完成任务数
            Metrics.gauge("thread.pool.largest.size", tags, threadPool, e -> e.getThreadPoolExecutor().getCompletedTaskCount());
        };
    }

}
