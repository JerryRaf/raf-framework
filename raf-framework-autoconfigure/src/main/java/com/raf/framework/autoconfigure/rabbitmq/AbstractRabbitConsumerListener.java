package com.raf.framework.autoconfigure.rabbitmq;

import com.rabbitmq.client.Channel;
import com.raf.framework.autoconfigure.jackson.Json;
import com.raf.framework.autoconfigure.spring.bean.SpringContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * @author Jerry
 * @date 2019/01/01
 */
@Slf4j
public abstract class AbstractRabbitConsumerListener implements ChannelAwareMessageListener {
    /**
     * 消费消息
     */
    @Override
    public void onMessage(Message message, Channel channel) throws Exception {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        RabbitMqMessage rabbitMqMessage = null;
        try {
            Json json = SpringContext.getBean(Json.class);
            rabbitMqMessage = json.strToObj(body, RabbitMqMessage.class);
            this.onMessage(rabbitMqMessage);
        } catch (Exception ex) {
            log.error("Mq消息消费失败:{}", ex.getMessage());

            if (Optional.ofNullable(rabbitMqMessage).isPresent() && retry(rabbitMqMessage)) {
                //重新消费
                channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
            } else {
                giveUp(message, channel, message.getMessageProperties().getDeliveryTag(), ex.getMessage());
            }
        }
        //消费成功
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), true);
    }

    /**
     * @param message
     * @param channel
     * @param deliveryTag
     * @param error
     */
    private void giveUp(Message message, Channel channel, long deliveryTag, String error) {
        try {
            onFailure(message, error);
        } catch (Exception ex) {
            log.error("Mq消息消费回滚异常body:{}", new String(message.getBody(), StandardCharsets.UTF_8));
        } finally {
            try {
                channel.basicReject(deliveryTag, false);
            } catch (IOException ex) {
                log.error("Mq消息放弃失败:{}", ex.getMessage());
            }
        }
    }


    /**
     * 消息接收
     *
     * @param message
     * @throws Exception
     */
    public abstract void onMessage(RabbitMqMessage message) throws Exception;

    /**
     * 消息失败
     *
     * @param message
     * @param error
     * @throws Exception
     */
    public abstract void onFailure(Message message, String error) throws Exception;

    /**
     * 判断是否重新消费，出现异常的情况下（可重写）
     *
     * @param message
     * @return
     */
    public boolean retry(RabbitMqMessage message) {
        return false;
    }

}
