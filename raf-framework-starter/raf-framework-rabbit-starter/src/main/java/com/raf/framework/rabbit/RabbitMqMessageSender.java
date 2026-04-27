package com.raf.framework.rabbit;

import com.raf.framework.core.common.exception.BusinessException;
import com.raf.framework.core.common.result.RafResponseEnum;
import com.raf.framework.core.jackson.JsonService;
import com.raf.framework.core.snowflake.SnowFlakeBuilder;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * @author Jerry
 * @date 2019/01/01
 * rabbitMQ 消息发送
 */
@Slf4j
public class RabbitMqMessageSender {

    private RabbitTemplate rabbitTemplate;
    private RabbitMqProperties.RabbitMqDelayProvider rabbitMqDelayProvider;
    private RabbitMessageCacheMgr rabbitMessageCacheMgr;
    private JsonService json;

    public RabbitMqMessageSender(
            RabbitTemplate rabbitTemplate,
            RabbitMqProperties.RabbitMqDelayProvider rabbitMqDelayProvider,
            JsonService json,
            RabbitMessageCacheMgr rabbitMessageCacheMgr) {
        this.rabbitTemplate = rabbitTemplate;
        this.json = json;
        this.rabbitMqDelayProvider = rabbitMqDelayProvider;
        this.rabbitMessageCacheMgr = rabbitMessageCacheMgr;
    }

    public void send(RabbitMqMessage message, String exchange, String routeKey) {
        String msgId = Optional.ofNullable(message.getMsgId()).orElseGet(SnowFlakeBuilder::generateId);
        message.setMsgId(msgId);
        CorrelationData data = new CorrelationData(msgId);
        if (rabbitTemplate.isConfirmListener() && rabbitTemplate.isReturnListener()) {
            // 次数加1,放到当前缓存中
            message.preSend();
            rabbitMessageCacheMgr.add2Now(message, exchange, routeKey);
        }
        try {
            rabbitTemplate.convertAndSend(exchange, routeKey, json.toJson(message), data);
        } catch (AmqpException ex) {
            log.error("RabbitMQ send failed: {}", ex.getMessage());
            rabbitMessageCacheMgr.addRetry(msgId);
        }
    }

    public void sendDelay(RabbitMqMessage message, String businessName, Integer second) {
        Optional.ofNullable(message).ifPresent(m -> {
            String msgId = Optional.ofNullable(m.getMsgId()).orElseGet(SnowFlakeBuilder::generateId);
            message.setMsgId(msgId);
            CorrelationData data = new CorrelationData(msgId);
            try {
                MessagePostProcessor processor = msg -> {
                    msg.getMessageProperties().setExpiration(String.valueOf(second * 1000));
                    return msg;
                };
                rabbitTemplate.convertAndSend(
                        rabbitMqDelayProvider.getDeadExchange(),
                        businessName.concat(".dead.route"),
                        json.toJson(message),
                        processor,
                        data);
            } catch (AmqpException ex) {
                log.error("RabbitMQ delay send failed: {}", ex.getMessage());
            }
        });
    }

    public void reSend(RabbitMqMessage message, String exchange, String routeKey) {
        if (Optional.ofNullable(message).isPresent()
                && Optional.ofNullable(message.getMsgId()).isPresent()) {
            send(message, exchange, routeKey);
            return;
        }
        throw new RuntimeException("Message id is missing, cannot retry send");
    }

    /**
     * 发送消息
     *
     * @param msgId    消息id
     * @param message  消息内容
     * @param routeKey 路由规则
     */
    public void send(String msgId, String message, String exchange, String routeKey) {
        if (StringUtils.isEmpty(message)) {
            log.warn("msg is null or empty.");
            throw new BusinessException(RafResponseEnum.PARAM_ERROR);
        }

        RabbitMqMessage rabbitMqMessage = new RabbitMqMessage();
        rabbitMqMessage.setMessage(message);
        rabbitMqMessage.setMsgId(msgId);
        send(rabbitMqMessage, exchange, routeKey);
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
