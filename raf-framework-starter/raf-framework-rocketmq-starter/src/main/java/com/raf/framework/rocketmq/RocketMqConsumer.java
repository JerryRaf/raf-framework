package com.raf.framework.rocketmq;

import java.lang.annotation.*;
import org.apache.rocketmq.remoting.protocol.heartbeat.MessageModel;
import org.springframework.stereotype.Component;

/**
 * RocketMQ消费者注解
 * 标注在消费者监听类上，自动注册消费者
 *
 * @author RAF Framework
 * @date 2025/01/01
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface RocketMqConsumer {

    /**
     * 消费者组名（支持SpEL表达式）
     */
    String consumerGroup() default "${raf.rocketmq.consumer.group:DEFAULT_CONSUMER_GROUP}";

    /**
     * 主题（支持SpEL表达式）
     */
    String topic();

    /**
     * 标签过滤表达式（支持SpEL表达式）
     * * 表示订阅所有消息
     * TagA || TagB 表示订阅TagA或TagB的消息
     */
    String tag() default "*";

    /**
     * 消费模式
     */
    MessageModel messageModel() default MessageModel.CLUSTERING;

    /**
     * 消费线程数
     */
    int consumeThreadMin() default 20;

    /**
     * 消费线程最大数
     */
    int consumeThreadMax() default 64;

    /**
     * 消息类型
     */
    RocketMqMessage.MessageType messageType() default RocketMqMessage.MessageType.NORMAL;
}
