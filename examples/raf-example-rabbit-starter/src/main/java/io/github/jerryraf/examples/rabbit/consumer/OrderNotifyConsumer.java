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
 * 订单通知消费者。
 *
 * <p>演示要点：
 * <ul>
 *   <li>框架层统一手动 ACK，业务只需实现 {@link #onMessage}</li>
 *   <li>幂等性：Redis setIfAbsent 按 msgId 去重</li>
 *   <li>重试：默认 3 次（{@link #maxRetryTimes}），超过后调用 {@link #onFailure} 持久化</li>
 * </ul>
 *
 * @author Jerry
 */
@Slf4j
@Component
@RabbitMqConsumer(queue = "order.notify.queue")
@RequiredArgsConstructor
public class OrderNotifyConsumer extends AbstractRabbitConsumerListener {

    private final IdempotentService idempotentService;

    @Autowired
    private JsonService jsonService;

    @Override
    public void onMessage(RabbitMqMessage rabbitMqMessage) {
        String msgId = rabbitMqMessage.getMsgId();
        log.info("Received order notify: msgId={}", msgId);

        if (!idempotentService.tryMark(msgId)) {
            log.warn("Duplicate message, skipping: msgId={}", msgId);
            return;
        }

        OrderMessage order = jsonService.parse(rabbitMqMessage.getMessage(), OrderMessage.class);
        log.info("Processing order notify: orderId={}, status={}", order.getOrderId(), order.getStatus());

        processOrderNotification(order);

        log.info("Order notify processed: msgId={}, orderId={}", msgId, order.getOrderId());
    }

    @Override
    public void onFailure(Message message, String error) {
        // 超过最大重试次数后持久化到 DB，人工补偿
        log.error("Order notify permanently failed, manual intervention required. error={}", error);
    }

    /**
     * 最大重试 3 次（框架默认值，此处显式声明便于阅读）。
     * 高可靠场景可调大，或在注解上配置更多消费者加快消费速度。
     */
    @Override
    public int maxRetryTimes() {
        return 3;
    }

    private void processOrderNotification(OrderMessage order) {
        if ("INVALID".equals(order.getStatus())) {
            throw new IllegalArgumentException("Invalid order status: " + order.getStatus());
        }
        log.info("Notification sent to user: userId={}, orderId={}", order.getUserId(), order.getOrderId());
    }
}
