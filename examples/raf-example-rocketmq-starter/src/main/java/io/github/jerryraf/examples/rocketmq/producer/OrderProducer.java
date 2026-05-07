package io.github.jerryraf.examples.rocketmq.producer;

import com.raf.framework.rocketmq.RocketMqMessage;
import com.raf.framework.rocketmq.RocketMqProducer;
import io.github.jerryraf.examples.rocketmq.dto.OrderMsgReq;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.springframework.stereotype.Component;

/**
 * 订单消息生产者
 * 演示：普通消息、顺序消息、延时消息、事务消息
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderProducer {

    private static final String TOPIC_ORDER_CREATED = "order-created-topic";
    private static final String TOPIC_ORDER_FIFO    = "order-fifo-topic";
    private static final String TOPIC_ORDER_DELAY   = "order-delay-topic";
    private static final String TOPIC_ORDER_TX      = "order-tx-topic";

    private final RocketMqProducer rocketMqProducer;

    /**
     * 发送普通消息（同步）
     */
    public SendResult sendNormal(OrderMsgReq req) {
        SendResult result = rocketMqProducer.sendSync(TOPIC_ORDER_CREATED, "created", req);
        log.info("普通消息发送成功，orderId={}, msgId={}", req.getOrderId(), result.getMsgId());
        return result;
    }

    /**
     * 发送顺序消息（同一 userId 的消息保证顺序）
     */
    public SendResult sendFifo(OrderMsgReq req) {
        RocketMqMessage<OrderMsgReq> msg = RocketMqMessage.<OrderMsgReq>builder()
            .topic(TOPIC_ORDER_FIFO)
            .tag("fifo")
            .key(req.getOrderId())
            .body(req)
            .shardingKey(String.valueOf(req.getUserId()))  // 同一 userId 路由到同一队列
            .messageType(RocketMqMessage.MessageType.FIFO)
            .build();
        SendResult result = rocketMqProducer.sendSync(msg);
        log.info("顺序消息发送成功，orderId={}, userId={}", req.getOrderId(), req.getUserId());
        return result;
    }

    /**
     * 发送延时消息（delayLevel 3 = 10s）
     */
    public SendResult sendDelay(OrderMsgReq req) {
        RocketMqMessage<OrderMsgReq> msg = RocketMqMessage.<OrderMsgReq>builder()
            .topic(TOPIC_ORDER_DELAY)
            .tag("timeout-check")
            .key(req.getOrderId())
            .body(req)
            .delayLevel(req.getDelayLevel())
            .messageType(RocketMqMessage.MessageType.DELAY)
            .build();
        SendResult result = rocketMqProducer.sendSync(msg);
        log.info("延时消息发送成功，orderId={}, delayLevel={}", req.getOrderId(), req.getDelayLevel());
        return result;
    }

    /**
     * 发送事务消息
     */
    public SendResult sendTransaction(OrderMsgReq req) {
        RocketMqMessage<OrderMsgReq> msg = RocketMqMessage.<OrderMsgReq>builder()
            .topic(TOPIC_ORDER_TX)
            .tag("tx")
            .key(req.getOrderId())
            .body(req)
            .messageType(RocketMqMessage.MessageType.TRANSACTION)
            .build();
        SendResult result = rocketMqProducer.sendTransactionMessage(msg, req);
        log.info("事务消息发送成功，orderId={}", req.getOrderId());
        return result;
    }
}
