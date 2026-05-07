# raf-example-rocketmq-starter

> 演示 `raf-framework-rocketmq-starter` 的核心能力：普通消息、顺序消息（FIFO）、延时消息、事务消息四种消息类型。

## 功能概述

`raf-framework-rocketmq-starter` 提供：

- **RocketMqProducer**：封装同步/异步/单向发送，支持四种消息类型
- **@RocketMqConsumer**：注解驱动消费者，支持并发消费和顺序消费
- **AbstractRocketMqConsumerListener**：消费者抽象基类，自动反序列化、traceId 传播、重试机制
- **RocketMqTransactionListener**：事务消息监听器，本地事务执行 + 状态回查

## 快速接入

### 1. 引入依赖

```xml
<dependency>
    <groupId>io.github.jerryraf</groupId>
    <artifactId>raf-framework-rocketmq-starter</artifactId>
</dependency>
```

### 2. 配置（`application.yml`）

```yaml
raf:
  rocketmq:
    enabled: true
    name-server: ${ROCKETMQ_NAMESERVER:localhost:9876}
    producer:
      group: my-producer-group
      send-msg-timeout: 3000
      retry-times-when-send-failed: 2
      enable-transaction: true       # 启用事务消息
    consumer:
      group: my-consumer-group
      consume-thread-min: 5
      consume-thread-max: 20
```

## 配置项详解

### 生产者（raf.rocketmq.producer）

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `group` | string | `DEFAULT_PRODUCER_GROUP` | 生产者组名 |
| `send-msg-timeout` | int | `3000` | 发送超时（毫秒） |
| `retry-times-when-send-failed` | int | `2` | 同步发送失败重试次数 |
| `retry-times-when-send-async-failed` | int | `2` | 异步发送失败重试次数 |
| `compress-msg-body-over-howmuch` | int | `4096` | 消息体超过此值启用压缩（字节） |
| `max-message-size` | int | `4194304` | 消息最大大小（4MB） |
| `enable-transaction` | boolean | `false` | 是否启用事务消息 |

### 消费者（raf.rocketmq.consumer）

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `group` | string | `DEFAULT_CONSUMER_GROUP` | 消费者组名 |
| `message-model` | string | `CLUSTERING` | 消费模式：`CLUSTERING`集群 / `BROADCASTING`广播 |
| `consume-from-where` | string | `CONSUME_FROM_LAST_OFFSET` | 消费起始位置 |
| `consume-thread-min` | int | `20` | 消费线程池最小线程数 |
| `consume-thread-max` | int | `64` | 消费线程池最大线程数 |
| `max-reconsume-times` | int | `-1` | 最大重试次数（-1 表示 16 次） |
| `consume-timeout` | long | `15` | 消费超时（分钟） |

### 阿里云 RocketMQ 凭证

```yaml
raf:
  rocketmq:
    credentials:
      access-key: ${ROCKETMQ_ACCESS_KEY}
      secret-key: ${ROCKETMQ_SECRET_KEY}
```

## 四种消息类型

### 1. 普通消息（NORMAL）

```java
@Autowired
private RocketMqProducer rocketMqProducer;

// 简化 API
SendResult result = rocketMqProducer.sendSync("order-topic", "created", orderDTO);

// 完整 API
RocketMqMessage<OrderDTO> msg = RocketMqMessage.<OrderDTO>builder()
    .topic("order-topic")
    .tag("created")
    .key(orderId)           // 业务唯一键，用于消息追踪和幂等
    .body(orderDTO)
    .build();
SendResult result = rocketMqProducer.sendSync(msg);
```

消费者：

```java
@RocketMqConsumer(
    topic = "order-topic",
    tag = "created",
    consumerGroup = "order-consumer-group"
)
public class OrderConsumer extends AbstractRocketMqConsumerListener<OrderDTO> {

    @Override
    public ConsumeConcurrentlyStatus onMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext ctx) {
        for (MessageExt msg : msgs) {
            OrderDTO order = deserialize(msg, OrderDTO.class);
            try {
                processOrder(order);
            } catch (Exception e) {
                log.error("消费失败，msgId={}", msg.getMsgId(), e);
                return ConsumeConcurrentlyStatus.RECONSUME_LATER;
            }
        }
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }
}
```

### 2. 顺序消息（FIFO）

同一 `shardingKey`（如 userId）的消息严格按发送顺序消费：

```java
RocketMqMessage<OrderDTO> msg = RocketMqMessage.<OrderDTO>builder()
    .topic("order-fifo-topic")
    .tag("status-change")
    .key(orderId)
    .body(orderDTO)
    .shardingKey(String.valueOf(userId))  // 相同 userId 路由到同一队列
    .messageType(RocketMqMessage.MessageType.FIFO)
    .build();
rocketMqProducer.sendSync(msg);
```

顺序消费者：

