package io.github.jerryraf.examples.rocketmq.consumer;

import com.raf.framework.rocketmq.AbstractRocketMqConsumerListener;
import com.raf.framework.rocketmq.RocketMqConsumer;
import com.raf.framework.rocketmq.RocketMqMessage;
import io.github.jerryraf.examples.rocketmq.dto.OrderMsgReq;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.List;

/**
 * 订单创建消费者（普通消息）
 */
@RocketMqConsumer(
    topic = "order-created-topic",
    tag = "created",
    consumerGroup = "order-created-consumer-group"
)
@Slf4j
public class OrderCreatedConsumer extends AbstractRocketMqConsumerListener<OrderMsgReq> {

    @Override
    public ConsumeConcurrentlyStatus onMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
        for (MessageExt msg : msgs) {
            try {
                OrderMsgReq req = deserialize(msg, OrderMsgReq.class);
                log.info("消费订单创建消息，orderId={}, userId={}, amount={}",
                    req.getOrderId(), req.getUserId(), req.getAmount());
                // 业务处理：发送通知、更新统计等
                processOrderCreated(req);
            } catch (Exception e) {
                log.error("消费订单创建消息失败，msgId={}", msg.getMsgId(), e);
                return ConsumeConcurrentlyStatus.RECONSUME_LATER;
            }
        }
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }

    private void processOrderCreated(OrderMsgReq req) {
        // 模拟业务处理
        log.info("处理订单创建事件，orderId={}", req.getOrderId());
    }
}
