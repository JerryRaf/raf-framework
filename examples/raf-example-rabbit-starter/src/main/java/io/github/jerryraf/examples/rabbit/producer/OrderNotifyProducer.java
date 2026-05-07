package io.github.jerryraf.examples.rabbit.producer;

import com.raf.framework.core.jackson.JsonService;
import com.raf.framework.core.snowflake.SnowFlakeBuilder;
import com.raf.framework.rabbit.RabbitMqMessage;
import com.raf.framework.rabbit.RabbitMqMessageSender;
import io.github.jerryraf.examples.rabbit.config.RabbitNormalConfig;
import io.github.jerryraf.examples.rabbit.dto.OrderMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Sends order notification messages to the normal queue.
 *
 * @author Jerry
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderNotifyProducer {

    private final RabbitMqMessageSender sender;
    private final JsonService jsonService;

    public void send(OrderMessage order) {
        RabbitMqMessage msg = new RabbitMqMessage();
        msg.setMsgId(SnowFlakeBuilder.generateId());
        msg.setMessage(jsonService.toJson(order));

        log.info("Sending order notify: msgId={}, orderId={}", msg.getMsgId(), order.getOrderId());
        sender.send(msg, RabbitNormalConfig.ORDER_NOTIFY_EXCHANGE, RabbitNormalConfig.ORDER_NOTIFY_ROUTE);
    }
}
