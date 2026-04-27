package com.raf.framework.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for RedisConfig.
 *
 * @author Jerry
 * @since 2026-04-27
 */
class RedisConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RedisConfig.class));

    @Test
    void shouldNotLoadWhenDisabled() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(RedisService.class);
            assertThat(context).doesNotHaveBean("rafRedisTemplate");
        });
    }

    @Test
    void shouldLoadWhenEnabled() {
        contextRunner
                .withPropertyValues("raf.redis.enabled=true")
                .withBean(RedisConnectionFactory.class, () -> mock(RedisConnectionFactory.class))
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .run(context -> {
                    assertThat(context).hasSingleBean(RedisService.class);
                    assertThat(context).hasBean("rafRedisTemplate");
                });
    }
}
