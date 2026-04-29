package com.raf.framework.kafka;

import java.lang.annotation.*;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer annotation
 * Mark on consumer listener class to auto-register consumer
 *
 * @author RAF Framework
 * @date 2025/01/01
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface KafkaConsumer {

    /**
     * Topics to subscribe (supports SpEL expression)
     */
    String[] topics();

    /**
     * Consumer group id (supports SpEL expression)
     */
    String groupId() default "${raf.kafka.consumer.groupId:DEFAULT_CONSUMER_GROUP}";

    /**
     * Concurrency for listener container
     */
    int concurrency() default 3;

    /**
     * Auto startup
     */
    boolean autoStartup() default true;

    /**
     * Container type: SINGLE or BATCH
     */
    ContainerType containerType() default ContainerType.SINGLE;

    /**
     * Ack mode: RECORD, BATCH, TIME, COUNT, COUNT_TIME, MANUAL, MANUAL_IMMEDIATE
     */
    String ackMode() default "MANUAL_IMMEDIATE";

    /**
     * Poll timeout in milliseconds
     */
    long pollTimeout() default 5000;

    /**
     * Container type enumeration
     */
    enum ContainerType {
        /**
         * Process single record at a time
         */
        SINGLE,

        /**
         * Process batch records at a time
         */
        BATCH
    }
}
