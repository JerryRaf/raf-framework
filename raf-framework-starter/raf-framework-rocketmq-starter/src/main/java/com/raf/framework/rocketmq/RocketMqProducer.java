package com.raf.framework.rocketmq;

import com.raf.framework.core.common.exception.InfrastructureException;
import com.raf.framework.core.common.result.RafResponseEnum;
import com.raf.framework.core.jackson.JsonService;
import com.raf.framework.core.trace.ContextHolder;

import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.*;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageQueue;
import org.springframework.beans.factory.DisposableBean;

/**
 * RocketMQ producer wrapper.
 * Supports: normal, orderly, delay, and transaction messages.
 *
 * @author RAF Framework
 * @since 2025-01-01
 */
@Slf4j
public class RocketMqProducer implements DisposableBean {

    private final DefaultMQProducer producer;
    private final TransactionMQProducer transactionProducer;
    private final JsonService jsonService;
    private final boolean enableTransaction;

    public RocketMqProducer(DefaultMQProducer producer, TransactionMQProducer transactionProducer,
                            JsonService jsonService, boolean enableTransaction) {
        this.producer = producer;
        this.transactionProducer = transactionProducer;
        this.jsonService = jsonService;
        this.enableTransaction = enableTransaction;
    }

    /**
     * Send a normal message synchronously.
     *
     * @param mqMessage message to send
     * @return send result
     */
    public SendResult sendSync(RocketMqMessage<?> mqMessage) {
        try {
            Message message = buildMessage(mqMessage);
            return producer.send(message);
        } catch (Exception e) {
            log.error("RocketMQ sync send failed, topic:{}, tag:{}, key:{}", mqMessage.getTopic(), mqMessage.getTag(), mqMessage.getKey(), e);
            throw new InfrastructureException(RafResponseEnum.SERVER_ERROR, e);
        }
    }

    /**
     * Send a normal message synchronously (simplified).
     *
     * @param topic message topic
     * @param tag   message tag
     * @param body  message body
     * @return send result
     */
    public SendResult sendSync(String topic, String tag, Object body) {
        RocketMqMessage<?> message = RocketMqMessage.builder().topic(topic).tag(tag).body(body)
                .messageType(RocketMqMessage.MessageType.NORMAL).build();
        return sendSync(message);
    }

    /**
     * Send a normal message asynchronously.
     *
     * @param mqMessage message to send
     * @param callback  send callback
     */
    public void sendAsync(RocketMqMessage<?> mqMessage, SendCallback callback) {
        try {
            Message message = buildMessage(mqMessage);
            producer.send(message, callback);
        } catch (Exception e) {
            log.error("RocketMQ async send failed, topic:{}, tag:{}, key:{}", mqMessage.getTopic(), mqMessage.getTag(), mqMessage.getKey(), e);
            throw new InfrastructureException(RafResponseEnum.SERVER_ERROR, e);
        }
    }

    /**
     * Send a normal message asynchronously (simplified).
     *
     * @param topic    message topic
     * @param tag      message tag
     * @param body     message body
     * @param callback send callback
     */
    public void sendAsync(String topic, String tag, Object body, SendCallback callback) {
        RocketMqMessage<?> message = RocketMqMessage.builder().topic(topic).tag(tag).body(body)
                .messageType(RocketMqMessage.MessageType.NORMAL).build();
        sendAsync(message, callback);
    }

    /**
     * Send one-way (fire and forget, highest throughput).
     *
     * @param mqMessage message to send
     */
    public void sendOneway(RocketMqMessage<?> mqMessage) {
        try {
            Message message = buildMessage(mqMessage);
            producer.sendOneway(message);
        } catch (Exception e) {
            log.error("RocketMQ oneway send failed, topic:{}, tag:{}, key:{}", mqMessage.getTopic(), mqMessage.getTag(), mqMessage.getKey(), e);
            throw new InfrastructureException(RafResponseEnum.SERVER_ERROR, e);
        }
    }

