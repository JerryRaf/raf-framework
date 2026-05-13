package com.raf.framework.redis.aspect;

import com.raf.framework.redis.annotation.IdempotentConsumer;
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
import java.time.Duration;

/**
 * {@link IdempotentConsumer} 注解的 AOP 切面实现。
 *
 * <p>幂等语义：
 * <ul>
 *   <li>首次消费：Redis SET NX 成功，执行业务逻辑</li>
 *   <li>重复消费：SET NX 失败（key 已存在），跳过业务逻辑，返回 null</li>
 *   <li>消费失败：捕获异常后删除幂等 key，允许 MQ 重试</li>
 * </ul>
 *
 * @author Jerry
 */
@Slf4j
@Aspect
@RequiredArgsConstructor
public class IdempotentConsumerAspect {

    private final RedissonService redissonService;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint pjp, IdempotentConsumer idempotent) throws Throwable {
        String idempotentKey = idempotent.prefix() + resolveKey(pjp, idempotent.key());
        Duration ttl = Duration.ofMillis(idempotent.timeUnit().toMillis(idempotent.ttl()));

        // SET NX 原子操作：返回 true 表示首次消费
        boolean isFirstConsume = redissonService.setIfAbsent(idempotentKey, "1", ttl);

        if (!isFirstConsume) {
            log.info("[Idempotent] Duplicate message skipped, key={}", idempotentKey);
            return null;
        }

        try {
            return pjp.proceed();
        } catch (Exception e) {
            // 消费失败时删除幂等 key，允许 MQ 重试
            redissonService.delete(idempotentKey);
            log.warn("[Idempotent] Consume failed, removed idempotent key={}", idempotentKey);
            throw e;
        }
    }

    private String resolveKey(ProceedingJoinPoint pjp, String keyExpression) {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(
                pjp.getTarget(), method, pjp.getArgs(), nameDiscoverer);
        Object value = parser.parseExpression(keyExpression).getValue(context);
        if (value == null) {
            throw new IllegalArgumentException(
                    "[IdempotentConsumer] SpEL key expression evaluated to null: " + keyExpression);
        }
        return value.toString();
    }
}
