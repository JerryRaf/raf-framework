package com.raf.framework.rabbit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.amqp.core.AcknowledgeMode;

/**
 * @author Jerry
 * @date 2019/01/01
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RabbitMqConsumer {

    /**
     * 交换机名称
     * <p>
     * //
     */
    String exchange();

    /**
     * 路由规则. queue 根据该规则绑定到exchange上面来
     */
    String routingKey();

    /**
     * 绑定队列
     */
    String queue();

    /**
     * 是否手动消费确认 默认手动,目前只支持手动
     */
    AcknowledgeMode ackModel() default AcknowledgeMode.MANUAL;
}
