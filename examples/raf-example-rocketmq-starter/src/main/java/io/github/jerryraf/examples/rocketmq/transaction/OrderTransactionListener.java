package io.github.jerryraf.examples.rocketmq.transaction;

import com.raf.framework.rocketmq.RocketMqTransactionListener;
import io.github.jerryraf.examples.rocketmq.dto.OrderMsgReq;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.stereotype.Component;

/**
 * 订单事务消息监听器
 * 演示：本地事务执行 + 事务状态回查
 */
@Component
@Slf4j
public class OrderTransactionListener extends RocketMqTransactionListener {

    @Override
    public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        OrderMsgReq req = (OrderMsgReq) arg;
        try {
            log.info("执行本地事务，orderId={}", req.getOrderId());
            // 执行本地数据库操作（扣减库存、创建订单等）
            // 成功则提交消息，让消费者可见
            return LocalTransactionState.COMMIT_MESSAGE;
        } catch (Exception e) {
            log.error("本地事务执行失败，orderId={}", req.getOrderId(), e);
            // 本地事务失败，回滚消息
            return LocalTransactionState.ROLLBACK_MESSAGE;
        }
    }

    @Override
    public LocalTransactionState checkLocalTransaction(MessageExt msg) {
        // Broker 回查本地事务状态（网络超时等情况下触发）
        String orderId = msg.getKeys();
        log.info("事务状态回查，orderId={}", orderId);
        // 查询本地数据库确认事务是否已提交
        boolean committed = checkOrderExists(orderId);
        return committed ? LocalTransactionState.COMMIT_MESSAGE : LocalTransactionState.ROLLBACK_MESSAGE;
    }

    private boolean checkOrderExists(String orderId) {
        // 实际场景：查询订单表确认记录是否存在
        return orderId != null && !orderId.isEmpty();
    }
}
