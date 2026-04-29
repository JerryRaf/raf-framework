# Kafka 消息队列（kafka-starter）

## 功能概述

`raf-framework-kafka-starter` 封装 Kafka 生产者、事务发送、消费者注册、traceId 透传和基础安全配置。组件默认关闭，配置 `raf.kafka.enabled=true` 后启用。

- **生产者封装**：`KafkaProducer` 支持 `sendSync`、`sendAsync` 和事务发送
- **默认可靠发送**：默认 `acks=all`、`enableIdempotence=true`、`isolationLevel=read_committed`
- **手动提交偏移量**：默认 `enableAutoCommit=false`，`MANUAL_IMMEDIATE` 只在业务处理成功后 ACK
- **错误反压**：消费失败会抛出异常，不会提交 offset，避免失败消息被静默跳过
- **事务生产者**：配置 `raf.kafka.producer.transactionalIdPrefix` 后启用，框架初始化 `initTransactions()`
- **安全接入**：支持 SSL、SASL_PLAINTEXT、SASL_SSL

## 配置项

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `raf.kafka.enabled` | boolean | `false` | 是否启用 Kafka |
| `raf.kafka.bootstrapServers` | string | — | Broker 地址，逗号分隔 |
| `raf.kafka.producer.acks` | string | `all` | 确认机制：`0` / `1` / `all` |
| `raf.kafka.producer.retries` | int | `3` | 发送失败重试次数 |
| `raf.kafka.producer.batchSize` | int | `16384` | 批量发送大小，单位字节 |
| `raf.kafka.producer.lingerMs` | int | `0` | 批量等待时间，单位毫秒 |
| `raf.kafka.producer.bufferMemory` | long | `33554432` | 发送缓冲区大小 |
| `raf.kafka.producer.compressionType` | string | `none` | 压缩类型：none/gzip/snappy/lz4/zstd |
| `raf.kafka.producer.enableIdempotence` | boolean | `true` | 是否启用幂等生产者 |
| `raf.kafka.producer.transactionalIdPrefix` | string | — | 事务生产者前缀，配置后启用事务发送 |
| `raf.kafka.consumer.groupId` | string | `DEFAULT_CONSUMER_GROUP` | 默认消费者组 ID |
| `raf.kafka.consumer.enableAutoCommit` | boolean | `false` | 是否自动提交偏移量 |
| `raf.kafka.consumer.autoOffsetReset` | string | `latest` | 偏移量重置策略 |
| `raf.kafka.consumer.maxPollRecords` | int | `500` | 单次拉取最大消息数 |
| `raf.kafka.consumer.isolationLevel` | string | `read_committed` | 消费事务消息隔离级别 |
| `raf.kafka.consumer.concurrency` | int | `3` | 默认监听容器并发数 |
| `raf.kafka.security.protocol` | string | `PLAINTEXT` | 安全协议 |
| `raf.kafka.security.saslMechanism` | string | — | SASL 机制 |
| `raf.kafka.security.saslJaasConfig` | string | — | SASL JAAS 配置 |

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
      acks: all
      enableIdempotence: true
    consumer:
      groupId: order-consumer-group
      enableAutoCommit: false
      autoOffsetReset: latest
```

## 发送消息

```java
@Autowired
private KafkaProducer kafkaProducer;

public void publish(OrderDTO order) {
    KafkaMessage<OrderDTO> message = KafkaMessage.<OrderDTO>builder()
            .topic("order-topic")
            .key(order.getOrderId())
            .body(order)
            .build();

    kafkaProducer.sendSync(message);
    kafkaProducer.sendAsync(message);
}
```

## 事务发送

```yaml
raf:
  kafka:
    producer:
      transactionalIdPrefix: order-service-tx-
```

```java
kafkaProducer.executeInTransaction(tx -> {
    tx.send("order-topic", orderId, orderDTO);
    tx.send("audit-topic", orderId, auditDTO);
});
```

## 消费消息

```java
@KafkaConsumer(
        topics = "order-topic",
        groupId = "${raf.kafka.consumer.groupId}",
        ackMode = "MANUAL_IMMEDIATE")
public class OrderKafkaConsumer extends AbstractKafkaConsumerListener<OrderDTO> {

    @Override
    protected void handleMessage(OrderDTO body, ConsumerRecord<String, String> record) {
        orderService.process(body);
    }
}
```

最佳实践：

- 消费者默认只支持 `SINGLE` 模式；`BATCH` 当前会显式报错，避免容器启动后没有 listener 的隐性故障。
- 业务处理成功后框架才 ACK；处理失败会抛异常并保留 offset，避免消息丢失。
- 高可靠场景保持 `acks=all` 和 `enableIdempotence=true`；高吞吐场景可再评估 `lingerMs`、`batchSize`、压缩类型。
- 生产环境建议启用 SASL/SSL，并把 JAAS、证书密码通过 Jasypt 加密。
