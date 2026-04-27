package com.raf.framework.rocketmq;

import com.raf.framework.core.jackson.JsonService;
import com.raf.framework.core.spring.bean.SpringContext;
import com.raf.framework.core.trace.ContextHolder;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.consumer.listener.*;
import org.apache.rocketmq.common.message.MessageExt;

/**
 * RocketMQ消费者监听器抽象类
 * 提供消息消费的通用逻辑：反序列化、异常处理、链路追踪等
 *
 * @author RAF Framework
 * @date 2025/01/01
 */
@Slf4j
public abstract class AbstractRocketMqConsumerListener<T> implements MessageListenerConcurrently, MessageListenerOrderly {

    private final Class<T> messageType;
    private final JsonService jsonService;

    @SuppressWarnings("unchecked")
    public AbstractRocketMqConsumerListener() {
        // 获取泛型类型
        Type superClass = getClass().getGenericSuperclass();
        if (superClass instanceof ParameterizedType) {
            Type[] typeArguments = ((ParameterizedType) superClass).getActualTypeArguments();
            this.messageType = (Class<T>) typeArguments[0];
        } else {
            this.messageType = null;
        }
        this.jsonService = SpringContext.getBean(JsonService.class);
    }

    /**
     * 并发消费（普通消息、延时消息）
     */
    @Override
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
        for (MessageExt msg : msgs) {
            try {
                // 设置链路追踪ID
                String traceId = msg.getUserProperty("traceId");
                if (StringUtils.isNotBlank(traceId)) {
                    ContextHolder.setTraceId(traceId);
                }

                // 反序列化消息
                String body = new String(msg.getBody(), StandardCharsets.UTF_8);
                T message = deserializeMessage(body);

                // 业务处理
                boolean success = handleMessage(message, msg);

                if (!success) {
                    log.warn("RocketMQ message consumption failed, will retry. topic:{}, msgId:{}, tag:{}",
                            msg.getTopic(), msg.getMsgId(), msg.getTags());
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                }

                log.debug("RocketMQ message consumed successfully. topic:{}, msgId:{}, tag:{}",
                        msg.getTopic(), msg.getMsgId(), msg.getTags());

            } catch (Exception e) {
                log.error("RocketMQ消息消费异常. topic:{}, msgId:{}, tag:{}",
                        msg.getTopic(), msg.getMsgId(), msg.getTags(), e);
                return ConsumeConcurrentlyStatus.RECONSUME_LATER;
            } finally {
                ContextHolder.clearTraceId();
            }
        }
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }

    /**
     * 顺序消费（顺序消息）
     */
    @Override
    public ConsumeOrderlyStatus consumeMessage(List<MessageExt> msgs, ConsumeOrderlyContext context) {
        // 顺序消息必须确保消费成功才能消费下一条
        context.setAutoCommit(true);

        for (MessageExt msg : msgs) {
            try {
                // 设置链路追踪ID
                String traceId = msg.getUserProperty("traceId");
                if (StringUtils.isNotBlank(traceId)) {
                    ContextHolder.setTraceId(traceId);
                }

                // 反序列化消息
                String body = new String(msg.getBody(), StandardCharsets.UTF_8);
                T message = deserializeMessage(body);

                // 业务处理
                boolean success = handleMessage(message, msg);

                if (!success) {
                    log.warn("RocketMQ orderly message consumption failed, suspending queue. topic:{}, msgId:{}, tag:{}",
                            msg.getTopic(), msg.getMsgId(), msg.getTags());
                    return ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
                }

                log.debug("RocketMQ orderly message consumed successfully. topic:{}, msgId:{}, tag:{}",
                        msg.getTopic(), msg.getMsgId(), msg.getTags());

            } catch (Exception e) {
                log.error("RocketMQ顺序消息消费异常. topic:{}, msgId:{}, tag:{}",
                        msg.getTopic(), msg.getMsgId(), msg.getTags(), e);
                return ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
            } finally {
                ContextHolder.clearTraceId();
            }
        }
        return ConsumeOrderlyStatus.SUCCESS;
    }

    /**
     * 反序列化消息
     */
    private T deserializeMessage(String body) {
        if (messageType == null || messageType == String.class) {
            return (T) body;
        }
        return jsonService.parse(body, messageType);
    }

    /**
     * 业务处理方法，子类实现具体的消费逻辑
     *
     * @param message    反序列化后的消息体
     * @param messageExt 原始消息对象（包含元数据）
     * @return true-消费成功，false-消费失败需要重试
     */
    protected abstract boolean handleMessage(T message, MessageExt messageExt);
}
