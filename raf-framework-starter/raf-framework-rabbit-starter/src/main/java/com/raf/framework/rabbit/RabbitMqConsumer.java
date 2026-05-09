package com.raf.framework.rabbit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.amqp.core.AcknowledgeMode;

/**
 * 标记 RabbitMQ 消费者监听器，框架自动注册 SimpleMessageListenerContainer。
 *
 * <p>队列/交换机/绑定关系由生产者服务通过 {@code raf.rabbit.bindings} 配置声明，
 * 消费者只需指定监听的队列名，支持 {@code ${...}} 占位符从配置中心读取。
 *
 * <p>并发参数优先级：注解值 > 全局配置（{@code raf.rabbit.consumer.concurrentConsumers}）。
 * 注解值为 -1（默认）时回退到全局配置。
 *
 * <p>使用示例：
 * <pre>{@code
 * // 使用全局并发配置
 * @RabbitMqConsumer(queue = "${mq.order.notify.queue:order.notify.queue}")
 * public class OrderNotifyConsumer extends AbstractRabbitConsumerListener { ... }
 *
 * // 覆盖并发配置（高优先级队列）
 * @RabbitMqConsumer(
 *     queue = "${mq.payment.queue:payment.queue}",
 *     concurrentConsumers = 5,
 *     maxConcurrentConsumers = 20
 * )
 * public class PaymentConsumer extends AbstractRabbitConsumerListener { ... }
 * }</pre>
 *
 * @author Jerry
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RabbitMqConsumer {

    /**
     * 监听的队列名，支持 {@code ${...}} 占位符。
     * 示例：{@code ${mq.order.queue:order.notify.queue}}
     */
    String queue();

    /**
     * ACK 模式，默认手动 ACK（MANUAL）。
     * 手动 ACK 由 AbstractRabbitConsumerListener 框架层统一处理。
     */
    AcknowledgeMode ackModel() default AcknowledgeMode.MANUAL;

    /**
     * 初始并发消费者数。
     * -1 表示使用全局配置 {@code raf.rabbit.consumer.concurrentConsumers}（默认 3）。
     */
    int concurrentConsumers() default -1;

    /**
     * 最大并发消费者数。
     * -1 表示使用全局配置 {@code raf.rabbit.consumer.maxConcurrentConsumers}（默认 10）。
     */
    int maxConcurrentConsumers() default -1;
}
