package com.raf.framework.rabbit;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.raf.framework.core.jackson.JsonService;
import com.raf.framework.core.spring.bean.SpringContext;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;

/**
 * @author Jerry
 * @date 2019/01/01
 */
@Slf4j
public abstract class AbstractRabbitConsumerListener implements ChannelAwareMessageListener {

    private static final String RETRY_COUNT_HEADER = "x-retry-count";
    private static final String TYPE_FIELD = "@type";

    /**
     * 消费消息
     */
    @Override
    public void onMessage(Message message, Channel channel) throws Exception {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        RabbitMqMessage rabbitMqMessage = null;

        try {
            // Security validation: check message type whitelist
            if (isStrictTypeValidationEnabled() && !validateMessageType(body)) {
                log.error("Message type validation failed, rejecting message without retry");
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                return;
            }

            // Check retry count
            Integer retryCount = getRetryCount(message);
            if (retryCount != null && retryCount >= getMaxRetryCount()) {
                log.error("Message exceeded max retry count: {}, moving to DLQ", retryCount);
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                sendToDLQ(message);
                return;
            }

            JsonService json = SpringContext.getBean(JsonService.class);
            rabbitMqMessage = json.parse(body, RabbitMqMessage.class);
            this.onMessage(rabbitMqMessage);

            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception ex) {
            log.error("Message consumption failed: {}", ex.getMessage(), ex);

            if (null != rabbitMqMessage && retry(rabbitMqMessage)) {
                Integer currentRetryCount = getRetryCount(message);
                int newRetryCount = (currentRetryCount == null ? 0 : currentRetryCount) + 1;
                log.warn("Retrying message, retry count: {}", newRetryCount);
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
            } else {
                giveUp(message, channel, message.getMessageProperties().getDeliveryTag(), ex.getMessage());
            }
        }
    }

    /**
     * Validate message type against whitelist
     *
     * @param messageBody message body
     * @return true if valid, false otherwise
     */
    private boolean validateMessageType(String messageBody) {
        try {
            Set<String> allowedClasses = getAllowedMessageClasses();
            if (allowedClasses == null || allowedClasses.isEmpty()) {
                // If no whitelist configured, allow all (backward compatible)
                return true;
            }

            JSONObject json = JSON.parseObject(messageBody);
            String className = json.getString(TYPE_FIELD);

            if (className != null && !allowedClasses.contains(className)) {
                log.error("Illegal message type: {}, allowed types: {}", className, allowedClasses);
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("Failed to validate message type", e);
            return false;
        }
    }

    /**
     * Get retry count from message headers
     *
     * @param message message
     * @return retry count
     */
    private Integer getRetryCount(Message message) {
        Map<String, Object> headers = message.getMessageProperties().getHeaders();
        if (headers != null) {
            Object retryCount = headers.get(RETRY_COUNT_HEADER);
            if (retryCount instanceof Integer) {
                return (Integer) retryCount;
            }
        }
        return null;
    }

    /**
     * @param message message
     * @param channel channel
     * @param deliveryTag delivery tag
     * @param error error message
     */
    private void giveUp(Message message, Channel channel, long deliveryTag, String error) {
        try {
            onFailure(message, error);
        } catch (Exception ex) {
            log.error("Message consumption rollback exception, body: {}", new String(message.getBody(), StandardCharsets.UTF_8));
        } finally {
            try {
                channel.basicReject(deliveryTag, false);
            } catch (IOException ex) {
                log.error("Failed to reject message: {}", ex.getMessage());
            }
        }
    }

    /**
     * Consume a message.
     *
     * @param message message to consume
     * @throws Exception on consumption failure
     */
    public abstract void onMessage(RabbitMqMessage message) throws Exception;

    /**
     * Handle a failed message.
     *
     * @param message original AMQP message
     * @param error   error description
     * @throws Exception on failure handling error
     */
    public abstract void onFailure(Message message, String error) throws Exception;

    /**
     * Whether to retry on exception. Override to enable retry logic.
     *
     * @param message failed message
     * @return true to retry, false to reject
     */
    public boolean retry(RabbitMqMessage message) {
        return false;
    }

    /**
     * Send message to Dead Letter Queue (DLQ)
     * Subclasses can override this method to implement custom DLQ logic
     *
     * @param message message
     */
    protected void sendToDLQ(Message message) {
        log.warn("Sending message to DLQ: {}", new String(message.getBody(), StandardCharsets.UTF_8));
        // Default implementation: just log
        // Subclasses should implement actual DLQ sending logic
    }

    /**
     * Get allowed message classes for deserialization
     * Subclasses should override this method to provide whitelist
     *
     * @return set of allowed class names
     */
    protected Set<String> getAllowedMessageClasses() {
        try {
            RabbitMqProperties properties = SpringContext.getBean(RabbitMqProperties.class);
            if (properties != null && properties.getSecurity() != null) {
                return properties.getSecurity().getAllowedMessageClasses();
            }
        } catch (Exception e) {
            log.warn("Failed to get RabbitMqProperties, using default empty whitelist");
        }
        return null;
    }

    /**
     * Get max retry count
     *
     * @return max retry count
     */
    protected int getMaxRetryCount() {
        try {
            RabbitMqProperties properties = SpringContext.getBean(RabbitMqProperties.class);
            if (properties != null && properties.getSecurity() != null) {
                return properties.getSecurity().getMaxRetryCount();
            }
        } catch (Exception e) {
            log.warn("Failed to get RabbitMqProperties, using default max retry count: 3");
        }
        return 3;
    }

    /**
     * Check if strict type validation is enabled
     *
     * @return true if enabled, false otherwise
     */
    protected boolean isStrictTypeValidationEnabled() {
        try {
            RabbitMqProperties properties = SpringContext.getBean(RabbitMqProperties.class);
            if (properties != null && properties.getSecurity() != null) {
                return properties.getSecurity().isStrictTypeValidation();
            }
        } catch (Exception e) {
            log.warn("Failed to get RabbitMqProperties, using default strict validation: true");
        }
        return true;
    }
}
