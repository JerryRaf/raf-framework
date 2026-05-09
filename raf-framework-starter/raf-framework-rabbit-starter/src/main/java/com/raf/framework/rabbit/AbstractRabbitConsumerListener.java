package com.raf.framework.rabbit;

import com.raf.framework.core.jackson.JsonService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * RabbitMQ 消费者基类，封装手动 ACK、重试、失败回调。
 *
 * <p>使用方式：
 * <ol>
 *   <li>继承此类，实现 {@link #onMessage} 处理业务逻辑</li>
 *   <li>实现 {@link #onFailure} 处理最终失败（持久化到 DB）</li>
 *   <li>按需重写 {@link #maxRetryTimes} 控制最大重试次数（默认 3 次）</li>
 *   <li>在类上加 {@link RabbitMqConsumer} 注解指定监听队列</li>
 * </ol>
 *
 * <p>消费流程：
 * <pre>
 * onMessage(RabbitMqMessage)
 *   ├── 成功 → basicAck
 *   └── 异常
 *         ├── times < maxRetryTimes() → basicAck + 重新发布（携带递增 times）
 *         └── times >= maxRetryTimes() → onFailure() → basicReject(requeue=false) 进死信队列
 * </pre>
 *
 * <p>重试原理：消费失败后先 ACK 当前消息，再将 times+1 后的消息重新发布到同一队列。
 * 这样重试计数持久化在消息体中，不会因重新入队而丢失，彻底避免无限循环。
 *
 * @author Jerry
 */
@Slf4j
public abstract class AbstractRabbitConsumerListener implements ChannelAwareMessageListener {

    @Autowired
    private JsonService jsonService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public void onMessage(Message message, Channel channel) throws Exception {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        RabbitMqMessage rabbitMqMessage = null;

        try {
            rabbitMqMessage = jsonService.parse(body, RabbitMqMessage.class);
            this.onMessage(rabbitMqMessage);
            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            log.error("Message consumption failed, msgId={}, error={}",
                    rabbitMqMessage != null ? rabbitMqMessage.getMsgId() : "unknown",
                    ex.getMessage(), ex);

            if (rabbitMqMessage != null && rabbitMqMessage.getTimes() < maxRetryTimes()) {
                // 先 ACK 当前消息，再将 times+1 的消息重新发布，确保重试计数持久化
                rabbitMqMessage.incrementRetryCount();
                log.warn("Scheduling retry, msgId={}, retryCount={}/{}",
                        rabbitMqMessage.getMsgId(), rabbitMqMessage.getTimes(), maxRetryTimes());
                try {
                    channel.basicAck(deliveryTag, false);
                    republish(message, rabbitMqMessage);
                } catch (Exception republishEx) {
                    log.error("Republish failed, falling back to reject. msgId={}", rabbitMqMessage.getMsgId(), republishEx);
                    doGiveUp(message, channel, deliveryTag, ex.getMessage());
                }
            } else {
                doGiveUp(message, channel, deliveryTag, ex.getMessage());
            }
        }
    }

    /**
     * 将更新了重试计数的消息重新发布到原队列。
     * 保留原始消息的 exchange 和 routingKey，确保路由一致。
     */
    private void republish(Message original, RabbitMqMessage updated) {
        String exchange = original.getMessageProperties().getReceivedExchange();
        String routingKey = original.getMessageProperties().getReceivedRoutingKey();
        byte[] newBody = jsonService.toJson(updated).getBytes(StandardCharsets.UTF_8);

        MessageProperties props = new MessageProperties();
        props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        props.setContentEncoding(StandardCharsets.UTF_8.name());
        Message newMessage = MessageBuilder.withBody(newBody).andProperties(props).build();

        rabbitTemplate.send(exchange, routingKey, newMessage);
        log.debug("Message republished for retry. exchange={}, routingKey={}, msgId={}",
                exchange, routingKey, updated.getMsgId());
    }

    private void doGiveUp(Message message, Channel channel, long deliveryTag, String error) {
        try {
            onFailure(message, error);
        } catch (Exception ex) {
            log.error("onFailure callback threw exception, body={}",
                    new String(message.getBody(), StandardCharsets.UTF_8), ex);
        } finally {
            try {
                channel.basicReject(deliveryTag, false);
            } catch (IOException ex) {
                log.error("basicReject failed: {}", ex.getMessage());
            }
        }
    }

    /**
     * 处理业务消息。
     *
     * @param message 反序列化后的消息
     * @throws Exception 抛出异常时触发重试或失败回调
     */
    public abstract void onMessage(RabbitMqMessage message) throws Exception;

    /**
     * 消息最终消费失败的兜底处理（超过最大重试次数时调用）。
     * 业务侧应将失败消息持久化到 DB，以便人工补偿。
     *
     * @param message 原始 AMQP 消息
     * @param error   错误描述
     * @throws Exception 允许抛出，框架会捕获并继续 reject
     */
    public abstract void onFailure(Message message, String error) throws Exception;

    /**
     * 最大重试次数，默认 3 次。子类可重写自定义。
     *
     * <p>消息的 {@code times} 字段从 0 开始，每次重试递增。
     * 当 {@code times >= maxRetryTimes()} 时不再重试，直接调用 {@link #onFailure}。
     *
     * @return 最大重试次数
     */
    public int maxRetryTimes() {
        return 3;
    }
}
