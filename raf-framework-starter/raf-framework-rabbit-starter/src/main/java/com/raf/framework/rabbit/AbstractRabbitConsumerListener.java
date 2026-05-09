package com.raf.framework.rabbit;

import com.raf.framework.core.jackson.JsonService;
import com.raf.framework.core.spring.bean.SpringContext;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;

/**
 * RabbitMQ 消费者基类，封装手动 ACK、重试、失败回调。
 *
 * <p>使用方式：
 * <ol>
 *   <li>继承此类，实现 {@link #onMessage} 处理业务逻辑</li>
 *   <li>实现 {@link #onFailure} 处理最终失败（持久化到 DB）</li>
 *   <li>按需重写 {@link #retry} 控制是否重试（默认不重试）</li>
 *   <li>在类上加 {@link RabbitMqConsumer} 注解指定监听队列</li>
 * </ol>
 *
 * <p>消费流程：
 * <pre>
 * onMessage(RabbitMqMessage)
 *   ├── 成功 → basicAck
 *   └── 异常
 *         ├── retry() == true → basicNack(requeue=true)  重新入队
 *         └── retry() == false → onFailure() → basicReject(requeue=false)  进死信队列
 * </pre>
 *
 * @author Jerry
 */
@Slf4j
public abstract class AbstractRabbitConsumerListener implements ChannelAwareMessageListener {

    @Override
    public void onMessage(Message message, Channel channel) throws Exception {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        RabbitMqMessage rabbitMqMessage = null;

        try {
            JsonService json = SpringContext.getBean(JsonService.class);
            rabbitMqMessage = json.parse(body, RabbitMqMessage.class);
            this.onMessage(rabbitMqMessage);
            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            log.error("Message consumption failed, msgId={}, error={}",
                    rabbitMqMessage != null ? rabbitMqMessage.getMsgId() : "unknown",
                    ex.getMessage(), ex);

            if (rabbitMqMessage != null && retry(rabbitMqMessage)) {
                log.warn("Retrying message, msgId={}", rabbitMqMessage.getMsgId());
                channel.basicNack(deliveryTag, false, true);
            } else {
                doGiveUp(message, channel, deliveryTag, ex.getMessage());
            }
        }
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
     * 消息最终消费失败的兜底处理（不再重试时调用）。
     * 业务侧应将失败消息持久化到 DB，以便人工补偿。
     *
     * @param message 原始 AMQP 消息
     * @param error   错误描述
     * @throws Exception 允许抛出，框架会捕获并继续 reject
     */
    public abstract void onFailure(Message message, String error) throws Exception;

    /**
     * 是否重试。默认不重试，子类按需重写。
     *
     * <p>注意：返回 true 会将消息重新入队（basicNack requeue=true），
     * 如果消费逻辑存在 bug 会导致无限循环，建议配合 {@link RabbitMqMessage#isOverTimes()} 限制次数。
     *
     * @param message 失败的消息
     * @return true 重新入队，false 进死信队列
     */
    public boolean retry(RabbitMqMessage message) {
        return false;
    }
}
