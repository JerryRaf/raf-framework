package io.github.jerryraf.examples.rocketmq.consumer;

import com.raf.framework.rocketmq.AbstractRocketMqConsumerListener;
import com.raf.framework.rocketmq.RocketMqConsumer;
import com.raf.framework.rocketmq.RocketMqMessage;
import io.github.jerryraf.examples.rocketmq.dto.OrderMsgReq;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.List;

/**
 * 订单顺序消费者（FIFO 消息）
 * 同一 userId 的消息严格按顺序消费
 */
@RocketMqConsumer(
    topic = "order-fifo-topic",
    tag = "fifo",
    consumerGroup = "order-fifo-consumer-group",
    messageType = RocketMqMessage.MessageType.FIFO
)
@Slf4j
public class OrderFifoConsumer extends AbstractRocketMqConsumerListener<OrderMsgReq> {

    @Override
    public ConsumeOrderlyStatus onMessage(List<MessageExt> msgs, ConsumeOrderlyContext context) {
        for (MessageExt msg : msgs) {
            try {
                OrderMsgReq req = deserialize(msg, OrderMsgReq.class);
                log.info("顺序消费订单消息，orderId={}, userId={}, reconsumeTimes={}",
                    req.getOrderId(), req.getUserId(), msg.getReconsumeTimes());
                processOrderFifo(req);
            } catch (Exception e) {
                log.error("顺序消费失败，msgId={}", msg.getMsgId(), e);
                return ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
            }
        }
        return ConsumeOrderlyStatus.SUCCESS;
    }

    private void processOrderFifo(OrderMsgReq req) {
        log.info("按序处理订单状态变更，orderId={}", req.getOrderId());
    }
}
