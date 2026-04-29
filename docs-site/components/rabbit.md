# RabbitMQ 消息队列（rabbit-starter）

## 功能概述

- **Provider/Consumer 分离**：生产者和消费者独立配置，职责清晰
- **延时队列**：基于死信交换机（DLX）实现延时消息
- **消息确认机制**：支持手动 ACK，确保消息不丢失
- **消息缓存管理**：`RabbitMessageCacheMgr` 防止重复消费
- **发送确认回调**：`AbstractRabbitSenderConfirm` 处理发送失败场景

## 配置项

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `raf.rabbit.enabled` | boolean | `false` | 是否启用 RabbitMQ |
| `raf.rabbit.host` | string | — | RabbitMQ 主机 |
| `raf.rabbit.port` | int | `5672` | 端口 |
| `raf.rabbit.username` | string | — | 用户名 |
| `raf.rabbit.password` | string | — | 密码 |
| `raf.rabbit.virtualHost` | string | `/` | 虚拟主机 |
| `raf.rabbit.publisher-confirm-type` | enum | `CORRELATED` | 发送确认类型 |
| `raf.rabbit.publisher-returns` | boolean | `true` | 是否启用消息返回 |

## 快速接入

**1. 引入依赖**

```xml
<dependency>
    <groupId>io.github.jerryraf</groupId>
    <artifactId>raf-framework-rabbit-starter</artifactId>
</dependency>
```

**2. 配置**（`application.yml`）

```yaml
raf:
  rabbit:
    enabled: true
    host: 127.0.0.1
    port: 5672
    username: guest
    password: guest
    virtualHost: /
    publisher-confirm-type: CORRELATED
    publisher-returns: true
```

## 核心用法

### 发送消息

```java
@Autowired
private RabbitMqMessageSender messageSender;

// 发送普通消息
RabbitMqMessage message = new RabbitMqMessage();
message.setExchange("order.exchange");
message.setRoutingKey("order.created");
message.setBody(orderDTO);
messageSender.send(message);

// 发送延时消息（30 秒后投递）
messageSender.sendDelay(message, 30000);
```

### 消费消息

```java
@Component
public class OrderConsumer extends AbstractRabbitConsumerListener<OrderDTO> {

    @Override
    @RabbitListener(queues = "order.queue")
    public void onMessage(Message message, Channel channel) throws IOException {
        super.onMessage(message, channel);
    }

    @Override
    protected void handleMessage(OrderDTO orderDTO, Message message, Channel channel) throws IOException {
        // 处理业务逻辑
        orderService.processOrder(orderDTO);
        // 手动 ACK
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
```

### 延时队列配置

```java
@Configuration
public class DelayQueueConfig {

    // 死信交换机
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange("order.dlx.exchange");
    }

    // 延时队列（消息过期后转发到死信交换机）
    @Bean
    public Queue delayQueue() {
        return QueueBuilder.durable("order.delay.queue")
            .withArgument("x-dead-letter-exchange", "order.dlx.exchange")
            .withArgument("x-dead-letter-routing-key", "order.timeout")
            .build();
    }

    // 实际处理队列
    @Bean
    public Queue orderTimeoutQueue() {
        return new Queue("order.timeout.queue");
    }
}
```

## 常见问题

**Q: 消息发送后没有收到确认回调？**

A: 确认 `publisher-confirm-type: CORRELATED` 已配置，且 `RabbitTemplate` 设置了 `ConfirmCallback`。

**Q: 消费者重复消费同一条消息？**

A: 使用 `RabbitMessageCacheMgr` 做幂等校验，以消息 ID 为 key 判断是否已处理。

**Q: 延时队列消息没有按时投递？**

A: 检查死信交换机和死信队列的绑定关系，确保 `x-dead-letter-exchange` 和 `x-dead-letter-routing-key` 与实际交换机/路由键一致。
