package com.raf.framework.kafka;

import com.raf.framework.core.common.exception.InfrastructureException;
import com.raf.framework.core.common.result.RafResponseEnum;
import com.raf.framework.core.jackson.JsonService;
import com.raf.framework.core.trace.ContextHolder;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.beans.factory.DisposableBean;

/**
 * Kafka producer wrapper
 * Supports normal message and transactional message
 *
 * @author RAF Framework
 * @date 2025/01/01
 */
@Slf4j
@RequiredArgsConstructor
public class KafkaProducer implements DisposableBean {

    private final Producer<String, String> producer;
    private final Producer<String, String> transactionalProducer;
    private final JsonService jsonService;
    private final boolean enableTransaction;

    /**
     * Send message synchronously
     */
    public KafkaMessage.SendResult sendSync(KafkaMessage<?> kafkaMessage) {
        return sendSync(kafkaMessage, 30, TimeUnit.SECONDS);
    }

    /**
     * Send message synchronously with timeout
     */
    public KafkaMessage.SendResult sendSync(KafkaMessage<?> kafkaMessage, long timeout, TimeUnit unit) {
        try {
            ProducerRecord<String, String> record = buildRecord(kafkaMessage);
            RecordMetadata metadata = producer.send(record).get(timeout, unit);
            log.debug("Kafka message sent successfully. topic:{}, partition:{}, offset:{}",
                    metadata.topic(), metadata.partition(), metadata.offset());
            return KafkaMessage.SendResult.from(metadata);
        } catch (Exception e) {
            log.error("Kafka send message failed. topic:{}, key:{}", kafkaMessage.getTopic(), kafkaMessage.getKey(), e);
            throw new InfrastructureException(RafResponseEnum.SERVER_ERROR, e);
        }
    }

    /**
     * Send message synchronously (simplified version)
     */
    public KafkaMessage.SendResult sendSync(String topic, String key, Object body) {
        KafkaMessage<?> message = KafkaMessage.builder()
                .topic(topic)
                .key(key)
                .body(body)
                .messageType(KafkaMessage.MessageType.NORMAL)
                .build();
        return sendSync(message);
    }

    /**
     * Send message asynchronously
     */
    public CompletableFuture<KafkaMessage.SendResult> sendAsync(KafkaMessage<?> kafkaMessage) {
        CompletableFuture<KafkaMessage.SendResult> future = new CompletableFuture<>();
        try {
            ProducerRecord<String, String> record = buildRecord(kafkaMessage);
            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    log.error("Kafka async send message failed. topic:{}, key:{}", kafkaMessage.getTopic(), kafkaMessage.getKey(), exception);
                    future.completeExceptionally(exception);
                } else {
                    log.debug("Kafka async message sent successfully. topic:{}, partition:{}, offset:{}",
                            metadata.topic(), metadata.partition(), metadata.offset());
                    future.complete(KafkaMessage.SendResult.from(metadata));
                }
            });
        } catch (Exception e) {
            log.error("Kafka async send message error. topic:{}, key:{}", kafkaMessage.getTopic(), kafkaMessage.getKey(), e);
            future.completeExceptionally(e);
        }
        return future;
    }

    /**
     * Send message asynchronously (simplified version)
     */
    public CompletableFuture<KafkaMessage.SendResult> sendAsync(String topic, String key, Object body) {
        KafkaMessage<?> message = KafkaMessage.builder()
                .topic(topic)
                .key(key)
                .body(body)
                .messageType(KafkaMessage.MessageType.NORMAL)
                .build();
        return sendAsync(message);
    }

    /**
     * Execute in transaction
     */
    public void executeInTransaction(TransactionCallback callback) {
        if (!enableTransaction) {
            throw new IllegalStateException("Transaction is not enabled. Please set raf.kafka.producer.transactionalIdPrefix");
        }

        if (transactionalProducer == null) {
            throw new IllegalStateException("Transactional producer is not initialized");
        }

        transactionalProducer.beginTransaction();
        try {
            callback.doInTransaction(new TransactionalKafkaProducer(transactionalProducer, jsonService));
            transactionalProducer.commitTransaction();
            log.debug("Kafka transaction committed successfully");
        } catch (Exception e) {
            log.error("Kafka transaction failed, rolling back", e);
            transactionalProducer.abortTransaction();
            throw new InfrastructureException(RafResponseEnum.SERVER_ERROR, e);
        }
    }

    /**
     * Build Kafka ProducerRecord
     */
    private ProducerRecord<String, String> buildRecord(KafkaMessage<?> kafkaMessage) {
        String jsonBody = jsonService.toJson(kafkaMessage.getBody());

        ProducerRecord<String, String> record = new ProducerRecord<>(
                kafkaMessage.getTopic(),
                kafkaMessage.getPartition(),
                kafkaMessage.getTimestamp(),
                kafkaMessage.getKey(),
                jsonBody
        );

        if (kafkaMessage.getHeaders() != null && !kafkaMessage.getHeaders().isEmpty()) {
            kafkaMessage.getHeaders().forEach((key, value) ->
                    record.headers().add(new RecordHeader(key, value.getBytes(StandardCharsets.UTF_8)))
            );
        }

        String traceId = ContextHolder.getTraceId();
        if (StringUtils.isNotBlank(traceId)) {
            record.headers().add(new RecordHeader("traceId", traceId.getBytes(StandardCharsets.UTF_8)));
        }

        return record;
    }

    @Override
    public void destroy() throws Exception {
        if (producer != null) {
            producer.close();
            log.info("Kafka producer closed");
        }
        if (transactionalProducer != null) {
            transactionalProducer.close();
            log.info("Kafka transactional producer closed");
        }
    }

    /**
     * Transaction callback interface
     */
    @FunctionalInterface
    public interface TransactionCallback {
        void doInTransaction(TransactionalKafkaProducer producer) throws Exception;
    }

    /**
     * Transactional Kafka producer wrapper
     */
    @RequiredArgsConstructor
    public static class TransactionalKafkaProducer {
        private final Producer<String, String> producer;
        private final JsonService jsonService;

        public void send(KafkaMessage<?> kafkaMessage) {
            try {
                String jsonBody = jsonService.toJson(kafkaMessage.getBody());
                ProducerRecord<String, String> record = new ProducerRecord<>(
                        kafkaMessage.getTopic(),
                        kafkaMessage.getPartition(),
                        kafkaMessage.getTimestamp(),
                        kafkaMessage.getKey(),
                        jsonBody
                );

                if (kafkaMessage.getHeaders() != null && !kafkaMessage.getHeaders().isEmpty()) {
                    kafkaMessage.getHeaders().forEach((key, value) ->
                            record.headers().add(new RecordHeader(key, value.getBytes(StandardCharsets.UTF_8)))
                    );
                }

                String traceId = ContextHolder.getTraceId();
                if (StringUtils.isNotBlank(traceId)) {
                    record.headers().add(new RecordHeader("traceId", traceId.getBytes(StandardCharsets.UTF_8)));
                }

                producer.send(record);
            } catch (Exception e) {
                log.error("Transactional send failed. topic:{}, key:{}", kafkaMessage.getTopic(), kafkaMessage.getKey(), e);
                throw new InfrastructureException(RafResponseEnum.SERVER_ERROR, e);
            }
        }

        public void send(String topic, String key, Object body) {
            KafkaMessage<?> message = KafkaMessage.builder()
                    .topic(topic)
                    .key(key)
                    .body(body)
                    .messageType(KafkaMessage.MessageType.TRANSACTIONAL)
                    .build();
            send(message);
        }
    }
}
