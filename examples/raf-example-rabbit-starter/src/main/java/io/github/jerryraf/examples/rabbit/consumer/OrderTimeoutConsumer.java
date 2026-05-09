package io.github.jerryraf.examples.rabbit.consumer;

import com.raf.framework.core.jackson.JsonService;
import com.raf.framework.rabbit.AbstractRabbitConsumerListener;
import com.raf.framework.rabbit.RabbitMqConsumer;
import com.raf.framework.rabbit.RabbitMqMessage;
import io.github.jerryraf.examples.rabbit.dto.OrderMessage;
import io.github.jerryraf.examples.rabbit.service.IdempotentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 订单超时消费者 — TTL 到期后由 DLX 路由到此队列触发。
 *
 * <p>监听 {@code order.timeout.queue}（receive queue），
 * 消息由 {@code order.timeout.dead.queue} 经 DLX 转发而来。
 *
 * @author Jerry
 */
@Slf4j
@Component
@RabbitMqConsumer(queue = "order.timeout.queue")
@RequiredArgsConstructor
public class OrderTimeoutConsumer extends AbstractRabbitConsumerListener {

    private final IdempotentService idempotentService;

    @Autowired
    private JsonService jsonService;

    @Override
    public void onMessage(RabbitMqMessage rabbitMqMessage) {
        String msgId = rabbitMqMessage.getMsgId();
        log.info("Received order timeout: msgId={}", msgId);

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
    public void onFailure(Message message, String error) {
        log.error("Order timeout processing permanently failed, manual intervention required. error={}", error);
    }

    private void cancelOrder(OrderMessage order) {
        log.info("Order {} cancelled due to payment timeout (userId={})",
                order.getOrderId(), order.getUserId());
    }
}
