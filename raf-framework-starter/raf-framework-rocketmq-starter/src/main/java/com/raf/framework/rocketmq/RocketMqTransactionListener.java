package com.raf.framework.rocketmq;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;

/**
 * RocketMQ 事务消息监听器抽象类
 * 子类需要实现executeLocalTransaction和checkLocalTransaction方法
 *
 * @author RAF Framework
 * @date 2025/01/01
 */
@Slf4j
public abstract class RocketMqTransactionListener implements TransactionListener {

    /**
     * 执行本地事务
     * 当发送半消息成功后，会调用此方法执行本地事务
     *
     * @param msg 半消息
     * @param arg 发送消息时传入的参数
     * @return LocalTransactionState.COMMIT_MESSAGE: 提交事务，消息对消费者可见
     * LocalTransactionState.ROLLBACK_MESSAGE: 回滚事务，消息会被删除
     * LocalTransactionState.UNKNOW: 中间状态，需要MQ服务器回查
     */
    @Override
    public abstract LocalTransactionState executeLocalTransaction(Message msg, Object arg);

    /**
     * 检查本地事务状态
     * 当半消息发送成功后，如果executeLocalTransaction返回UNKNOW，
     * 或者没有返回，MQ服务器会定期回查本地事务状态
     *
     * @param msg 检查消息
     * @return LocalTransactionState.COMMIT_MESSAGE: 提交事务
     * LocalTransactionState.ROLLBACK_MESSAGE: 回滚事务
     * LocalTransactionState.UNKNOW: 无法确定状态，等待下次回查
     */
    @Override
    public abstract LocalTransactionState checkLocalTransaction(MessageExt msg);
}
