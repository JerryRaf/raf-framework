package com.raf.framework.rabbit;

import com.raf.framework.core.common.exception.BusinessException;
import com.raf.framework.core.common.exception.InfrastructureException;
import com.raf.framework.core.common.result.RafResponseEnum;
import com.raf.framework.core.jackson.JsonService;
import com.raf.framework.core.snowflake.SnowFlakeBuilder;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * RabbitMQ 消息发送门面。
 *
 * <p>普通消息：{@link #send(RabbitMqMessage, String, String)}
 * <p>延迟消息：{@link #sendDelay(RabbitMqMessage, String, String, int)}
 *   - exchange：死信 Exchange（消息先投递到此，TTL 到期后由 DLX 转发到 receive queue）
 *   - routingKey：死信队列的 routing key
 *   - second：消息级 TTL（秒），队列级 TTL 在 bindings 配置中设置时此参数传 0
 *
 * @author Jerry
 */
@Slf4j
@RequiredArgsConstructor
public class RabbitMqMessageSender {

    private final RabbitTemplate rabbitTemplate;
    private final JsonService json;

    /**
     * 发送普通消息。
     *
     * @param message  消息体
     * @param exchange 目标 Exchange
     * @param routeKey Routing Key
     */
    public void send(RabbitMqMessage message, String exchange, String routeKey) {
        String msgId = Optional.ofNullable(message.getMsgId())
                .orElseGet(SnowFlakeBuilder::generateId);
        message.setMsgId(msgId);
        CorrelationData data = new CorrelationData(msgId);
        try {
            rabbitTemplate.convertAndSend(exchange, routeKey, json.toJson(message), data);
            log.debug("Message sent. exchange={}, routeKey={}, msgId={}", exchange, routeKey, msgId);
        } catch (AmqpException ex) {
            log.error("RabbitMQ send failed. exchange={}, routeKey={}, msgId={}", exchange, routeKey, msgId, ex);
            throw new InfrastructureException(RafResponseEnum.SERVER_ERROR, "RabbitMQ send failed", ex);
        }
    }

    /**
     * 发送延迟消息（消息级 TTL）。
     *
     * @param message    消息体
     * @param exchange   死信 Exchange 名称（对应 bindings[].delay.dead-exchange）
     * @param routingKey 死信队列 routing key（对应 bindings[].delay.dead-routing-key）
     * @param second     延迟秒数；队列级 TTL 已在 bindings 配置时传 0
     */
    public void sendDelay(RabbitMqMessage message, String exchange, String routingKey, int second) {
        String msgId = Optional.ofNullable(message.getMsgId())
                .orElseGet(SnowFlakeBuilder::generateId);
        message.setMsgId(msgId);
        CorrelationData data = new CorrelationData(msgId);
        MessagePostProcessor ttlProcessor = msg -> {
            if (second > 0) {
                msg.getMessageProperties().setExpiration(String.valueOf((long) second * 1000));
            }
            return msg;
        };
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, json.toJson(message), ttlProcessor, data);
            log.debug("Delay message sent. exchange={}, routingKey={}, msgId={}, ttl={}s",
                    exchange, routingKey, msgId, second);
        } catch (AmqpException ex) {
            log.error("RabbitMQ delay send failed. exchange={}, routingKey={}, msgId={}",
                    exchange, routingKey, msgId, ex);
            throw new InfrastructureException(RafResponseEnum.SERVER_ERROR, "RabbitMQ delay send failed", ex);
        }
    }

    /**
     * 发送普通消息（字符串重载）。
     *
     * @param msgId       消息 ID，为 null 时自动生成
     * @param messageBody 消息内容（JSON 字符串）
     * @param exchange    目标 Exchange
     * @param routeKey    Routing Key
     */
    public void send(String msgId, String messageBody, String exchange, String routeKey) {
        if (StringUtils.isBlank(messageBody)) {
            throw new BusinessException(RafResponseEnum.PARAM_ERROR, "message body must not be blank");
        }
        RabbitMqMessage msg = new RabbitMqMessage();
        msg.setMsgId(msgId);
        msg.setMessage(messageBody);
        send(msg, exchange, routeKey);
    }

    /**
     * 发送延迟消息（字符串重载）。
     *
     * @param msgId       消息 ID，为 null 时自动生成
     * @param messageBody 消息内容（JSON 字符串）
     * @param exchange    死信 Exchange 名称
     * @param routingKey  死信队列 routing key
     * @param second      延迟秒数
     */
    public void sendDelay(String msgId, String messageBody, String exchange, String routingKey, int second) {
        if (StringUtils.isBlank(messageBody)) {
            throw new BusinessException(RafResponseEnum.PARAM_ERROR, "message body must not be blank");
        }
        RabbitMqMessage msg = new RabbitMqMessage();
        msg.setMsgId(msgId);
        msg.setMessage(messageBody);
        sendDelay(msg, exchange, routingKey, second);
    }
}
