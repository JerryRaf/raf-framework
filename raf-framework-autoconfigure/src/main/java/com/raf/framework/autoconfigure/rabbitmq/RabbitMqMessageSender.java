package com.raf.framework.autoconfigure.rabbitmq;

import com.raf.framework.autoconfigure.jackson.Json;
import com.raf.framework.autoconfigure.snowflake.SnowFlakeBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Optional;

/**
 * @author Jerry
 * @date 2019/01/01
 * rabbitMQ 消息发送
 */
@Slf4j
public class RabbitMqMessageSender {

    private RabbitTemplate rabbitTemplate;
    private RabbitMqProperties.RabbitMqProvider rabbitMqProvider;
    private RabbitMqProperties.RabbitMqDelayProvider rabbitMqDelayProvider;
    private RabbitMessageCacheMgr rabbitMessageCacheMgr;
    private Json json;

    public RabbitMqMessageSender(RabbitTemplate rabbitTemplate, RabbitMqProperties.RabbitMqProvider rabbitMqProvider, RabbitMqProperties.RabbitMqDelayProvider rabbitMqDelayProvider, Json json, RabbitMessageCacheMgr rabbitMessageCacheMgr) {
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitMqProvider = rabbitMqProvider;
        this.rabbitMqDelayProvider = rabbitMqDelayProvider;
        this.json = json;
        this.rabbitMessageCacheMgr = rabbitMessageCacheMgr;
    }

    public void send(RabbitMqMessage message, String routeKey) {
        Optional.ofNullable(message).ifPresent(
                m -> {
                    String msgId = Optional.ofNullable(m.getMsgId()).orElseGet(SnowFlakeBuilder::generateId);
                    message.setMsgId(msgId);
                    CorrelationData data = new CorrelationData(msgId);
                    if (rabbitTemplate.isConfirmListener() && rabbitTemplate.isReturnListener()) {
                        // 次数加1,放到当前缓存中
                        message.preSend();
                        rabbitMessageCacheMgr.add2Now(message, rabbitMqProvider.getExchange(), routeKey);
                    }
                    try {
                        rabbitTemplate.convertAndSend(rabbitMqProvider.getExchange(), routeKey, json.objToString(message), data);
                    } catch (AmqpException ex) {
                        log.error("Mq发送消息失败:{}", ex.getMessage());
                        rabbitMessageCacheMgr.addRetry(msgId);
                    }
                }
        );
    }

    public void sendDelay(RabbitMqMessage message, String businessName, Integer second) {
        Optional.ofNullable(message).ifPresent(
                m -> {
                    String msgId = Optional.ofNullable(m.getMsgId()).orElseGet(() -> SnowFlakeBuilder.generateId());
                    message.setMsgId(msgId);
                    CorrelationData data = new CorrelationData(msgId);
                    try {

                        MessagePostProcessor processor = msg -> {
                            msg.getMessageProperties().setExpiration(String.valueOf(second * 1000));
                            return msg;
                        };
                        rabbitTemplate.convertAndSend(rabbitMqDelayProvider.getDeadExchange(), businessName.concat(".dead.route"), json.objToString(message), processor, data);
                    } catch (AmqpException ex) {
                        log.error("Mq发送延迟消息失败:{}", ex.getMessage());
                    }
                }
        );
    }

    /**
     *
     * @param message
     * @param routeKey
     */
    public void reSend(RabbitMqMessage message, String routeKey) {
        if (Optional.ofNullable(message).isPresent() && Optional.ofNullable(message.getMsgId()).isPresent()) {
            send(message, routeKey);
            return;
        }
        throw new RuntimeException("消息id不存在，不能重试发送");
    }


    /**
     * 发送消息
     *
     * @param msgId    消息id
     * @param message  消息内容
     * @param routeKey 路由规则
     */
    public void send(String msgId, String message, String routeKey) {
        Optional.ofNullable(message).ifPresent(m -> {
            RabbitMqMessage rabbitMqMessage = new RabbitMqMessage();
            rabbitMqMessage.setMessage(message);
            rabbitMqMessage.setMsgId(msgId);
            send(rabbitMqMessage, routeKey);
        });
    }

    /**
     * 发送延迟消息
     *
     * @param msgId        消息id
     * @param message      消息内容
     * @param businessName 业务名称
     */
    public void sendDelay(String msgId, String message, String businessName, Integer second) {
        Optional.ofNullable(message).ifPresent(m -> {
            RabbitMqMessage rabbitMqMessage = new RabbitMqMessage();
            rabbitMqMessage.setMessage(message);
            rabbitMqMessage.setMsgId(msgId);
            sendDelay(rabbitMqMessage, businessName, second);
        });
    }
}
