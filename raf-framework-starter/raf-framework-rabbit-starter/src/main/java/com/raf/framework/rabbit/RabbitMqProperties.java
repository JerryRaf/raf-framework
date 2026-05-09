package com.raf.framework.rabbit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RabbitMQ 配置属性。
 *
 * <p>拓扑声明（Exchange/Queue/Binding）通过 {@code raf.rabbit.bindings} 配置，
 * 由生产者服务在启动时自动声明。消费者服务只需配置连接信息和 consumer 并发参数。
 *
 * @author Jerry
 */
@Data
@ConfigurationProperties("raf.rabbit")
public class RabbitMqProperties {

    private boolean enabled = false;
    private String username;
    private String password;
    private String addresses;
    private String virtualHost = "/";
    private Ssl ssl = new Ssl();
    private Provider provider = new Provider();
    private Consumer consumer = new Consumer();

    /**
     * 拓扑声明列表。生产者服务配置此项，启动时框架自动声明 Exchange/Queue/Binding。
     * 消费者服务不需要配置此项。
     */
    private List<BindingDefinition> bindings = new ArrayList<>();

    @Data
    public static class Ssl {
        private boolean enabled = false;
        private String keyStore;
        private String keyStorePassword;
        private String trustStore;
        private String trustStorePassword;
        private String algorithm;
    }

    /** 生产者配置 */
    @Data
    public static class Provider {
        /**
         * 是否开启 publisher confirm + mandatory return。
         * 默认 false。开启后需在业务侧实现 AbstractRabbitSenderConfirm。
         */
        private boolean ack = false;
    }

    /** 消费者配置 */
    @Data
    public static class Consumer {
        private String group = "DEFAULT_RABBIT_GROUP";
        private int concurrentConsumers = 3;
        private int maxConcurrentConsumers = 10;
    }

    /** 单条拓扑声明，描述一个完整的 Exchange → Queue 绑定关系 */
    @Data
    public static class BindingDefinition {
        /** 逻辑名称，仅用于日志和 Bean 命名，同一服务内不能重复 */
        private String name;

        /** Exchange 名称 */
        private String exchange;

        /**
         * Exchange 类型。支持：direct（默认）/ topic / fanout / headers
         */
        private String exchangeType = "direct";

        /** 队列名称 */
        private String queue;

        /** Routing Key。fanout 类型时忽略此字段 */
        private String routingKey = "";

        /** 是否持久化，默认 true */
        private boolean durable = true;

        /**
         * 延迟队列配置。不为 null 时，框架额外声明 DLX + 死信队列拓扑。
         * 消息先投递到 dead exchange → dead queue（等待 TTL）→ DLX 转发到 exchange → queue。
         */
        private DelayConfig delay;

        /**
         * 队列自定义参数，直接传给 RabbitMQ。
         * 示例：x-max-priority: 10（优先级队列），x-queue-mode: lazy（惰性队列）
         */
        private Map<String, Object> arguments = new HashMap<>();
    }

    /** 延迟队列（DLX + TTL）配置 */
    @Data
    public static class DelayConfig {
        /** 死信 Exchange 名称（消息 TTL 到期后路由到此） */
        private String deadExchange;

        /** 死信 Exchange 类型，默认 direct */
        private String deadExchangeType = "direct";

        /** 死信队列名称（设置了 TTL 的等待队列） */
        private String deadQueue;

        /** 死信队列 routing key */
        private String deadRoutingKey = "";

        /**
         * 队列级 TTL（毫秒）。
         * 大于 0 时在死信队列上设置 x-message-ttl，消息等待此时长后自动转发到 receive queue。
         * 等于 0 时不设置队列级 TTL，改用消息级 TTL（在 sendDelay 时传入 second 参数）。
         */
        private long ttl = 0;
    }
}
