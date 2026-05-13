package com.raf.framework.redis.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 消息消费幂等性注解。
 *
 * <p>基于 Redis SET NX 原子操作实现，防止 MQ 重投导致的重复消费。
 * 与具体 MQ 类型无关，适用于 RabbitMQ、Kafka、RocketMQ 等任意消费者。
 *
 * <p>使用示例：
 * <pre>{@code
 * // RabbitMQ 消费者
 * @Override
 * @IdempotentConsumer(key = "#message.msgId")
 * public void onMessage(RabbitMqMessage message) throws Exception {
 *     orderService.process(message);
 * }
 *
 * // 自定义前缀和 TTL
 * @IdempotentConsumer(key = "'order:pay:' + #event.orderId", prefix = "mq:idem:", ttl = 3600)
 * public void handlePayEvent(PayEvent event) { ... }
 * }</pre>
 *
 * <p>幂等语义：
 * <ul>
 *   <li>首次消费：正常执行业务逻辑，写入幂等 key</li>
 *   <li>重复消费：跳过业务逻辑，返回 null</li>
 *   <li>消费失败：删除幂等 key，允许 MQ 重试</li>
 * </ul>
 *
 * @author Jerry
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface IdempotentConsumer {

    /**
     * 幂等 key，支持 SpEL 表达式。
     * <p>示例：{@code "#message.msgId"}、{@code "'order:' + #event.orderId"}
     */
    String key();

    /**
     * 幂等 key 前缀，默认 {@code "idempotent:"}。
     */
    String prefix() default "idempotent:";

    /**
     * 幂等 key 保留时间，默认 86400（24 小时）。
     * <p>建议设置为消息最大重投时间窗口的 2 倍以上。
     */
    long ttl() default 86400;

    /**
     * 时间单位，默认秒。
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}
