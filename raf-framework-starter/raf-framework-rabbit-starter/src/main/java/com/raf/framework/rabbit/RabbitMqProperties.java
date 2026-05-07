package com.raf.framework.rabbit;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Jerry
 * @date 2019/01/01
 */
@Data
@ConfigurationProperties("raf.rabbit")
public class RabbitMqProperties {
    private String username;
    private String password;
    private String addresses;
    private String virtualHost = "/";
    private Ssl ssl = new Ssl();

    private RabbitMqConsumer consumer = new RabbitMqConsumer();
    private RabbitMqProvider provider = new RabbitMqProvider();
    private RabbitMqDelayProvider delay;
    private Security security = new Security();

    @Data
    public static class Ssl {
        private boolean enabled = false;
        private String keyStore;
        private String keyStorePassword;
        private String trustStore;
        private String trustStorePassword;
        private String algorithm;
        private String verifyHostname;
    }

    @Data
    public static class RabbitMqProvider {
        private boolean ack = true;
    }

    @Data
    public static class RabbitMqDelayProvider {
        private String deadExchange;
        private String receiveExchange;
        private List<String> queuePrefix;
    }

    @Data
    public static class RabbitMqConsumer {
        private String group = "DEFAULT_RABBIT_GROUP";
        private int concurrentConsumers = 3;
        private int maxConcurrentConsumers = 10;
    }

    @Data
    public static class ListenerContainer {
        private int concurrentConsumers = 1;
        private int maxConcurrentConsumers = 3;
    }

    @Data
    public static class Security {
        /**
         * Allowed message class names for deserialization (whitelist)
         * Example: com.company.dto.OrderMessage, com.company.dto.PaymentMessage
         */
        private Set<String> allowedMessageClasses = new HashSet<>();

        /**
         * Maximum retry count for failed messages
         */
        private int maxRetryCount = 3;

        /**
         * Enable strict type validation
         */
        private boolean strictTypeValidation = true;
    }
}