```java
@RocketMqConsumer(
    topic = "order-fifo-topic",
    tag = "status-change",
    consumerGroup = "order-fifo-group",
    messageType = RocketMqMessage.MessageType.FIFO
)
public class OrderFifoConsumer extends AbstractRocketMqConsumerListener<OrderDTO> {

    @Override
    public ConsumeOrderlyStatus onMessage(List<MessageExt> msgs, ConsumeOrderlyContext ctx) {
        for (MessageExt msg : msgs) {
            OrderDTO order = deserialize(msg, OrderDTO.class);
            processOrderStatus(order);
        }
        return ConsumeOrderlyStatus.SUCCESS;
    }
}
```

### 3. 延时消息（DELAY）

RocketMQ 支持 18 个延时级别：1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h

```java
RocketMqMessage<OrderDTO> msg = RocketMqMessage.<OrderDTO>builder()
    .topic("order-delay-topic")
    .tag("timeout-check")
    .key(orderId)
    .body(orderDTO)
    .delayLevel(4)   // 4 = 30s 后投递
    .messageType(RocketMqMessage.MessageType.DELAY)
    .build();
rocketMqProducer.sendSync(msg);
```

| delayLevel | 延时时间 |
|---|---|
| 1 | 1s |
| 3 | 10s |
| 4 | 30s |
| 5 | 1min |
| 14 | 10min |
| 16 | 30min |
| 17 | 1h |
| 18 | 2h |

### 4. 事务消息（TRANSACTION）

```java
// 发送事务消息（Half Message）
RocketMqMessage<OrderDTO> msg = RocketMqMessage.<OrderDTO>builder()
    .topic("order-tx-topic")
    .tag("tx")
    .key(orderId)
    .body(orderDTO)
    .messageType(RocketMqMessage.MessageType.TRANSACTION)
    .build();
rocketMqProducer.sendTransactionMessage(msg, orderDTO);  // 第二个参数传给本地事务
```

事务监听器：

```java
@Component
public class OrderTransactionListener extends RocketMqTransactionListener {

    @Override
    public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        OrderDTO order = (OrderDTO) arg;
        try {
            // 执行本地数据库操作
            orderService.createOrder(order);
            return LocalTransactionState.COMMIT_MESSAGE;  // 提交，消费者可见
        } catch (Exception e) {
            return LocalTransactionState.ROLLBACK_MESSAGE; // 回滚，消费者不可见
        }
    }

    @Override
    public LocalTransactionState checkLocalTransaction(MessageExt msg) {
        // Broker 回查（网络超时等情况触发）
        String orderId = msg.getKeys();
        boolean exists = orderService.exists(orderId);
        return exists ? LocalTransactionState.COMMIT_MESSAGE : LocalTransactionState.ROLLBACK_MESSAGE;
    }
}
```

## 示例项目结构

```
raf-example-rocketmq-starter/
├── src/main/java/io/github/jerryraf/examples/rocketmq/
│   ├── RocketMqExampleApplication.java
│   ├── controller/
│   │   └── OrderMqController.java      # POST /api/mq/{normal,fifo,delay,transaction}
│   ├── producer/
│   │   └── OrderProducer.java          # 四种消息类型发送
│   ├── consumer/
│   │   ├── OrderCreatedConsumer.java   # 普通消息消费者
│   │   └── OrderFifoConsumer.java      # 顺序消息消费者
│   ├── transaction/
│   │   └── OrderTransactionListener.java  # 事务消息监听器
│   └── dto/
│       └── OrderMsgReq.java
└── src/main/resources/
    └── application.yml
```

## API 接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/mq/normal` | 发送普通消息 |
| POST | `/api/mq/fifo` | 发送顺序消息（同 userId 有序） |
| POST | `/api/mq/delay` | 发送延时消息 |
| POST | `/api/mq/transaction` | 发送事务消息 |

## 最佳实践

1. **消息 key**：设置业务唯一键（如 orderId），便于消息追踪和幂等消费
2. **幂等消费**：消费者必须幂等，RocketMQ 至少一次语义可能重复投递，用 Redis `setIfAbsent` 去重
3. **顺序消息**：仅在必要时使用，顺序消息吞吐低于普通消息，且单队列故障会阻塞后续消息
4. **延时消息 vs DLX**：RocketMQ 延时消息比 RabbitMQ DLX 更简单，但只支持固定 18 个级别
5. **事务消息**：`checkLocalTransaction` 必须实现，否则 Broker 回查超时后会回滚消息
6. **死信队列**：消息重试超过 `maxReconsumeTimes` 后进入死信 Topic（`%DLQ%{consumerGroup}`），需监控处理

## 常见问题

**Q: 消费者重试多少次后进入死信队列？**

A: 默认 16 次（`max-reconsume-times=-1`），超过后消息进入 `%DLQ%{consumerGroup}` 死信 Topic，需订阅该 Topic 做人工处理或告警。

**Q: 顺序消息消费失败怎么办？**

A: 返回 `SUSPEND_CURRENT_QUEUE_A_MOMENT`，当前队列暂停消费并重试，直到成功或超过重试次数。注意这会阻塞同队列后续消息。

**Q: 事务消息的 `checkLocalTransaction` 什么时候触发？**

A: 当 `executeLocalTransaction` 返回 `UNKNOW` 或网络超时时，Broker 会定期（默认 60s）回查，最多回查 15 次。
