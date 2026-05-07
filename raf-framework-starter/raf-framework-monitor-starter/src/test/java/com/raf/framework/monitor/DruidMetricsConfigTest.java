package com.raf.framework.monitor;

import javax.sql.DataSource;
import com.alibaba.druid.pool.DruidDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Tests for DruidMetricsConfig.
 *
 * @author Jerry
 * @since 2026-04-27
 */
class DruidMetricsConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DruidMetricsConfig.class));

    @Test
    void shouldNotLoadWhenDisabled() {
        contextRunner
                .withBean(MeterRegistry.class, () -> mock(MeterRegistry.class))
                .withBean(DataSource.class, DruidDataSource::new)
                .run(context -> assertThat(context).doesNotHaveBean(DruidMetricsConfig.class));
    }

    @Test
    void shouldLoadWhenEnabled() {
        contextRunner
                .withPropertyValues("raf.monitor.druid.enabled=true")
                .withBean(MeterRegistry.class, () -> mock(MeterRegistry.class))
                .withBean(DataSource.class, DruidDataSource::new)
                .run(context -> assertThat(context).hasSingleBean(DruidMetricsConfig.class));
    }
}
