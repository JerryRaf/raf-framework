package com.raf.framework.redis.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明式多级缓存清除注解。
 *
 * <p>使用示例：
 * <pre>{@code
 * @MultiLevelCacheEvict(key = "'user:' + #userId")
 * public void updateUser(Long userId, UserDTO dto) { ... }
 * }</pre>
 *
 * <p>注意：cacheName 自动推导为 {@code 类名:方法名}，与 {@link MultiLevelCache} 保持一致。
 * 若清除的是其他方法的缓存，需手动指定 {@link #cacheName}。
 *
 * @author Jerry
 * @see MultiLevelCache
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface MultiLevelCacheEvict {

    /**
     * 缓存 key，支持 SpEL 表达式。
     */
    String key();

    /**
     * 缓存名称，默认为空（自动推导为 {@code 类名:方法名}）。
     * <p>若需清除其他方法的缓存，需手动指定对应方法的 cacheName（格式：{@code 类名:方法名}）。
     */
    String cacheName() default "";
}
