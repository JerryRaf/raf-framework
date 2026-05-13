package com.raf.framework.rocketmq;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RocketMQ 配置属性
 *
 * @author RAF Framework
 * @date 2025/01/01
 */
@Data
@ConfigurationProperties("raf.rocketmq")
public class RocketMqProperties {

    /**
     * NameServer地址，多个地址用分号分隔
     */
    private String nameServer;

    /**
     * 生产者配置
     */
    private Producer producer;

    /**
     * 消费者配置
     */
    private Consumer consumer;

    /**
     * 是否启用RocketMQ
     */
    private boolean enabled = false;

    /**
     * 访问凭证配置（阿里云RocketMQ需要）
     */
    private Credentials credentials;

    @Data
    public static class Producer {
        /**
         * 生产者组名
         */
        private String group = "DEFAULT_PRODUCER_GROUP";

        /**
         * 消息发送超时时间（毫秒）
         */
        private int sendMsgTimeout = 3000;

        /**
         * 消息体超过该值则启用压缩（字节）
         */
        private int compressMsgBodyOverHowmuch = 4096;

        /**
         * 同步发送失败重试次数
         */
        private int retryTimesWhenSendFailed = 2;

        /**
         * 异步发送失败重试次数
         */
        private int retryTimesWhenSendAsyncFailed = 2;

        /**
         * 消息发送失败是否重试其他broker
         */
        private boolean retryAnotherBrokerWhenNotStoreOK = false;

        /**
         * 消息最大大小（字节）
         */
        private int maxMessageSize = 1024 * 1024 * 4; // 4MB

        /**
         * 是否启用事务消息
         */
        private boolean enableTransaction = false;

        /**
         * 事务消息回查线程池大小
         */
        private int checkThreadPoolSize = 1;

        /**
         * 事务消息回查线程池队列容量
         */
        private int checkThreadPoolQueueCapacity = 2000;
    }

    @Data
    public static class Consumer {
        /**
         * 消费者组名
         */
        private String group = "DEFAULT_CONSUMER_GROUP";

        /**
         * 消费模式：集群消费(CLUSTERING)或广播消费(BROADCASTING)
         */
        private String messageModel = "CLUSTERING";

        /**
         * 消费方式：推模式(PUSH)或拉模式(PULL)
         */
        private String consumeMode = "PUSH";

        /**
         * 消费起始位置：最后位置(CONSUME_FROM_LAST_OFFSET)、第一个位置(CONSUME_FROM_FIRST_OFFSET)、指定时间(CONSUME_FROM_TIMESTAMP)
         */
        private String consumeFromWhere = "CONSUME_FROM_LAST_OFFSET";

        /**
         * 消费线程池最小线程数
         */
        private int consumeThreadMin = 20;

        /**
         * 消费线程池最大线程数
         */
        private int consumeThreadMax = 64;

        /**
         * 消费失败最大重试次数（-1表示16次）
         */
        private int maxReconsumeTimes = -1;

        /**
         * 消费超时时间（分钟）
         */
        private long consumeTimeout = 15;

        /**
         * 批量消费消息条数
         */
        private int consumeMessageBatchMaxSize = 1;

        /**
         * 批量拉取消息条数
         */
        private int pullBatchSize = 32;
    }

    @Data
    public static class Credentials {
        /**
         * AccessKey（阿里云RocketMQ）
         */
        private String accessKey;

        /**
         * SecretKey（阿里云RocketMQ）
         */
        private String secretKey;

        /**
         * 安全令牌（可选）
         */
        private String securityToken;
    }
}
