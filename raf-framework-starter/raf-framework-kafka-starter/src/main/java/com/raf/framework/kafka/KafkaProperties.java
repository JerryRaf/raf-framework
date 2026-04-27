package com.raf.framework.kafka;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Kafka configuration properties
 *
 * @author RAF Framework
 * @date 2025/01/01
 */
@Data
@ConfigurationProperties("raf.kafka")
public class KafkaProperties {

    /**
     * Kafka broker addresses, separated by comma
     */
    private String bootstrapServers;

    /**
     * Producer configuration
     */
    private Producer producer;

    /**
     * Consumer configuration
     */
    private Consumer consumer;

    /**
     * Whether to enable Kafka
     */
    private boolean enabled = false;

    /**
     * Security configuration
     */
    private Security security;

    @Data
    public static class Producer {
        /**
         * Retry times when send failed
         */
        private int retries = 3;

        /**
         * Request timeout in milliseconds
         */
        private int requestTimeout = 30000;

        /**
         * Batch size in bytes
         */
        private int batchSize = 16384;

        /**
         * Linger time in milliseconds
         */
        private int lingerMs = 0;

        /**
         * Buffer memory in bytes
         */
        private long bufferMemory = 33554432L;

        /**
         * Compression type: none, gzip, snappy, lz4, zstd
         */
        private String compressionType = "none";

        /**
         * Max in-flight requests per connection
         */
        private int maxInFlightRequestsPerConnection = 5;

        /**
         * Acks mode: 0, 1, all
         */
        private String acks = "all";

        /**
         * Enable idempotence
         */
        private boolean enableIdempotence = true;

        /**
         * Transaction id prefix (required for transactional producer)
         */
        private String transactionalIdPrefix;

        /**
         * Transaction timeout in milliseconds
         */
        private int transactionTimeout = 60000;

        /**
         * Key serializer class
         */
        private String keySerializer = "org.apache.kafka.common.serialization.StringSerializer";

        /**
         * Value serializer class
         */
        private String valueSerializer = "org.apache.kafka.common.serialization.StringSerializer";
    }

    @Data
    public static class Consumer {
        /**
         * Consumer group id
         */
        private String groupId = "DEFAULT_CONSUMER_GROUP";

        /**
         * Enable auto commit
         */
        private boolean enableAutoCommit = false;

        /**
         * Auto commit interval in milliseconds
         */
        private int autoCommitIntervalMs = 5000;

        /**
         * Session timeout in milliseconds
         */
        private int sessionTimeoutMs = 30000;

        /**
         * Heartbeat interval in milliseconds
         */
        private int heartbeatIntervalMs = 3000;

        /**
         * Max poll records
         */
        private int maxPollRecords = 500;

        /**
         * Max poll interval in milliseconds
         */
        private int maxPollIntervalMs = 300000;

        /**
         * Auto offset reset: earliest, latest, none
         */
        private String autoOffsetReset = "latest";

        /**
         * Key deserializer class
         */
        private String keyDeserializer = "org.apache.kafka.common.serialization.StringDeserializer";

        /**
         * Value deserializer class
         */
        private String valueDeserializer = "org.apache.kafka.common.serialization.StringDeserializer";

        /**
         * Isolation level: read_uncommitted, read_committed
         */
        private String isolationLevel = "read_committed";

        /**
         * Concurrency for listener container
         */
        private int concurrency = 3;

        /**
         * Poll timeout in milliseconds
         */
        private int pollTimeout = 5000;
    }

    @Data
    public static class MessageValidation {
        /**
         * Maximum message size in bytes, default 1MB
         */
        private int maxMessageSize = 1024 * 1024;

        /**
         * Enable JSON format validation
         */
        private boolean validateJsonFormat = true;
    }

    /**
     * Message validation configuration
     */
    private MessageValidation messageValidation = new MessageValidation();

    @Data
    public static class Security {
        /**
         * Security protocol: PLAINTEXT, SSL, SASL_PLAINTEXT, SASL_SSL
         */
        private String protocol = "PLAINTEXT";

        /**
         * SASL mechanism: PLAIN, SCRAM-SHA-256, SCRAM-SHA-512
         */
        private String saslMechanism;

        /**
         * SASL JAAS config
         */
        private String saslJaasConfig;

        /**
         * SSL truststore location
         */
        private String sslTruststoreLocation;

        /**
         * SSL truststore password
         */
        private String sslTruststorePassword;

        /**
         * SSL keystore location
         */
        private String sslKeystoreLocation;

        /**
         * SSL keystore password
         */
        private String sslKeystorePassword;

        /**
         * SSL key password
         */
        private String sslKeyPassword;
    }
}
