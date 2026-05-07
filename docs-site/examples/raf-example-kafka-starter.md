# raf-example-kafka-starter

> 演示 `raf-framework-kafka-starter` 的核心能力：高吞吐生产者（同步/异步）、手动 ACK 消费者、幂等生产者、消息 key 路由分区。

## 功能概述

`raf-framework-kafka-starter` 提供：

- **KafkaProducer**：封装同步/异步发送、事务消息、批量发送
- **@KafkaConsumer**：注解驱动消费者，支持并发度、ACK 模式、批量消费
- **AbstractKafkaConsumerListener**：消费者抽象基类，自动反序列化、traceId 传播
- **幂等生产者**：`enable-idempotence=true`，Broker 端去重，精确一次语义

## 快速接入

### 1. 引入依赖

```xml
<dependency>
    <groupId>io.github.jerryraf</groupId>
    <artifactId>raf-framework-kafka-starter</artifactId>
</dependency>
```

### 2. 配置（`application.yml`）

```yaml
raf:
  kafka:
    enabled: true
    bootstrap-servers: ${KAFKA_SERVERS:localhost:9092}
    producer:
      acks: all                    # 等待所有副本确认
      retries: 3
      batch-size: 16384            # 16KB 批量发送
      linger-ms: 5                 # 最多等待 5ms 凑批
      compression-type: snappy     # 压缩减少网络传输
      enable-idempotence: true     # 幂等生产者
    consumer:
      group-id: my-service-group
      enable-auto-commit: false    # 手动提交 offset
      auto-offset-reset: latest
      max-poll-records: 100
      concurrency: 3
```

## 配置项详解

### 生产者（raf.kafka.producer）

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `acks` | string | `all` | 确认模式：`0`不等待 / `1`Leader确认 / `all`所有副本确认 |
| `retries` | int | `3` | 发送失败重试次数 |
| `batch-size` | int | `16384` | 批量发送大小（字节） |
| `linger-ms` | int | `0` | 凑批等待时间（毫秒），>0 可提升吞吐 |
| `compression-type` | string | `none` | 压缩：`none` / `gzip` / `snappy` / `lz4` / `zstd` |
| `enable-idempotence` | boolean | `true` | 幂等生产者（精确一次） |
| `transactional-id-prefix` | string | — | 事务 ID 前缀，配置后启用事务消息 |

### 消费者（raf.kafka.consumer）

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `group-id` | string | `DEFAULT_CONSUMER_GROUP` | 消费者组 ID |
| `enable-auto-commit` | boolean | `false` | 是否自动提交 offset（推荐 false） |
| `auto-offset-reset` | string | `latest` | 无 offset 时从哪里开始：`earliest` / `latest` |
| `max-poll-records` | int | `500` | 单次 poll 最大消息数 |
| `concurrency` | int | `3` | 消费者线程数（不超过分区数） |
| `isolation-level` | string | `read_committed` | 事务隔离级别 |

## 核心用法

### 同步发送（等待 Broker 确认）

```java
@Autowired
private KafkaProducer kafkaProducer;

// 简化 API
KafkaMessage.SendResult result = kafkaProducer.sendSync("order-events", orderId, orderDTO);
log.info("发送成功，partition={}, offset={}", result.getPartition(), result.getOffset());

// 完整 API（指定分区、自定义 Header）
KafkaMessage<OrderDTO> message = KafkaMessage.<OrderDTO>builder()
    .topic("order-events")
    .key(orderId)                    // key 决定路由到哪个分区，相同 key 保证有序
    .body(orderDTO)
    .build();
KafkaMessage.SendResult result = kafkaProducer.sendSync(message);
```

### 异步发送（高吞吐场景）

```java
kafkaProducer.sendAsync(message, (metadata, exception) -> {
    if (exception != null) {
        log.error("发送失败，topic={}", message.getTopic(), exception);
        // 写入本地补偿表，后台重试
    } else {
        log.debug("发送成功，partition={}, offset={}", metadata.partition(), metadata.offset());
    }
});
```

### 消费者（手动 ACK）

