package com.raf.framework.rabbit;

import com.raf.framework.core.jackson.JsonService;

import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate.ConfirmCallback;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 生产者发送确认基类。
 *
 * <p>当 {@code raf.rabbit.provider.ack=true} 时，框架自动将此 Bean 注册为
 * RabbitTemplate 的 ConfirmCallback 和 ReturnsCallback。
 *
 * <p>业务侧继承此类并实现 {@link #giveUp}，在消息彻底无法投递时持久化到 DB 做补偿。
 *
 * <p>使用示例：
 * <pre>{@code
 * @Component
 * public class OrderSenderConfirm extends AbstractRabbitSenderConfirm {
 *     @Override
 *     public void giveUp(RabbitMqMessage message, Integer replyCode,
 *                        String replyText, String exchange, String routingKey) {
 *         // 持久化失败消息到 DB，人工补偿
 *         failedMessageRepo.save(FailedMessage.of(message, exchange, routingKey, replyText));
 *     }
 * }
 * }</pre>
 *
 * @author Jerry
 */
@Slf4j
public abstract class AbstractRabbitSenderConfirm
        implements ConfirmCallback, RabbitTemplate.ReturnsCallback {

    @Autowired
    private JsonService jsonService;

    /**
     * confirm 回调：消息是否成功到达 Exchange。
     * ack=false 时调用 {@link #onConfirmFail} 通知业务侧。
     */
    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        if (correlationData == null) {
            return;
        }
        if (!ack) {
            String msgId = correlationData.getId();
            log.error("Message failed to reach exchange, msgId={}, cause={}", msgId, cause);
            onConfirmFail(msgId, cause);
        }
    }

    /**
     * return 回调：消息到达 Exchange 但无法路由到任何 Queue。
     * 调用 {@link #giveUp} 通知业务侧处理。
     */
    @Override
    public void returnedMessage(ReturnedMessage returnedMessage) {
        String body = new String(returnedMessage.getMessage().getBody(), StandardCharsets.UTF_8);
        log.error("Message returned from exchange, exchange={}, routingKey={}, replyCode={}, replyText={}",
                returnedMessage.getExchange(),
                returnedMessage.getRoutingKey(),
                returnedMessage.getReplyCode(),
                returnedMessage.getReplyText());
        RabbitMqMessage message = tryParseMessage(body);
        try {
            giveUp(message,
                    returnedMessage.getReplyCode(),
                    returnedMessage.getReplyText(),
                    returnedMessage.getExchange(),
                    returnedMessage.getRoutingKey());
        } catch (Exception ex) {
            log.error("giveUp callback threw exception, body={}", body, ex);
        }
    }

    /**
     * confirm 失败回调（消息未到达 Exchange，如网络问题）。
     * 默认记录错误日志，子类可重写做告警或重发。
     *
     * @param msgId 消息 ID
     * @param cause 失败原因
     */
    protected void onConfirmFail(String msgId, String cause) {
        log.error("Message confirm failed - msgId: {}, cause: {}. Consider implementing onConfirmFail() for retry or alerting.", msgId, cause);
    }

    /**
     * 消息彻底无法投递时的兜底处理（到达 Exchange 但无法路由到 Queue）。
     * 业务侧必须实现此方法，将失败消息持久化到 DB 以便人工补偿。
     *
     * @param message    消息体（解析失败时为 null）
     * @param replyCode  AMQP reply code
     * @param replyText  AMQP reply text
     * @param exchange   目标 Exchange
     * @param routingKey 路由 Key
     */
    public abstract void giveUp(
            RabbitMqMessage message,
            Integer replyCode,
            String replyText,
            String exchange,
            String routingKey);

    private RabbitMqMessage tryParseMessage(String body) {
        try {
            return jsonService.parse(body, RabbitMqMessage.class);
        } catch (Exception e) {
            log.warn("Failed to parse message body as RabbitMqMessage: {}", body);
            return null;
        }
    }
}