    /**
     * Send an orderly message synchronously.
     *
     * @param mqMessage message with shardingKey set
     * @return send result
     */
    public SendResult sendOrderlySync(RocketMqMessage<?> mqMessage) {
        if (StringUtils.isBlank(mqMessage.getShardingKey())) {
            throw new IllegalArgumentException("Orderly message requires a shardingKey");
        }

        try {
            Message message = buildMessage(mqMessage);
            return producer.send(message, new MessageQueueSelector() {
                @Override
                public MessageQueue select(java.util.List<MessageQueue> mqs, Message msg, Object arg) {
                    int index = Math.abs(arg.hashCode()) % mqs.size();
                    return mqs.get(index);
                }
            }, mqMessage.getShardingKey());
        } catch (Exception e) {
            log.error("RocketMQ orderly send failed, topic:{}, tag:{}, key:{}, shardingKey:{}",
                    mqMessage.getTopic(), mqMessage.getTag(), mqMessage.getKey(), mqMessage.getShardingKey(), e);
            throw new InfrastructureException(RafResponseEnum.SERVER_ERROR, e);
        }
    }

    /**
     * Send a delay message synchronously.
     * delayLevel: 1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h
     *
     * @param mqMessage message with delayLevel set (1-18)
     * @return send result
     */
    public SendResult sendDelaySync(RocketMqMessage<?> mqMessage) {
        if (mqMessage.getDelayLevel() <= 0 || mqMessage.getDelayLevel() > 18) {
            throw new IllegalArgumentException("Delay level must be between 1 and 18");
        }

        try {
            Message message = buildMessage(mqMessage);
            message.setDelayTimeLevel(mqMessage.getDelayLevel());
            return producer.send(message);
        } catch (Exception e) {
            log.error("RocketMQ delay send failed, topic:{}, tag:{}, key:{}, delayLevel:{}",
                    mqMessage.getTopic(), mqMessage.getTag(), mqMessage.getKey(), mqMessage.getDelayLevel(), e);
            throw new InfrastructureException(RafResponseEnum.SERVER_ERROR, e);
        }
    }

    /**
     * Send a transaction message.
     *
     * @param mqMessage message to send
     * @param arg       argument passed to the transaction listener
     * @return transaction send result
     */
    public TransactionSendResult sendTransactionMessage(RocketMqMessage<?> mqMessage, Object arg) {
        if (!enableTransaction) {
            throw new IllegalStateException("Transaction messages are disabled. Set raf.rocketmq.producer.enableTransaction=true");
        }

        if (transactionProducer == null) {
            throw new IllegalStateException("Transaction producer is not initialized");
        }

        try {
            Message message = buildMessage(mqMessage);
            return transactionProducer.sendMessageInTransaction(message, arg);
        } catch (MQClientException e) {
            log.error("RocketMQ transaction send failed, topic:{}, tag:{}, key:{}", mqMessage.getTopic(), mqMessage.getTag(), mqMessage.getKey(), e);
            throw new InfrastructureException(RafResponseEnum.SERVER_ERROR, e);
        }
    }

    private Message buildMessage(RocketMqMessage<?> mqMessage) {
        String jsonBody = jsonService.toJson(mqMessage.getBody());
        Message message = new Message(
                mqMessage.getTopic(),
                StringUtils.defaultString(mqMessage.getTag(), "*"),
                StringUtils.defaultString(mqMessage.getKey(), ""),
                jsonBody.getBytes(StandardCharsets.UTF_8));

        if (mqMessage.getProperties() != null && !mqMessage.getProperties().isEmpty()) {
            mqMessage.getProperties().forEach(message::putUserProperty);
        }

        String traceId = ContextHolder.getTraceId();
        if (StringUtils.isNotBlank(traceId)) {
            message.putUserProperty("traceId", traceId);
        }

        return message;
    }

    @Override
    public void destroy() throws Exception {
        if (producer != null) {
            producer.shutdown();
            log.info("RocketMQ producer shutdown");
        }
        if (transactionProducer != null) {
            transactionProducer.shutdown();
            log.info("RocketMQ transaction producer shutdown");
        }
    }
}
