package com.raf.framework.kafka;

import java.util.Map;
import lombok.Builder;
import lombok.Data;
import org.apache.kafka.clients.producer.RecordMetadata;

/**
 * Kafka message wrapper
 *
 * @author RAF Framework
 * @date 2025/01/01
 */
@Data
@Builder
public class KafkaMessage<T> {

    /**
     * Topic name
     */
    private String topic;

    /**
     * Message key (used for partition selection)
     */
    private String key;

    /**
     * Message body
     */
    private T body;

    /**
     * Target partition (optional, use key hash if not specified)
     */
    private Integer partition;

    /**
     * Message timestamp (optional, use current time if not specified)
     */
    private Long timestamp;

    /**
     * Custom headers
     */
    private Map<String, String> headers;

    /**
     * Message type
     */
    @Builder.Default
    private MessageType messageType = MessageType.NORMAL;

    /**
     * Kafka message type
     */
    public enum MessageType {
        /**
         * Normal message
         */
        NORMAL,

        /**
         * Transactional message
         */
        TRANSACTIONAL
    }

    /**
     * Send result wrapper
     */
    @Data
    @Builder
    public static class SendResult {
        /**
         * Topic name
         */
        private String topic;

        /**
         * Partition
         */
        private int partition;

        /**
         * Offset
         */
        private long offset;

        /**
         * Timestamp
         */
        private long timestamp;

        /**
         * Serialized key size
         */
        private int serializedKeySize;

        /**
         * Serialized value size
         */
        private int serializedValueSize;

        /**
         * Convert from Kafka RecordMetadata
         */
        public static SendResult from(RecordMetadata metadata) {
            return SendResult.builder()
                    .topic(metadata.topic())
                    .partition(metadata.partition())
                    .offset(metadata.offset())
                    .timestamp(metadata.timestamp())
                    .serializedKeySize(metadata.serializedKeySize())
                    .serializedValueSize(metadata.serializedValueSize())
                    .build();
        }
    }
}
