package com.raf.framework.rocketmq;

import java.io.Serializable;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RocketMQ 消息封装
 *
 * @author RAF Framework
 * @date 2025/01/01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RocketMqMessage<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息主题
     */
    private String topic;

    /**
     * 消息标签
     */
    private String tag;

    /**
     * 消息业务键
     */
    private String key;

    /**
     * 消息体
     */
    private T body;

    /**
     * 延时级别（0-18）
     * 1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h
     */
    private int delayLevel;

    /**
     * 自定义属性
     */
    private Map<String, String> properties;

    /**
     * 消息发送时间戳
     */
    private Long timestamp;

    /**
     * 顺序消息的分区键（HashKey）
     */
    private String shardingKey;

    /**
     * 消息类型：NORMAL(普通消息)、FIFO(顺序消息)、DELAY(延时消息)、TRANSACTION(事务消息)
     */
    @Builder.Default
    private MessageType messageType = MessageType.NORMAL;

    /**
     * 消息类型枚举
     */
    public enum MessageType {
        /**
         * 普通消息
         */
        NORMAL,

        /**
         * 顺序消息（FIFO）
         */
        FIFO,

        /**
         * 延时消息
         */
        DELAY,

        /**
         * 事务消息
         */
        TRANSACTION
    }
}
