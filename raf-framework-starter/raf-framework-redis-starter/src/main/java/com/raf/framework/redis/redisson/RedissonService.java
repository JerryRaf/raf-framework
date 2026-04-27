package com.raf.framework.redis.redisson;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.redisson.codec.TypedJsonJacksonCodec;
import org.springframework.util.Assert;

/**
 * Redisson 操作封装工具类
 * <p>
 * 使用 RedissonClient 默认配置的 Codec（在 RedissonConfig 中设置）
 */
@Slf4j
@SuppressWarnings({"unchecked", "unused"})
public class RedissonService {

    private final RedissonClient redissonClient;

    public RedissonService(RedissonClient redissonClient) {
        Assert.notNull(redissonClient, "RedissonClient must not be null");
        this.redissonClient = redissonClient;
    }

    /* ------------------------- 通用操作 ------------------------- */

    public boolean exists(String key) {
        return redissonClient.getBucket(key).isExists();
    }

    public boolean delete(String key) {
        return redissonClient.getBucket(key).delete();
    }

    public long deleteKeys(String... keys) {
        return redissonClient.getKeys().delete(keys);
    }

    public boolean expire(String key, long ttl, TimeUnit unit) {
        return redissonClient.getBucket(key).expire(Duration.ofMillis(unit.toMillis(ttl)));
    }

    public long getTtl(String key) {
        return getTtl(key, TimeUnit.SECONDS);
    }

    /**
     * -2:key不存在 -1:永久key
     * @param key
     * @param unit
     * @return
     */
    public long getTtl(String key, TimeUnit unit) {
        long ttlMillis = redissonClient.getBucket(key).remainTimeToLive();
        if (ttlMillis == -2 || ttlMillis == -1) {
            return ttlMillis;
        }
        return unit.convert(ttlMillis, TimeUnit.MILLISECONDS);
    }

    /* ------------------------- String / Object 操作 ------------------------- */

    public <V> void set(String key, V value) {
        redissonClient.getBucket(key).set(value);
    }

    public <V> void set(String key, V value, long ttl, TimeUnit unit) {
        redissonClient.getBucket(key).set(value, Duration.ofMillis(unit.toMillis(ttl)));
    }

    public boolean setIfAbsent(String key, Object value) {
        return redissonClient.getBucket(key).setIfAbsent(value);
    }

    public boolean setIfAbsent(String key, Object value, Duration duration) {
        return redissonClient.getBucket(key).setIfAbsent(value, duration);
    }

    @Nullable
    public <T> T get(String key) {
        return (T) redissonClient.getBucket(key).get();
    }

    public <V> V getOrDefault(String key, V defaultValue) {
        V value = get(key);
        return value != null ? value : defaultValue;
    }

    /* ------------------------- 原子操作 ------------------------- */

    public long increment(String key) {
        return redissonClient.getAtomicLong(key).incrementAndGet();
    }

    public long incrementBy(String key, long delta) {
        return redissonClient.getAtomicLong(key).addAndGet(delta);
    }

    public long decrement(String key) {
        return redissonClient.getAtomicLong(key).decrementAndGet();
    }

    public long decrementBy(String key, long delta) {
        return incrementBy(key, -delta);
    }

    /* ------------------------- Hash 操作 ------------------------- */

    public <K, V> void hPut(String key, K field, V value) {
        redissonClient.<K, V>getMap(key).fastPut(field, value);
    }

    public <K, V> boolean hPutIfAbsent(String key, K field, V value) {
        return redissonClient.<K, V>getMap(key).putIfAbsent(field, value) == null;
    }

    @Nullable
    public <K, V> V hGet(String key, K field) {
        return redissonClient.<K, V>getMap(key).get(field);
    }

    public <K, V> Map<K, V> hGetAll(String key) {
        return redissonClient.<K, V>getMap(key).readAllMap();
    }

    public <K> boolean hDelete(String key, K... fields) {
        return redissonClient.<K, Object>getMap(key).fastRemove(fields) > 0;
    }

    public <K> boolean hExists(String key, K field) {
        return redissonClient.<K, Object>getMap(key).containsKey(field);
    }

    /* ------------------------- Set 操作 ------------------------- */

    public <V> boolean sAdd(String key, V... values) {
        return redissonClient.getSet(key).addAll(Arrays.asList(values));
    }

    public <V> boolean sRemove(String key, V... values) {
        return redissonClient.getSet(key).removeAll(Arrays.asList(values));
    }

    public <T> Set<T> sMembers(String key, Class<T> type) {
        // 修复点：使用双参数构造函数，Object.class 占位 Key，type 指定 Value 类型
        RSet<T> set = redissonClient.getSet(
                key,
                new TypedJsonJacksonCodec(Object.class, type)
        );
        return set.readAll();
    }

    public <V> boolean sIsMember(String key, V value) {
        return redissonClient.getSet(key).contains(value);
    }

    public long sSize(String key) {
        return redissonClient.getSet(key).size();
    }

    /* ------------------------- ZSet 操作 ------------------------- */

    public <V> boolean zAdd(String key, double score, V value) {
        return redissonClient.getScoredSortedSet(key).add(score, value);
    }

    public <V> boolean zRemove(String key, V... values) {
        return redissonClient.getScoredSortedSet(key).removeAll(Arrays.asList(values));
    }

    public <T> Set<T> zRange(String key, int start, int end, Class<T> type) {
        // 修复点：同上
        return (Set<T>) redissonClient.getScoredSortedSet(
                key,
                new TypedJsonJacksonCodec(Object.class, type)
        ).valueRange(start, end);
    }

    public long zCard(String key) {
        return redissonClient.getScoredSortedSet(key).size();
    }

    /* ------------------------- List 操作 ------------------------- */

    public <T> void leftPush(String key, T value) {
        redissonClient.getDeque(key).addFirst(value);
    }

    @Nullable
    public <T> T leftPop(String key) {
        return (T) redissonClient.getDeque(key).pollFirst();
    }

    /* ------------------------- 分布式锁封装 ------------------------- */

    /**
     * 获取锁实例（不建议直接用于加锁，仅用于高级操作）
     */
    public RLock getLock(String key) {
        return redissonClient.getLock(key);
    }

    /**
     * 尝试加锁 (看门狗模式 - 自动续期)
     *
     * @param key      锁键
     * @param waitTime 等待时间
     * @param unit     时间单位
     * @return true=加锁成功, false=失败
     */
    public boolean tryLock(String key, long waitTime, TimeUnit unit) {
        RLock lock = redissonClient.getLock(key);
        try {
            // leaseTime = -1 开启看门狗
            return lock.tryLock(waitTime, -1, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Redisson tryLock interrupted: {}", key, e);
            return false;
        }
    }

    /**
     * 尝试加锁 (指定过期时间 - 无看门狗)
     * 适合明确知道业务执行时长的场景
     */
    public boolean tryLock(String key, long waitTime, long leaseTime, TimeUnit unit) {
        RLock lock = redissonClient.getLock(key);
        try {
            return lock.tryLock(waitTime, leaseTime, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Redisson tryLock interrupted: {}", key, e);
            return false;
        }
    }

    /**
     * 安全解锁
     * 只有当前线程持有锁时才释放
     */
    public void unlock(String key) {
        RLock lock = redissonClient.getLock(key);
        if (lock.isLocked() && lock.isHeldByCurrentThread()) {
            try {
                lock.unlock();
            } catch (Exception ex) {
                log.warn("Redisson unlock failed: {}", key, ex);
            }
        }
    }
}