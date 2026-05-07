package io.github.jerryraf.examples.rabbit.consumer;

import com.raf.framework.core.jackson.JsonService;
import com.raf.framework.rabbit.AbstractRabbitConsumerListener;
import com.raf.framework.rabbit.RabbitMqConsumer;
import com.raf.framework.rabbit.RabbitMqMessage;
import io.github.jerryraf.examples.rabbit.config.RabbitNormalConfig;
import io.github.jerryraf.examples.rabbit.dto.OrderMessage;
import io.github.jerryraf.examples.rabbit.service.IdempotentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.stereotype.Component;

/**
 * Order notification consumer.
 *
 * <p>Demonstrates:
 * <ul>
 *   <li>Manual ACK via AbstractRabbitConsumerListener</li>
 *   <li>Idempotency: Redis setIfAbsent dedup by msgId</li>
 *   <li>Exception handling: BusinessException → nack no-requeue (→ DLQ); others → retry up to 3 times</li>
 * </ul>
 *
 * @author Jerry
 */
@Slf4j
@Component
@RabbitMqConsumer(
        exchange   = RabbitNormalConfig.ORDER_NOTIFY_EXCHANGE,
        routingKey = RabbitNormalConfig.ORDER_NOTIFY_ROUTE,
        queue      = RabbitNormalConfig.ORDER_NOTIFY_QUEUE,
        ackModel   = AcknowledgeMode.MANUAL
)
@RequiredArgsConstructor
public class OrderNotifyConsumer extends AbstractRabbitConsumerListener {

    private final IdempotentService idempotentService;
    private final JsonService jsonService;

    @Override
    public void onMessage(RabbitMqMessage rabbitMqMessage) {
        String msgId = rabbitMqMessage.getMsgId();
        log.info("Received order notify message: msgId={}", msgId);

        if (!idempotentService.tryMark(msgId)) {
            log.warn("Duplicate message, skipping: msgId={}", msgId);
            return;
        }

        OrderMessage order = jsonService.parse(rabbitMqMessage.getMessage(), OrderMessage.class);
        log.info("Processing order notify: orderId={}, status={}", order.getOrderId(), order.getStatus());

        processOrderNotification(order);

        log.info("Order notify processed successfully: msgId={}, orderId={}", msgId, order.getOrderId());
    }

    @Override
    public boolean retry(RabbitMqMessage message) {
        return !message.isOverTimes();
    }

    private void processOrderNotification(OrderMessage order) {
        if ("INVALID".equals(order.getStatus())) {
            throw new IllegalArgumentException("Invalid order status: " + order.getStatus());
        }
        log.info("Order notification sent to user: userId={}, orderId={}", order.getUserId(), order.getOrderId());
    }
}
