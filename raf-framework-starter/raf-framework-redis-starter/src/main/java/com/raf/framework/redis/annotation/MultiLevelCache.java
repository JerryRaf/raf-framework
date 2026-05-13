package com.raf.framework.redis.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 声明式多级缓存注解（L1: Caffeine + L2: Redisson）。
 *
 * <p>内置防穿透（空值缓存）和防雪崩（TTL 随机抖动）能力，无需业务侧手动实现。
 *
 * <p>使用示例：
 * <pre>{@code
 * @MultiLevelCache(key = "'user:' + #userId", l2Ttl = 3600)
 * public UserDTO getUser(Long userId) { ... }
 *
 * // 关闭空值缓存（允许穿透）
 * @MultiLevelCache(key = "#id", nullValueTtl = 0)
 * public OrderDTO getOrder(Long id) { ... }
 * }</pre>
 *
 * @author Jerry
 * @see MultiLevelCacheEvict
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface MultiLevelCache {

    /**
     * 缓存 key，支持 SpEL 表达式。
     */
    String key();

    /**
     * L1（Caffeine）最大条目数，默认 500。
     */
    int l1MaxSize() default 500;

    /**
     * L1（Caffeine）TTL，默认 60 秒。
     */
    long l1Ttl() default 60;

    /**
     * L2（Redis）TTL，默认 3600 秒。
     */
    long l2Ttl() default 3600;

    /**
     * 时间单位，同时作用于 l1Ttl 和 l2Ttl，默认秒。
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 空值缓存 TTL，防止缓存穿透。
     * <p>设为 0 表示不缓存空值（允许穿透到数据库）。
     * <p>默认 60 秒。
     */
    long nullValueTtl() default 60;

    /**
     * 是否对 L2 TTL 添加随机抖动（±10%），防止缓存雪崩。
     * <p>默认开启。
     */
    boolean jitter() default true;
}
