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
 * 订单超时延迟消息生产者（DLX + TTL 模式）。
 *
 * <p>消息投递到 dead exchange，TTL 到期后由 DLX 自动路由到 receive queue，
 * 触发 {@link io.github.jerryraf.examples.rabbit.consumer.OrderTimeoutConsumer}。
 *
 * <p>延迟拓扑（对应 application.yml bindings[order-timeout].delay）：
 * <pre>
 * sendDelay() → order.timeout.dead.exchange → order.timeout.dead.queue (TTL)
 *                                                    ↓ 到期
 *                                          order.timeout.exchange → order.timeout.queue → Consumer
 * </pre>
 *
 * @author Jerry
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderDelayProducer {

    /** 死信 Exchange（消息先投递到此，等待 TTL 到期） */
    private static final String DEAD_EXCHANGE    = "order.timeout.dead.exchange";
    /** 死信队列 routing key */
    private static final String DEAD_ROUTING_KEY = "order.timeout.dead";
    /** 默认超时时间：30 分钟 */
    private static final int DEFAULT_TIMEOUT_SECONDS = 30 * 60;

    private final RabbitMqMessageSender sender;
    private final JsonService jsonService;

    /**
     * 发送订单超时延迟消息，使用默认 30 分钟超时。
     */
    public void sendTimeout(OrderMessage order) {
        sendTimeout(order, DEFAULT_TIMEOUT_SECONDS);
    }

    /**
     * 发送订单超时延迟消息。
     *
     * @param order        订单信息
     * @param delaySeconds 延迟秒数；传 0 时使用队列级 TTL（需在 bindings 中配置 delay.ttl）
     */
    public void sendTimeout(OrderMessage order, int delaySeconds) {
        RabbitMqMessage msg = new RabbitMqMessage();
        msg.setMsgId(SnowFlakeBuilder.generateId());
        msg.setMessage(jsonService.toJson(order));

        log.info("Sending order timeout delay: msgId={}, orderId={}, delaySeconds={}",
                msg.getMsgId(), order.getOrderId(), delaySeconds);

        sender.sendDelay(msg, DEAD_EXCHANGE, DEAD_ROUTING_KEY, delaySeconds);
    }
}
