package com.raf.framework.shardingsphere;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Tests for ShardingSphereAutoConfiguration.
 *
 * @author Jerry
 * @since 2026-04-27
 */
class ShardingSphereAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ShardingSphereAutoConfiguration.class));

    @Test
    void shouldNotLoadWhenDisabled() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(ShardingSphereAutoConfiguration.class));
    }

    @Test
    void shouldFailFastWhenEnabledWithoutDatasourceConfig() {
        contextRunner
                .withPropertyValues("raf.shardingsphere.enabled=true")
                .run(context -> assertThat(context).hasFailed());
    }
}
