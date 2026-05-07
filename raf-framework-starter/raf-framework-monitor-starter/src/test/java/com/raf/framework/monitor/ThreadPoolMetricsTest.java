package com.raf.framework.monitor;

import io.micrometer.core.instrument.Metrics;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Tests for ThreadPoolMetrics.
 *
 * @author Jerry
 * @since 2026-04-27
 */
class ThreadPoolMetricsTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ThreadPoolMetrics.class));

    @Test
    void shouldNotLoadWhenMetricDisabled() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(ThreadPoolMetrics.class));
    }

    @Test
    void shouldLoadWhenMetricEnabled() {
        contextRunner
                .withPropertyValues("raf.executor.metricEnabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(ThreadPoolMetrics.class);
                    assertThat(Metrics.globalRegistry).isNotNull();
                });
    }
}
