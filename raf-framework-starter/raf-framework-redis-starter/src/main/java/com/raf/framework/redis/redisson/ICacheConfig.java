package com.raf.framework.redis.redisson;

import java.util.concurrent.TimeUnit;

/**
 * 缓存配置标准接口
 * 业务层的枚举需要实现此接口
 */
public interface ICacheConfig {
    /**
     * 缓存名称 (唯一标识)
     */
    String getCacheName();

    /**
     * 本地最大容量
     */
    int getMaxSize();

    /**
     * 本地过期时间
     */
    long getLocalTtl();

    /**
     * 时间单位
     */
    TimeUnit getLocalUnit();
}