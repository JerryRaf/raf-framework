package com.raf.framework.okhttp;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Tests for OkHttpAutoConfig.
 *
 * @author Jerry
 * @since 2026-04-27
 */
class OkHttpAutoConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OkHttpAutoConfig.class));

    @Test
    void shouldNotLoadWhenDisabled() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(OkHttpAutoConfig.class));
    }

    @Test
    void shouldFailWhenEnabledWithoutChannels() {
        contextRunner
                .withPropertyValues("raf.okhttp.enabled=true")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void shouldLoadWhenEnabledWithOneChannel() {
        contextRunner
                .withPropertyValues(
                        "raf.okhttp.enabled=true",
                        "raf.okhttp.channels.main.level=BASIC")
                .run(context -> assertThat(context).hasNotFailed());
    }
}
