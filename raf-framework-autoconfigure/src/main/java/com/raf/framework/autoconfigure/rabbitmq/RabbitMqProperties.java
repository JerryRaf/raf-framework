package com.raf.framework.autoconfigure.rabbitmq;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

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

    private RabbitMqConsumer consumer;
    private RabbitMqProvider provider;
    private RabbitMqDelayProvider delay;

    @Data
    public static class RabbitMqProvider {
        private String exchange;
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
        private String group;
    }
}
