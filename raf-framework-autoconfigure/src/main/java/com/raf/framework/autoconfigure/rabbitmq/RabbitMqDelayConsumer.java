package com.raf.framework.autoconfigure.rabbitmq;

import org.springframework.amqp.core.AcknowledgeMode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Jerry
 * @date 2019/01/01
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RabbitMqDelayConsumer {

    /**
     * 业务名称
     *
     */
    String businessName();

    /**
     * 交换机名称
     *
     */
    String exchange();

    /**
     * 是否手动消费确认 默认手动
     *
     */
    AcknowledgeMode ackModel() default AcknowledgeMode.MANUAL;

}
