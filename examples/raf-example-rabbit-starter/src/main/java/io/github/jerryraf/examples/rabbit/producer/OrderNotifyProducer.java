package io.github.jerryraf.examples.rabbit.producer;

import com.raf.framework.core.jackson.JsonService;
import com.raf.framework.core.snowflake.SnowFlakeBuilder;
import com.raf.framework.rabbit.RabbitMqMessage;
import com.raf.framework.rabbit.RabbitMqMessageSender;
import io.github.jerryraf.examples.rabbit.dto.OrderMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 订单通知消息生产者。
 *
 * @author Jerry
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderNotifyProducer {

    private static final String EXCHANGE    = "order.notify.exchange";
    private static final String ROUTING_KEY = "order.notify";

    private final RabbitMqMessageSender sender;
    private final JsonService jsonService;

    public void send(OrderMessage order) {
        RabbitMqMessage msg = new RabbitMqMessage();
        msg.setMsgId(SnowFlakeBuilder.generateId());
        msg.setMessage(jsonService.toJson(order));

        log.info("Sending order notify: msgId={}, orderId={}", msg.getMsgId(), order.getOrderId());
        sender.send(msg, EXCHANGE, ROUTING_KEY);
    }
}
