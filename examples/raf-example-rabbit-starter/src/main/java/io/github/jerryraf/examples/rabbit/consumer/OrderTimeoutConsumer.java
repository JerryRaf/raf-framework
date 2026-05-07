package io.github.jerryraf.examples.rabbit.consumer;

import com.raf.framework.core.jackson.JsonService;
import com.raf.framework.rabbit.AbstractRabbitConsumerListener;
import com.raf.framework.rabbit.RabbitMqDelayConsumer;
import com.raf.framework.rabbit.RabbitMqMessage;
import io.github.jerryraf.examples.rabbit.config.RabbitDelayConfig;
import io.github.jerryraf.examples.rabbit.dto.OrderMessage;
import io.github.jerryraf.examples.rabbit.service.IdempotentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.stereotype.Component;

/**
 * Order timeout consumer — triggered after TTL expires in the dead-letter queue.
 *
 * @author Jerry
 */
@Slf4j
@Component
@RabbitMqDelayConsumer(
        businessName = "order.timeout",
        exchange     = RabbitDelayConfig.DELAY_RECEIVE_EXCHANGE,
        ackModel     = AcknowledgeMode.MANUAL
)
@RequiredArgsConstructor
public class OrderTimeoutConsumer extends AbstractRabbitConsumerListener {

    private final IdempotentService idempotentService;
    private final JsonService jsonService;

    @Override
    public void onMessage(RabbitMqMessage rabbitMqMessage) {
        String msgId = rabbitMqMessage.getMsgId();
        log.info("Received order timeout message: msgId={}", msgId);

        if (!idempotentService.tryMark(msgId)) {
            log.warn("Duplicate timeout message, skipping: msgId={}", msgId);
            return;
        }

        OrderMessage order = jsonService.parse(rabbitMqMessage.getMessage(), OrderMessage.class);
        log.info("Processing order timeout cancellation: orderId={}", order.getOrderId());

        cancelOrder(order);

        log.info("Order timeout cancelled: msgId={}, orderId={}", msgId, order.getOrderId());
    }

    @Override
    public boolean retry(RabbitMqMessage message) {
        return !message.isOverTimes();
    }

    private void cancelOrder(OrderMessage order) {
        log.info("Order {} cancelled due to payment timeout (userId={})",
                order.getOrderId(), order.getUserId());
    }
}
