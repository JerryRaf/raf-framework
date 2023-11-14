package com.raf.framework.autoconfigure.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.cache.CacheStatistics;
import org.springframework.data.redis.cache.CacheStatisticsCollector;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.lang.Nullable;

import java.time.Duration;

/**
 * @author Jerry
 * @date 2019/01/01
 * redis不可用的时候，可以继续进行后续操作，认为缓存是可能出问题的，但是不会影响具体的业务逻辑操作
 */
@Slf4j
public class CustomRedisCacheWriter implements RedisCacheWriter {

    private RedisCacheWriter delegate;

    public CustomRedisCacheWriter(RedisCacheWriter delegate) {
        this.delegate = delegate;
    }

    @Override
    public void put(String s, byte[] bytes, byte[] bytes1, @Nullable Duration duration) {
        try {
            delegate.put(s, bytes, bytes1, duration);
        } catch (Exception ex) {
            handleException(ex);
        }
    }

    @Nullable
    @Override
    public byte[] get(String s, byte[] bytes) {
        try {
            return delegate.get(s, bytes);
        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    @Nullable
    @Override
    public byte[] putIfAbsent(String s, byte[] bytes, byte[] bytes1, @Nullable Duration duration) {
        try {
            return delegate.putIfAbsent(s, bytes, bytes1, duration);
        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    @Override
    public void remove(String s, byte[] bytes) {
        try {
            delegate.remove(s, bytes);
        } catch (Exception ex) {
            handleException(ex);
        }
    }

    @Override
    public void clean(String s, byte[] bytes) {
        try {
            delegate.clean(s, bytes);
        } catch (Exception ex) {
            handleException(ex);
        }
    }

    @Override
    public void clearStatistics(String name) {

    }

    @Override
    public RedisCacheWriter withStatisticsCollector(CacheStatisticsCollector cacheStatisticsCollector) {
        return null;
    }

    private <T> T handleException(Exception ex) {
        log.error("redis handleException", ex);
        return null;
    }

    @Override
    public CacheStatistics getCacheStatistics(String cacheName) {
        return null;
    }
}
