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
 * Sends order timeout delay messages via DLX+TTL pattern.
 *
 * @author Jerry
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderDelayProducer {

    private static final String BUSINESS_NAME = "order.timeout";
    private static final int DEFAULT_TIMEOUT_SECONDS = 30 * 60;

    private final RabbitMqMessageSender sender;
    private final JsonService jsonService;

    public void sendTimeout(OrderMessage order) {
        sendTimeout(order, DEFAULT_TIMEOUT_SECONDS);
    }

    public void sendTimeout(OrderMessage order, int delaySeconds) {
        RabbitMqMessage msg = new RabbitMqMessage();
        msg.setMsgId(SnowFlakeBuilder.generateId());
        msg.setMessage(jsonService.toJson(order));

        log.info("Sending order timeout delay: msgId={}, orderId={}, delaySeconds={}",
                msg.getMsgId(), order.getOrderId(), delaySeconds);
        sender.sendDelay(msg, BUSINESS_NAME, delaySeconds);
    }
}
