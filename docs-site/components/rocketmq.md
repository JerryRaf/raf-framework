# RocketMQ 消息队列（rocketmq-starter）

## 功能概述

- **四种消息类型**：NORMAL（普通）、FIFO（顺序）、DELAY（延时）、TRANSACTION（事务）
- **事务消息**：本地事务与消息发送原子性保证，支持回查机制
- **消费者抽象**：`AbstractRocketMqConsumerListener` 统一消费逻辑
- **阿里云支持**：内置 AccessKey/SecretKey 凭证配置

## 配置项

### 基础配置

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `raf.rocketmq.enabled` | boolean | `false` | 是否启用 RocketMQ |
| `raf.rocketmq.nameServer` | string | — | NameServer 地址，多个用分号分隔 |

### 生产者（raf.rocketmq.producer）

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `group` | string | `DEFAULT_PRODUCER_GROUP` | 生产者组名 |
| `sendMsgTimeout` | int | `3000` | 发送超时（毫秒） |
| `retryTimesWhenSendFailed` | int | `2` | 同步发送失败重试次数 |
| `retryTimesWhenSendAsyncFailed` | int | `2` | 异步发送失败重试次数 |
| `maxMessageSize` | int | `4194304` | 消息最大大小（4MB） |
| `enableTransaction` | boolean | `false` | 是否启用事务消息 |

### 消费者（raf.rocketmq.consumer）

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `group` | string | `DEFAULT_CONSUMER_GROUP` | 消费者组名 |
| `messageModel` | string | `CLUSTERING` | 消费模式：CLUSTERING（集群）/ BROADCASTING（广播） |
| `consumeThreadMin` | int | `20` | 消费线程池最小线程数 |
| `consumeThreadMax` | int | `64` | 消费线程池最大线程数 |
| `maxReconsumeTimes` | int | `-1` | 最大重试次数（-1 表示 16 次） |
| `consumeTimeout` | long | `15` | 消费超时时间（分钟） |

### 阿里云凭证（raf.rocketmq.credentials）

| 配置键 | 类型 | 说明 |
|---|---|---|
| `accessKey` | string | 阿里云 AccessKey |
| `secretKey` | string | 阿里云 SecretKey |
| `securityToken` | string | 安全令牌（可选） |

## 快速接入

**1. 引入依赖**

```xml
<dependency>
    <groupId>io.github.jerryraf</groupId>
    <artifactId>raf-framework-rocketmq-starter</artifactId>
</dependency>
```

**2. 配置**（`application.yml`）

```yaml
raf:
  rocketmq:
    enabled: true
    nameServer: 127.0.0.1:9876
    producer:
      group: my-producer-group
      sendMsgTimeout: 3000
    consumer:
      group: my-consumer-group
      consumeThreadMin: 20
      consumeThreadMax: 64
```

## 核心用法

### 发送普通消息

```java
@Autowired
private RocketMqProducer rocketMqProducer;

// 同步发送
RocketMqMessage message = new RocketMqMessage();
message.setTopic("ORDER_TOPIC");
message.setTag("ORDER_CREATED");
message.setBody(orderDTO);
SendResult result = rocketMqProducer.syncSend(message);

// 异步发送
rocketMqProducer.asyncSend(message, new SendCallback() {
    @Override
    public void onSuccess(SendResult sendResult) {
        log.info("消息发送成功: {}", sendResult.getMsgId());
    }
    @Override
    public void onException(Throwable e) {
        log.error("消息发送失败", e);
    }
});
```

### 发送延时消息

```java
// RocketMQ 延时级别：1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h
message.setDelayTimeLevel(3); // 10 秒后投递
rocketMqProducer.syncSend(message);
```

### 消费消息

```java
@Component
@RocketMQMessageListener(
    topic = "ORDER_TOPIC",
    consumerGroup = "order-consumer-group",
    selectorExpression = "ORDER_CREATED"
)
public class OrderConsumer extends AbstractRocketMqConsumerListener<OrderDTO> {

    @Override
    protected void handleMessage(OrderDTO orderDTO) {
        orderService.processOrder(orderDTO);
    }
}
```

### 事务消息

```java
// 1. 配置启用事务
// raf.rocketmq.producer.enableTransaction: true

// 2. 实现事务监听器
@Component
public class OrderTransactionListener implements RocketMqTransactionListener {

    @Override
    public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        try {
            // 执行本地事务
            orderService.createOrder((OrderDTO) arg);
            return LocalTransactionState.COMMIT_MESSAGE;
        } catch (Exception e) {
            return LocalTransactionState.ROLLBACK_MESSAGE;
        }
    }

    @Override
    public LocalTransactionState checkLocalTransaction(MessageExt msg) {
        // 回查本地事务状态
        String orderId = msg.getUserProperty("orderId");
        return orderService.exists(orderId)
            ? LocalTransactionState.COMMIT_MESSAGE
            : LocalTransactionState.ROLLBACK_MESSAGE;
    }
}

// 3. 发送事务消息
rocketMqProducer.sendMessageInTransaction(message, orderDTO);
```

## 常见问题

**Q: 消费者启动后没有收到消息？**

A: 检查 `consumerGroup` 是否与其他消费者组重复，以及 `topic` 和 `selectorExpression`（Tag）是否与生产者一致。

**Q: 事务消息一直处于 UNKNOWN 状态？**

A: 确保 `RocketMqTransactionListener` 的 `checkLocalTransaction` 方法能正确查询本地事务状态，且 NameServer 网络可达。

**Q: 消息重复消费如何处理？**

A: RocketMQ 保证至少一次投递，业务层需实现幂等。推荐以消息 ID（`msg.getMsgId()`）为幂等键，存入 Redis 或数据库去重表。
