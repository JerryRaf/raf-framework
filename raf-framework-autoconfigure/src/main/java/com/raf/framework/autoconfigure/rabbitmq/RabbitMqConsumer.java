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
public @interface RabbitMqConsumer {

    /**
     * 绑定队列
     *
     */
    String queue();

    /**
     * 交换机名称
     *
     */
    String exchange();

    /**
     * 是否手动消费确认 默认手动,目前只支持手动
     *
     */
    AcknowledgeMode ackModel() default AcknowledgeMode.MANUAL;

    /**
     * 路由规则. queue 根据该规则绑定到exchange上面来
     *
     */
    String routingKey();


}
