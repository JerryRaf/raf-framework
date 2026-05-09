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
 * <p>使用示例：
 * <pre>{@code
 * @RabbitMqConsumer(queue = "${mq.order.notify.queue:order.notify.queue}")
 * public class OrderNotifyConsumer extends AbstractRabbitConsumerListener {
 *     @Override
 *     public void onMessage(RabbitMqMessage message) throws Exception { ... }
 *
 *     @Override
 *     public void onFailure(Message message, String error) throws Exception { ... }
 * }
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
}
