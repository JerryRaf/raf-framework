package io.github.jerryraf.examples.rabbit.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Normal queue configuration: Direct Exchange + queue + binding.
 *
 * <p>Topology:
 * <pre>
 * Producer → order.notify.exchange (Direct) → order.notify.queue → Consumer
 * </pre>
 *
 * @author Jerry
 */
@Configuration
public class RabbitNormalConfig {

    public static final String ORDER_NOTIFY_EXCHANGE = "order.notify.exchange";
    public static final String ORDER_NOTIFY_QUEUE    = "order.notify.queue";
    public static final String ORDER_NOTIFY_ROUTE    = "order.notify.route";

    @Bean
    public DirectExchange orderNotifyExchange() {
        return ExchangeBuilder.directExchange(ORDER_NOTIFY_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue orderNotifyQueue() {
        return QueueBuilder.durable(ORDER_NOTIFY_QUEUE).build();
    }

    @Bean
    public Binding orderNotifyBinding() {
        return BindingBuilder
                .bind(orderNotifyQueue())
                .to(orderNotifyExchange())
                .with(ORDER_NOTIFY_ROUTE);
    }
}