```java
@KafkaConsumer(
    topics = {"order-events"},
    groupId = "order-consumer-group",
    concurrency = 3,
    ackMode = "MANUAL_IMMEDIATE"
)
public class OrderEventConsumer extends AbstractKafkaConsumerListener<OrderDTO> {

    @Override
    public void onMessage(ConsumerRecord<String, String> record, OrderDTO order, Acknowledgment ack) {
        try {
            log.info("消费订单事件，orderId={}, partition={}, offset={}",
                order.getOrderId(), record.partition(), record.offset());

            processOrder(order);

            ack.acknowledge();  // 处理成功后手动提交
        } catch (Exception e) {
            log.error("消费失败，key={}", record.key(), e);
            // 不 ack，消息会被重新投递
        }
    }
}
```

### 批量消费

```java
@KafkaConsumer(
    topics = {"log-events"},
    groupId = "log-batch-group",
    containerType = KafkaConsumer.ContainerType.BATCH,
    concurrency = 2
)
public class LogBatchConsumer extends AbstractKafkaConsumerListener<LogEventReq> {

    @Override
    public void onBatchMessage(List<ConsumerRecord<String, String>> records,
                               List<LogEventReq> events, Acknowledgment ack) {
        log.info("批量消费 {} 条日志", events.size());
        // 批量写入存储（ES、ClickHouse 等）
        bulkInsert(events);
        ack.acknowledge();
    }
}
```

### 事务消息

```yaml
raf:
  kafka:
    producer:
      transactional-id-prefix: order-tx-
```

```java
kafkaProducer.executeInTransaction(txProducer -> {
    txProducer.send("order-created", orderId, orderDTO);
    txProducer.send("inventory-deduct", productId, deductDTO);
    // 两条消息原子提交，消费者 isolation-level=read_committed 才能看到
});
```

## 示例项目结构

```
raf-example-kafka-starter/
├── src/main/java/io/github/jerryraf/examples/kafka/
│   ├── KafkaExampleApplication.java
│   ├── controller/
│   │   └── LogEventController.java     # POST /api/events/sync|async
│   ├── producer/
│   │   └── LogEventProducer.java       # 同步/异步发送演示
│   ├── consumer/
│   │   └── LogEventConsumer.java       # 手动 ACK 消费者
│   └── dto/
│       └── LogEventReq.java
└── src/main/resources/
    └── application.yml
```

## API 接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/events/sync` | 同步发送日志事件（等待 Broker 确认） |
| POST | `/api/events/async` | 异步发送日志事件（高吞吐） |

## 最佳实践

1. **key 设计**：相同业务实体（如同一订单）使用相同 key，保证消息路由到同一分区，实现局部有序
2. **分区数**：分区数 = 消费者并发数，避免消费者空闲或竞争
3. **幂等消费**：消费者必须幂等，Kafka 至少一次语义可能重复投递
4. **offset 提交**：使用 `MANUAL_IMMEDIATE`，处理成功后再 ack，避免消息丢失
5. **批量发送**：设置 `linger-ms=5` + `batch-size=65536`，显著提升吞吐量
6. **压缩**：生产环境推荐 `snappy`，CPU 开销小，压缩比适中

## 常见问题

**Q: 消费者数量超过分区数会怎样？**

A: 多余的消费者会空闲，不消费任何消息。分区是 Kafka 并发的基本单位，消费者数不应超过分区数。

**Q: `auto-offset-reset: latest` 和 `earliest` 的区别？**

A: `latest` 从最新消息开始消费（新消费者组默认），`earliest` 从最早未删除消息开始消费（适合数据回溯）。

**Q: 消息发送失败如何保证不丢失？**

A: 配置 `acks=all` + `retries=3` + `enable-idempotence=true`，发送失败会抛出 `InfrastructureException`，业务侧捕获后写入本地补偿表，后台定时重试。

**Q: 如何监控消费者 lag？**

A: 使用 `kafka-consumer-groups.sh --describe` 或接入 Kafka Exporter + Prometheus + Grafana 监控 `kafka_consumer_group_lag` 指标。
