package com.raf.framework.redis.redisson;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * Multi-level cache service (L1: Caffeine + L2: Redisson + DB fallback).
 * <p>
 * Architecture:
 * L1: In-process Caffeine cache (ultra-fast, hot-spot protection)
 * L2: Distributed Redisson cache (shared, high-throughput)
 * Source: Database (fallback)
 * <p>
 * Uses Cache-Aside pattern. Eviction deletes the cache entry to trigger
 * lazy reload on next access.
 * <p>
 * Notes:
 * - No {@code @Component} — bean is declared explicitly in RedissonConfig.
 * - JVM-level per-key locking via ConcurrentHashMap prevents cache stampede.
 * - All values stored as JSON strings to avoid generic type erasure issues.
 *
 * @author Jerry
 * @since 2019-01-01
 */
@Slf4j
public class MultiLevelCacheService {

    private final RedissonService redissonHelper;
    private final RedisJsonHelper jsonHelper;

    private final Map<String, Cache<String, String>> cacheRegistry = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> lockMap = new ConcurrentHashMap<>();

    public MultiLevelCacheService(RedissonService redissonHelper, RedisJsonHelper jsonHelper) {
        this.redissonHelper = redissonHelper;
        this.jsonHelper = jsonHelper;
    }

    /**
     * Register a cache pool. Must be called before any {@link #getOrLoad} calls for this config.
     *
     * @param config cache configuration (typically an enum implementing ICacheConfig)
     */
    public void registerCache(ICacheConfig config) {
        String name = config.getCacheName();
        if (cacheRegistry.containsKey(name)) {
            return;
        }

        log.info("Registering cache pool: name={}, maxSize={}, ttl={} {}",
                name, config.getMaxSize(), config.getLocalTtl(), config.getLocalUnit());

        Cache<String, String> cache = Caffeine.newBuilder()
                .initialCapacity(16)
                .maximumSize(config.getMaxSize())
                .expireAfterWrite(config.getLocalTtl(), config.getLocalUnit())
                .recordStats()
                .build();

        cacheRegistry.put(name, cache);
    }

    /**
     * 幂等注册缓存配置，已注册则跳过（线程安全）。
     * 供 {@link com.raf.framework.redis.aspect.MultiLevelCacheAspect} 在首次调用时动态注册。
     *
     * @param config 缓存配置
     */
    public void registerCacheIfAbsent(ICacheConfig config) {
        cacheRegistry.computeIfAbsent(config.getCacheName(), name -> {
            log.info("Registering cache pool (lazy): name={}, maxSize={}, ttl={} {}",
                    name, config.getMaxSize(), config.getLocalTtl(), config.getLocalUnit());
            return Caffeine.newBuilder()
                    .initialCapacity(16)
                    .maximumSize(config.getMaxSize())
                    .expireAfterWrite(config.getLocalTtl(), config.getLocalUnit())
                    .recordStats()
                    .build();
        });
    }

    /**
     * Get a value from cache, loading from DB if absent.
     *
     * @param cacheConfig  cache configuration
     * @param key          cache key (within the cache namespace)
     * @param redisExpire  Redis TTL value
     * @param redisUnit    Redis TTL unit
     * @param typeReference target type reference for deserialization
     * @param dbLoader     supplier to load from DB on cache miss
     * @param <T>          value type
     * @return cached or freshly loaded value
     */
    public <T> T getOrLoad(ICacheConfig cacheConfig,
                           String key,
                           long redisExpire, TimeUnit redisUnit,
                           TypeReference<T> typeReference,
                           Supplier<T> dbLoader) {

        String cacheName = cacheConfig.getCacheName();

        Cache<String, String> localCache = cacheRegistry.get(cacheName);
        if (localCache == null) {
            throw new IllegalArgumentException("Cache [" + cacheName + "] not registered. Call registerCache() at startup.");
        }

        String globalKey = cacheName + ":" + key;

        // L1: local Caffeine
        String json = localCache.getIfPresent(globalKey);
        if (json != null) {
            return jsonHelper.parse(json, typeReference);
        }

        // L2: Redisson
        try {
            json = redissonHelper.get(globalKey);
            if (StringUtils.isNotEmpty(json)) {
                localCache.put(globalKey, json);
                return jsonHelper.parse(json, typeReference);
            }
        } catch (Exception e) {
            log.error("[{}] Redisson get error, key={}", cacheName, globalKey, e);
        }

        // DB with per-key lock to prevent stampede
        Object lock = lockMap.computeIfAbsent(globalKey, k -> new Object());
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (lock) {
            // Double-check after acquiring lock
            if ((json = localCache.getIfPresent(globalKey)) != null) {
                return jsonHelper.parse(json, typeReference);
            }

            try {
                if (StringUtils.isNotEmpty(json = redissonHelper.get(globalKey))) {
                    localCache.put(globalKey, json);
                    return jsonHelper.parse(json, typeReference);
                }
            } catch (Exception e) {
                log.warn("[{}] Redisson get error in lock block, key={}, falling through to DB", cacheName, globalKey, e);
            }

            T dbResult = dbLoader.get();
            if (dbResult != null) {
                String resultJson = jsonHelper.toJson(dbResult);
                localCache.put(globalKey, resultJson);
                try {
                    redissonHelper.set(globalKey, resultJson, redisExpire, redisUnit);
                } catch (Exception e) {
                    log.error("[{}] Redisson set error, key={}", cacheName, globalKey, e);
                }
            }
            return dbResult;
        }
    }

    /**
     * 基于 {@link Class} 类型的 getOrLoad 重载，供 AOP 切面使用。
     *
     * <p>与 {@link #getOrLoad(ICacheConfig, String, long, TimeUnit, TypeReference, Supplier)} 语义相同，
     * 区别在于使用 {@code Class<T>} 而非 {@code TypeReference<T>}，适合泛型信息已擦除的场景。
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrLoad(ICacheConfig cacheConfig,
                           String key,
                           long redisExpire, TimeUnit redisUnit,
                           Class<T> type,
                           Supplier<T> dbLoader) {
        return getOrLoad(cacheConfig, key, redisExpire, redisUnit,
                new TypeReference<T>() {
                    @Override
                    public java.lang.reflect.Type getType() {
                        return type;
                    }
                },
                dbLoader);
    }

    /**
     * Evict a cache entry from both L1 and L2.
     *
     * @param config cache configuration
     * @param key    cache key
     */
    public void evict(ICacheConfig config, String key) {
        String cacheName = config.getCacheName();
        String globalKey = cacheName + ":" + key;

        Cache<String, String> cache = cacheRegistry.get(cacheName);
        if (cache != null) {
            cache.invalidate(globalKey);
        }
        redissonHelper.delete(globalKey);
        lockMap.remove(globalKey);
    }
}
