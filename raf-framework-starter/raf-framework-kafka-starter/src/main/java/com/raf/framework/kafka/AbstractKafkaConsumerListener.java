package com.raf.framework.kafka;

import com.raf.framework.core.jackson.JsonService;
import com.raf.framework.core.spring.bean.SpringContext;
import com.raf.framework.core.trace.ContextHolder;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;

/**
 * Kafka consumer listener abstract class
 * Provides common logic for message consumption: deserialization, exception handling, distributed tracing
 *
 * @author RAF Framework
 * @date 2025/01/01
 */
@Slf4j
public abstract class AbstractKafkaConsumerListener<T> {

    private final Class<T> messageType;
    private final JsonService jsonService;

    @SuppressWarnings("unchecked")
    public AbstractKafkaConsumerListener() {
        Type superClass = getClass().getGenericSuperclass();
        if (superClass instanceof ParameterizedType) {
            Type[] typeArguments = ((ParameterizedType) superClass).getActualTypeArguments();
            this.messageType = (Class<T>) typeArguments[0];
        } else {
            this.messageType = null;
        }
        this.jsonService = SpringContext.getBean(JsonService.class);
    }

    /**
     * Process single message
     */
    public void onMessage(ConsumerRecord<String, String> record) {
        try {
            Header traceIdHeader = record.headers().lastHeader("traceId");
            if (traceIdHeader != null) {
                String traceId = new String(traceIdHeader.value(), StandardCharsets.UTF_8);
                if (StringUtils.isNotBlank(traceId)) {
                    ContextHolder.setTraceId(traceId);
                }
            }

            // Message size validation
            String value = record.value();
            if (!validateMessage(value, record)) {
                return;
            }

            T message = deserializeMessage(value);

            boolean success = handleMessage(message, record);

            if (!success) {
                log.warn("Kafka message consumption failed. topic:{}, partition:{}, offset:{}, key:{}",
                        record.topic(), record.partition(), record.offset(), record.key());
            } else {
                log.debug("Kafka message consumed successfully. topic:{}, partition:{}, offset:{}, key:{}",
                        record.topic(), record.partition(), record.offset(), record.key());
            }

        } catch (Exception e) {
            log.error("Kafka message consumption exception. topic:{}, partition:{}, offset:{}, key:{}",
                    record.topic(), record.partition(), record.offset(), record.key(), e);
            throw e;
        } finally {
            ContextHolder.clearTraceId();
        }
    }

    /**
     * Validate message size and format
     *
     * @param value  message value
     * @param record original record
     * @return true if valid
     */
    private boolean validateMessage(String value, ConsumerRecord<String, String> record) {
        if (StringUtils.isEmpty(value)) {
            log.warn("Kafka message is empty. topic:{}, partition:{}, offset:{}",
                    record.topic(), record.partition(), record.offset());
            return false;
        }

        KafkaProperties properties = getKafkaProperties();
        if (properties != null && properties.getMessageValidation() != null) {
            KafkaProperties.MessageValidation validation = properties.getMessageValidation();

            // Size check
            if (value.length() > validation.getMaxMessageSize()) {
                log.error("Kafka message size exceeds limit:  bytes, max: {} bytes. topic:{}, partition:{}, offset:{}",
                        value.length(), validation.getMaxMessageSize(),
                        record.topic(), record.partition(), record.offset());
                return false;
            }

            // JSON format check
            if (validation.isValidateJsonFormat() && messageType != null && messageType != String.class) {
                if (!value.startsWith("{") && !value.startsWith("[")) {
                    log.error("Kafka message is not valid JSON. topic:{}, partition:{}, offset:{}",
                            record.topic(), record.partition(), record.offset());
                    return false;
                }
            }
        }

        return true;
    }

    private KafkaProperties getKafkaProperties() {
        try {
            return SpringContext.getBean(KafkaProperties.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Deserialize message
     */
    private T deserializeMessage(String body) {
        if (messageType == null || messageType == String.class) {
            return (T) body;
        }
        return jsonService.parse(body, messageType);
    }

    /**
     * Business logic implementation
     *
     * @param message deserialized message body
     * @param record  original Kafka record (contains metadata)
     * @return true for success, false for failure (will throw exception for retry if auto-commit is disabled)
     */
    protected abstract boolean handleMessage(T message, ConsumerRecord<String, String> record);
}
