package com.raf.framework.monitor;

import java.util.function.ToDoubleFunction;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Commons Pool2 metrics configuration.
 * Registers GenericObjectPool metrics to Micrometer registry.
 *
 * @author Jerry
 * @since 2024-08-01
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "raf.monitor.pool.enabled", havingValue = "true")
@ConditionalOnClass({GenericObjectPool.class, MeterRegistry.class})
public class CommonPoolMetricsConfig {

    private static final String LABEL_NAME = "pool";
    private final MeterRegistry registry;
    private final GenericObjectPool<?> pool;

    public CommonPoolMetricsConfig(MeterRegistry registry, GenericObjectPool<?> genericObjectPool) {
        this.registry = registry;
        this.pool = genericObjectPool;
        bindMetrics();
    }

    /**
     * Registers pool metrics to the Micrometer registry.
     */
    private void bindMetrics() {
        createGauge(pool, "commons_pool_num_active", "Number of active objects", p -> (double) pool.getNumActive());
        createGauge(pool, "commons_pool_min_idle", "Minimum number of idle objects", p -> (double) pool.getMinIdle());
        createGauge(pool, "commons_pool_max_idle", "Maximum number of idle objects", p -> (double) pool.getMaxIdle());
        createGauge(pool, "commons_pool_num_idle", "Number of idle objects", p -> (double) pool.getNumIdle());
        createGauge(pool, "commons_pool_num_waiters", "Number of threads waiting for an object", p -> (double) pool.getNumWaiters());
        log.info("Commons Pool2 metrics registered to Micrometer, pool={}", pool.getJmxName().getCanonicalName());
    }

    private void createGauge(GenericObjectPool<?> weakRef, String metric, String help, ToDoubleFunction<GenericObjectPool<?>> measure) {
        Gauge.builder(metric, weakRef, measure)
                .description(help)
                .tag(LABEL_NAME, weakRef.getJmxName().getCanonicalName())
                .register(this.registry);
    }
}
