package com.raf.framework.redis.aspect;

import com.raf.framework.redis.annotation.MultiLevelCache;
import com.raf.framework.redis.annotation.MultiLevelCacheEvict;
import com.raf.framework.redis.redisson.ICacheConfig;
import com.raf.framework.redis.redisson.MultiLevelCacheService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * {@link MultiLevelCache} 和 {@link MultiLevelCacheEvict} 注解的 AOP 切面实现。
 *
 * <p>特性：
 * <ul>
 *   <li>首次调用时自动注册缓存配置（幂等），无需手动 registerCache</li>
 *   <li>空值缓存防穿透：返回 null 时存入占位符，读取时还原为 null</li>
 *   <li>TTL 随机抖动防雪崩：L2 TTL 在 ±10% 范围内随机偏移</li>
 *   <li>cacheName 自动推导为 {@code 类名:方法名}</li>
 * </ul>
 *
 * @author Jerry
 */
@Slf4j
@Aspect
@RequiredArgsConstructor
public class MultiLevelCacheAspect {

    /**
     * 空值占位符，用于防止缓存穿透。
     * 存入 Redis 时使用此字符串，读取时转换回 null。
     */
    static final String NULL_PLACEHOLDER = "__NULL__";

    private final MultiLevelCacheService multiLevelCacheService;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(cache)")
    public Object aroundCache(ProceedingJoinPoint pjp, MultiLevelCache cache) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        String cacheName = buildCacheName(pjp);
        String resolvedKey = resolveKey(pjp, cache.key());

        // 首次调用时幂等注册缓存配置
        ICacheConfig config = buildConfig(cacheName, cache);
        multiLevelCacheService.registerCacheIfAbsent(config);

        long l2Ttl = applyJitter(cache.l2Ttl(), cache.jitter());

        Object result = multiLevelCacheService.getOrLoad(config, resolvedKey,
                l2Ttl, cache.timeUnit(),
                Object.class,
                () -> {
                    try {
                        Object val = pjp.proceed();
                        if (val == null && cache.nullValueTtl() > 0) {
                            // 返回占位符，防止穿透；TTL 使用 nullValueTtl
                            return NULL_PLACEHOLDER;
                        }
                        return val;
                    } catch (Throwable e) {
                        if (e instanceof RuntimeException re) {
                            throw re;
                        }
                        throw new RuntimeException(e);
                    }
                });

        // 将占位符还原为 null
        if (NULL_PLACEHOLDER.equals(result)) {
            log.debug("[MultiLevelCache] Cache hit null placeholder, cacheName={}, key={}", cacheName, resolvedKey);
            return null;
        }
        return result;
    }

    @Around("@annotation(evict)")
    public Object aroundEvict(ProceedingJoinPoint pjp, MultiLevelCacheEvict evict) throws Throwable {
        Object result = pjp.proceed();

        String cacheName = evict.cacheName().isEmpty() ? buildCacheName(pjp) : evict.cacheName();
        String resolvedKey = resolveKey(pjp, evict.key());

        // 构造一个最小化的 ICacheConfig 用于 evict（只需要 cacheName）
        ICacheConfig config = buildMinimalConfig(cacheName);
        multiLevelCacheService.evict(config, resolvedKey);
        log.debug("[MultiLevelCacheEvict] Evicted cacheName={}, key={}", cacheName, resolvedKey);

        return result;
    }

    // ── 私有工具方法 ──────────────────────────────────────────────

    private String buildCacheName(ProceedingJoinPoint pjp) {
        String className = pjp.getTarget().getClass().getSimpleName();
        String methodName = pjp.getSignature().getName();
        return className + ":" + methodName;
    }

    private String resolveKey(ProceedingJoinPoint pjp, String keyExpression) {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(
                pjp.getTarget(), method, pjp.getArgs(), nameDiscoverer);
        Object value = parser.parseExpression(keyExpression).getValue(context);
        if (value == null) {
            throw new IllegalArgumentException(
                    "[MultiLevelCache] SpEL key expression evaluated to null: " + keyExpression);
        }
        return value.toString();
    }

    /**
     * 对 TTL 应用 ±10% 随机抖动，防止大量 key 同时过期导致雪崩。
     */
    private long applyJitter(long ttl, boolean jitter) {
        if (!jitter || ttl <= 0) {
            return ttl;
        }
        // 在 [0.9, 1.1) 范围内随机
        double factor = 0.9 + Math.random() * 0.2;
        return Math.max(1L, Math.round(ttl * factor));
    }

    private ICacheConfig buildConfig(String cacheName, MultiLevelCache cache) {
        return new ICacheConfig() {
            @Override
            public String getCacheName() { return cacheName; }
            @Override
            public int getMaxSize() { return cache.l1MaxSize(); }
            @Override
            public long getLocalTtl() { return cache.l1Ttl(); }
            @Override
            public TimeUnit getLocalUnit() { return cache.timeUnit(); }
        };
    }

    private ICacheConfig buildMinimalConfig(String cacheName) {
        return new ICacheConfig() {
            @Override
            public String getCacheName() { return cacheName; }
            @Override
            public int getMaxSize() { return 0; }
            @Override
            public long getLocalTtl() { return 0; }
            @Override
            public TimeUnit getLocalUnit() { return TimeUnit.SECONDS; }
        };
    }
}
