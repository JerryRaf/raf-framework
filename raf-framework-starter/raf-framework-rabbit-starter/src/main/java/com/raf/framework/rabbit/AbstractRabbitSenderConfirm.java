package com.raf.framework.rabbit;

import com.raf.framework.core.jackson.JsonService;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate.ConfirmCallback;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Jerry
 * @date 2019/01/01
 * rabbitMQ 生产端确认 消息的确认及return
 */
@Slf4j
public abstract class AbstractRabbitSenderConfirm implements ConfirmCallback, RabbitTemplate.ReturnsCallback {

    @Autowired
    private JsonService json;

    @Autowired
    private RabbitMessageCacheMgr rabbitMessageCacheMgr;

    /**
     * confirm只会判断是否成功达到exchange
     *
     * @param correlationData
     * @param ack             返回的一个交换机确认状态 true为确认(正常），false为未确认
     * @param cause           未确认的一个原因，如果ack为true的话，此值为null
     */
    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        String msgId = correlationData.getId();
        if (!ack) {
            RabbitMessageCacheMgr.CachedMessage message = rabbitMessageCacheMgr.getNow(msgId);
            if (Optional.ofNullable(message).isPresent()) {
                RabbitMqMessage mqMessage = message.getMessage();
                if (mqMessage.isOverTimes()) {
                    log.error("Message routing to exchange failed, giving up. msgId={}", msgId);
                    giveUp(mqMessage, null, null, message.getExchange(), message.getRouteKey());
                    return;
                }
                rabbitMessageCacheMgr.addRetry(mqMessage.getMsgId());
            }
            return;
        }
        rabbitMessageCacheMgr.remove(msgId);
    }

    @Override
    public void returnedMessage(ReturnedMessage returnedMessage) {
        RabbitMqMessage rabbitMqMessage = json.parse(
                new String(returnedMessage.getMessage().getBody(), StandardCharsets.UTF_8), RabbitMqMessage.class);
        log.error("Message returned from exchange. content={}, replyText={}", json.toJson(rabbitMqMessage), returnedMessage.getReplyText());
        if (rabbitMqMessage.isOverTimes()) {
            giveUp(
                    rabbitMqMessage,
                    returnedMessage.getReplyCode(),
                    returnedMessage.getReplyText(),
                    returnedMessage.getExchange(),
                    returnedMessage.getRoutingKey());
            return;
        }
        rabbitMessageCacheMgr.addRetry(rabbitMqMessage.getMsgId());
    }

    /**
     * 失败放弃
     *
     * @param message
     * @param replyCode
     * @param replyText
     * @param exchange
     * @param routingKey
     */
    public abstract void giveUp(
            RabbitMqMessage message, Integer replyCode, String replyText, String exchange, String routingKey);
}
