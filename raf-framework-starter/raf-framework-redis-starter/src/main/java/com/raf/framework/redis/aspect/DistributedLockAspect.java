package com.raf.framework.redis.aspect;

import com.raf.framework.core.common.exception.BusinessException;
import com.raf.framework.core.common.result.RafResponseEnum;
import com.raf.framework.redis.annotation.DistributedLock;
import com.raf.framework.redis.redisson.RedissonService;

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

/**
 * {@link DistributedLock} 注解的 AOP 切面实现。
 *
 * <p>执行流程：
 * <ol>
 *   <li>解析 SpEL key 表达式，拼接最终锁 key</li>
 *   <li>调用 {@link RedissonService#tryLock} 尝试获取锁</li>
 *   <li>获取失败抛出 {@link BusinessException}（{@code TOO_MANY_REQUESTS}）</li>
 *   <li>业务执行完毕后在 finally 块安全释放锁</li>
 * </ol>
 *
 * @author Jerry
 */
@Slf4j
@Aspect
@RequiredArgsConstructor
public class DistributedLockAspect {

    private static final String LOCK_KEY_PREFIX = "dlock:";

    private final RedissonService redissonService;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint pjp, DistributedLock distributedLock) throws Throwable {
        String lockKey = LOCK_KEY_PREFIX + resolveKey(pjp, distributedLock.key());

        boolean locked;
        if (distributedLock.leaseTime() < 0) {
            // leaseTime = -1：启用看门狗自动续期
            locked = redissonService.tryLock(lockKey, distributedLock.waitTime(), distributedLock.timeUnit());
        } else {
            locked = redissonService.tryLock(lockKey,
                    distributedLock.waitTime(), distributedLock.leaseTime(), distributedLock.timeUnit());
        }

        if (!locked) {
            log.warn("[DistributedLock] Failed to acquire lock, key={}", lockKey);
            throw new BusinessException(RafResponseEnum.TOO_MANY_REQUESTS, distributedLock.failMessage());
        }

        log.debug("[DistributedLock] Acquired lock, key={}", lockKey);
        try {
            return pjp.proceed();
        } finally {
            redissonService.unlock(lockKey);
            log.debug("[DistributedLock] Released lock, key={}", lockKey);
        }
    }

    /**
     * 解析 SpEL 表达式，支持 {@code #参数名}、{@code #root.method} 等。
     */
    private String resolveKey(ProceedingJoinPoint pjp, String keyExpression) {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(
                pjp.getTarget(), method, pjp.getArgs(), nameDiscoverer);
        Object value = parser.parseExpression(keyExpression).getValue(context);
        if (value == null) {
            throw new IllegalArgumentException(
                    "[DistributedLock] SpEL key expression evaluated to null: " + keyExpression);
        }
        return value.toString();
    }
}
