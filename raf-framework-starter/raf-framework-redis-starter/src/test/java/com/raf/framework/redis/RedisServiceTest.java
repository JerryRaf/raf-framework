package com.raf.framework.redis;

import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

/**
 * Tests for RedisService.
 *
 * @author Jerry
 * @since 2026-04-27
 */
class RedisServiceTest {

    @Test
    void shouldReturnFalseWhenDeleteReturnsNull() {
        RedisTemplate<String, Object> template = mock(RedisTemplate.class);
        ValueOperations<String, Object> valueOps = mock(ValueOperations.class);
        ZSetOperations<String, Object> zSetOps = mock(ZSetOperations.class);
        when(template.opsForValue()).thenReturn(valueOps);
        when(template.opsForZSet()).thenReturn(zSetOps);
        when(template.delete("k")).thenReturn(null);

        RedisService service = new RedisService(template);

        assertThat(service.delete("k")).isFalse();
    }

    @Test
    void shouldReturnZeroWhenIncrementReturnsNull() {
        RedisTemplate<String, Object> template = mock(RedisTemplate.class);
        ValueOperations<String, Object> valueOps = mock(ValueOperations.class);
        ZSetOperations<String, Object> zSetOps = mock(ZSetOperations.class);
        when(template.opsForValue()).thenReturn(valueOps);
        when(template.opsForZSet()).thenReturn(zSetOps);
        when(valueOps.increment("counter")).thenReturn(null);

        RedisService service = new RedisService(template);

        assertThat(service.increment("counter")).isZero();
    }

    @Test
    void shouldSetValueWithTtl() {
        RedisTemplate<String, Object> template = mock(RedisTemplate.class);
        ValueOperations<String, Object> valueOps = mock(ValueOperations.class);
        ZSetOperations<String, Object> zSetOps = mock(ZSetOperations.class);
        when(template.opsForValue()).thenReturn(valueOps);
        when(template.opsForZSet()).thenReturn(zSetOps);

        RedisService service = new RedisService(template);
        service.set("k", "v", 10L, TimeUnit.SECONDS);

        org.mockito.Mockito.verify(valueOps).set("k", "v", 10L, TimeUnit.SECONDS);
    }
}
