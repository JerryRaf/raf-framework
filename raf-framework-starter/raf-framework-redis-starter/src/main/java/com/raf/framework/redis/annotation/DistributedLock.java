package com.raf.framework.redis.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 注解式分布式锁。
 *
 * <p>基于 Redisson 实现，通过 SpEL 表达式动态解析锁 key，支持看门狗自动续期。
 *
 * <p>使用示例：
 * <pre>{@code
 * @DistributedLock(key = "'order:pay:' + #orderId")
 * public void payOrder(Long orderId) { ... }
 *
 * // 指定等待时间和租约时间
 * @DistributedLock(key = "#req.userId + ':submit'", waitTime = 0, leaseTime = 30)
 * public void submit(SubmitRequest req) { ... }
 * }</pre>
 *
 * <p>与编程式 API 并存：
 * <ul>
 *   <li>简单场景：使用本注解，零样板代码</li>
 *   <li>复杂场景（条件加锁、多锁）：使用 {@link com.raf.framework.redis.redisson.RedissonService}</li>
 * </ul>
 *
 * @author Jerry
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface DistributedLock {

    /**
     * 锁 key，支持 SpEL 表达式。
     * <p>示例：{@code "'order:' + #orderId"}、{@code "#req.userId + ':action'"}
     */
    String key();

    /**
     * 等待获取锁的最长时间，默认 3 秒。
     * <p>设为 0 表示不等待，获取不到立即失败。
     */
    long waitTime() default 3;

    /**
     * 锁的持有时间，默认 -1（启用看门狗自动续期）。
     * <p>明确知道业务执行时长时可指定正值，关闭看门狗以节省资源。
     */
    long leaseTime() default -1;

    /**
     * 时间单位，默认秒。
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 获取锁失败时的提示信息。
     */
    String failMessage() default "操作频繁，请稍后再试";
}
