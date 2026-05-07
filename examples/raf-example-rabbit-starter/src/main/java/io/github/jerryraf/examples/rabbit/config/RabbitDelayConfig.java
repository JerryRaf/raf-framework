package io.github.jerryraf.examples.rabbit.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Delay queue configuration using DLX (Dead Letter Exchange) + TTL pattern.
 *
 * <p>Topology:
 * <pre>
 * Producer → order.delay.dead.exchange (Direct) → order.timeout.dead.queue (TTL=30min)
 *                                                        ↓ (on expiry, DLX routes to)
 *                                          order.delay.receive.exchange → order.timeout.queue → Consumer
 * </pre>
 *
 * @author Jerry
 */
@Configuration
public class RabbitDelayConfig {

    public static final String DELAY_DEAD_EXCHANGE    = "order.delay.dead.exchange";
    public static final String DELAY_RECEIVE_EXCHANGE = "order.delay.receive.exchange";
    public static final String ORDER_TIMEOUT_DEAD_QUEUE    = "order.timeout.dead.queue";
    public static final String ORDER_TIMEOUT_QUEUE         = "order.timeout.queue";
    public static final String ORDER_TIMEOUT_ROUTE         = "order.timeout";

    /** 30 minutes in milliseconds */
    private static final int TTL_30_MIN_MS = 30 * 60 * 1000;

    @Bean
    public DirectExchange delayDeadExchange() {
        return ExchangeBuilder.directExchange(DELAY_DEAD_EXCHANGE).durable(true).build();
    }

    @Bean
    public DirectExchange delayReceiveExchange() {
        return ExchangeBuilder.directExchange(DELAY_RECEIVE_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue orderTimeoutDeadQueue() {
        Map<String, Object> args = new HashMap<>(4);
        args.put("x-dead-letter-exchange", DELAY_RECEIVE_EXCHANGE);
        args.put("x-dead-letter-routing-key", ORDER_TIMEOUT_ROUTE);
        args.put("x-message-ttl", TTL_30_MIN_MS);
        return QueueBuilder.durable(ORDER_TIMEOUT_DEAD_QUEUE).withArguments(args).build();
    }

    @Bean
    public Queue orderTimeoutQueue() {
        return QueueBuilder.durable(ORDER_TIMEOUT_QUEUE).build();
    }

    @Bean
    public Binding orderTimeoutDeadBinding() {
        return BindingBuilder
                .bind(orderTimeoutDeadQueue())
                .to(delayDeadExchange())
                .with(ORDER_TIMEOUT_ROUTE + ".dead.route");
    }

    @Bean
    public Binding orderTimeoutBinding() {
        return BindingBuilder
                .bind(orderTimeoutQueue())
                .to(delayReceiveExchange())
                .with(ORDER_TIMEOUT_ROUTE);
    }
}
