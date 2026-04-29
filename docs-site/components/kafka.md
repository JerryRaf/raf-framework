# Kafka 消息队列（kafka-starter）

## 功能概述

- **高吞吐量生产者**：KafkaProducer 封装，支持同步/异步发送
- **消费者抽象**：AbstractKafkaConsumerListener 统一消费逻辑
- **消息模型**：KafkaMessage 统一消息结构

## 配置项

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `raf.kafka.enabled` | boolean | `false` | 是否启用 Kafka |
| `raf.kafka.bootstrapServers` | string | — | Broker 地址，逗号分隔 |
| `raf.kafka.producer.acks` | string | `1` | 确认机制：0/1/all |
| `raf.kafka.producer.retries` | int | `3` | 发送失败重试次数 |
| `raf.kafka.producer.batchSize` | int | `16384` | 批量发送大小（字节） |
| `raf.kafka.producer.lingerMs` | int | `1` | 批量等待时间（毫秒） |
| `raf.kafka.producer.bufferMemory` | long | `33554432` | 缓冲区大小（32MB） |
| `raf.kafka.consumer.groupId` | string | — | 消费者组 ID |
| `raf.kafka.consumer.autoOffsetReset` | string | `latest` | 偏移量重置策略：earliest/latest |
| `raf.kafka.consumer.enableAutoCommit` | boolean | `false` | 是否自动提交偏移量 |
| `raf.kafka.consumer.maxPollRecords` | int | `500` | 单次拉取最大消息数 |

## 快速接入

```xml
<dependency>
    <groupId>io.github.jerryraf</groupId>
    <artifactId>raf-framework-kafka-starter</artifactId>
</dependency>
```

```yaml
raf:
  kafka:
    enabled: true
    bootstrapServers: 127.0.0.1:9092
    producer:
      acks: 1
      retries: 3
    consumer:
      groupId: my-consumer-group
      autoOffsetReset: latest
      enableAutoCommit: false
```

## 核心用法

### 发送消息

```java
@Autowired
private KafkaProducer kafkaProducer;

// 同步发送
KafkaMessage message = new KafkaMessage();
message.setTopic("order-topic");
message.setKey(orderId.toString());
message.setBody(orderDTO);
kafkaProducer.syncSend(message);

// 异步发送
kafkaProducer.asyncSend(message, (metadata, exception) -> {
    if (exception != null) {
        log.error("Kafka 发送失败", exception);
    } else {
        log.info("发送成功，offset: {}", metadata.offset());
    }
});
```

### 消费消息

```java
@Component
public class OrderKafkaConsumer extends AbstractKafkaConsumerListener<OrderDTO> {

    @KafkaListener(topics = "order-topic", groupId = "order-consumer-group")
    @Override
    public void listen(ConsumerRecord<String, String> record, Acknowledgment ack) {
        super.listen(record, ack);
    }

    @Override
    protected void handleMessage(OrderDTO orderDTO, ConsumerRecord<String, String> record) {
        orderService.processOrder(orderDTO);
    }
}
```

## 常见问题

**Q: 消费者启动后一直没有消息？**

A: 检查 `autoOffsetReset` 配置。`latest` 只消费启动后的新消息，`earliest` 从最早的消息开始消费。

**Q: 消息重复消费如何处理？**

A: 设置 `enableAutoCommit: false`，在业务处理成功后手动调用 `ack.acknowledge()` 提交偏移量，并在业务层实现幂等。

**Q: 生产者发送性能不够？**

A: 调大 `batchSize` 和 `lingerMs`，允许更多消息批量发送。同时确认 `acks: 1` 而非 `all`（`all` 需要等待所有副本确认）。
