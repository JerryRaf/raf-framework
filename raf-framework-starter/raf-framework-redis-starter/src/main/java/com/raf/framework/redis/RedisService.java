package com.raf.framework.redis;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Redis 通用操作服务
 * 封装了常用的 Redis 操作，屏蔽底层的类型转换细节
 */
@SuppressWarnings({"unchecked", "unused"})
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ValueOperations<String, Object> valueOps;
    private final ZSetOperations<String, Object> zSetOps;

    public RedisService(RedisTemplate<String, Object> redisTemplate) {
        Assert.notNull(redisTemplate, "RedisTemplate must not be null");
        this.redisTemplate = redisTemplate;
        this.valueOps = redisTemplate.opsForValue();
        this.zSetOps = redisTemplate.opsForZSet();
    }

    // ============================= Common =============================

    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public boolean delete(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    public boolean expire(String key, long timeout, TimeUnit unit) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, timeout, unit));
    }

    // ============================= String =============================

    public void set(String key, Object value) {
        valueOps.set(key, value);
    }

    public void set(String key, Object value, long ttl, TimeUnit unit) {
        valueOps.set(key, value, ttl, unit);
    }

    public boolean setIfAbsent(String key, Object value) {
        return Boolean.TRUE.equals(valueOps.setIfAbsent(key, value));
    }

    public boolean setIfAbsent(String key, Object value, long ttl, TimeUnit unit) {
        return Boolean.TRUE.equals(valueOps.setIfAbsent(key, value, ttl, unit));
    }

    @Nullable
    public <T> T get(String key) {
        return (T) valueOps.get(key);
    }

    @Nullable
    public <T> T getAndSet(String key, Object newValue) {
        return (T) valueOps.getAndSet(key, newValue);
    }

    public long increment(String key) {
        return Optional.ofNullable(valueOps.increment(key)).orElse(0L);
    }

    public long decrement(String key) {
        return Optional.ofNullable(valueOps.decrement(key)).orElse(0L);
    }

    // ============================= Set / ZSet =============================

    public long addToSet(String key, Object... values) {
        return Optional.ofNullable(redisTemplate.opsForSet().add(key, values)).orElse(0L);
    }

    public boolean addToZSet(String key, Object value, double score) {
        return Boolean.TRUE.equals(zSetOps.add(key, value, score));
    }

    public long removeFromZSet(String key, Object... values) {
        return Optional.ofNullable(zSetOps.remove(key, values)).orElse(0L);
    }

    @Nullable
    public <T> Set<T> zRange(String key, long start, long end) {
        return (Set<T>) zSetOps.range(key, start, end);
    }

    // ============================= List =============================

    public boolean leftPush(String key, Object value) {
        Long result = redisTemplate.opsForList().leftPush(key, value);
        return result != null && result > 0;
    }

    @Nullable
    public <T> T rightPop(String key) {
        return (T) redisTemplate.opsForList().rightPop(key);
    }

    @Nullable
    public <T> T leftPop(String key) {
        return (T) redisTemplate.opsForList().leftPop(key);
    }

    // ============================= Hash =============================

    public void hSet(String key, String field, Object value) {
        redisTemplate.opsForHash().put(key, field, value);
    }

    public boolean hSetIfAbsent(String key, String field, Object value) {
        return Boolean.TRUE.equals(redisTemplate.opsForHash().putIfAbsent(key, field, value));
    }

    @Nullable
    public <T> T hGet(String key, String field) {
        return (T) redisTemplate.opsForHash().get(key, field);
    }

    public long hDelete(String key, Object... fields) {
        return redisTemplate.opsForHash().delete(key, fields);
    }
}
