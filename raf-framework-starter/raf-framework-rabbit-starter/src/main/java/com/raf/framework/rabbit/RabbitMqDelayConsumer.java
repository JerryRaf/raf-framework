package com.raf.framework.rabbit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.amqp.core.AcknowledgeMode;

/**
 * 延迟消费者注解（已废弃）。
 *
 * <p>请迁移到 {@link RabbitMqConsumer}，通过 {@code queue} 属性直接指定监听队列名。
 * 延迟队列的实际消费队列名在 {@code raf.rabbit.bindings[].delay.deadQueue} 中配置。
 *
 * @author Jerry
 * @deprecated 使用 {@link RabbitMqConsumer} 替代
 */
@Deprecated
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RabbitMqDelayConsumer {

    /**
     * 业务名称
     */
    String businessName();

    /**
     * 交换机名称
     */
    String exchange();

    /**
     * 是否手动消费确认 默认手动
     */
    AcknowledgeMode ackModel() default AcknowledgeMode.MANUAL;
}
