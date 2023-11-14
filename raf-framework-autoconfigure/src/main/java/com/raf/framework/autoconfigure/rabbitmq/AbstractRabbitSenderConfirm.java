package com.raf.framework.autoconfigure.rabbitmq;

import com.raf.framework.autoconfigure.jackson.Json;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate.ConfirmCallback;
import org.springframework.amqp.rabbit.core.RabbitTemplate.ReturnCallback;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * @author Jerry
 * @date 2019/01/01
 * rabbitMQ 生产端确认 消息的确认及return
 */
@Slf4j
public abstract class AbstractRabbitSenderConfirm implements ConfirmCallback, ReturnCallback {

    @Autowired
    private Json json;

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
                    log.error("路由exchange失败，请核对");
                    giveUp(mqMessage, null, null, message.getExchange(), message.getRouteKey());
                    return;
                }
                //放到重试队列
                rabbitMessageCacheMgr.addRetry(mqMessage.getMsgId());
            }
            return;
        }
        // 如果是正常的则从当前队列移除
        rabbitMessageCacheMgr.remove(msgId);
    }

    /**
     * 消息从交换机成功到达队列，则returnedMessage方法不会执行
     * 消息从交换机未能成功到达队列&，则returnedMessage方法会执行
     *
     * @param message
     * @param replyCode
     * @param replyText
     * @param exchange
     * @param routingKey
     */
    @Override
    public void returnedMessage(Message message, int replyCode, String replyText, String exchange, String routingKey) {
        RabbitMqMessage rabbitMqMessage = json.strToObj(new String(message.getBody(), StandardCharsets.UTF_8), RabbitMqMessage.class);
        log.info("发送消息被退回. 消息内容:{}", json.objToString(rabbitMqMessage));
        if (rabbitMqMessage.isOverTimes()) {
            giveUp(rabbitMqMessage, replyCode, replyText, exchange, routingKey);
            return;
        }
        //放到重试队列
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
    public abstract void giveUp(RabbitMqMessage message, Integer replyCode, String replyText, String exchange, String routingKey);
}
	