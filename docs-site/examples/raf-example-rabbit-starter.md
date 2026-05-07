# raf-example-rabbit-starter

> 演示 `raf-framework-rabbit-starter` 的核心能力：普通消息发送/消费、延时消息（DLX+TTL）、手动 ACK、幂等性控制。

## 功能概述

`raf-framework-rabbit-starter` 提供：

- **Provider/Consumer 分离**：通过 `raf.rabbit.provider`、`raf.rabbit.consumer` 分别启用发送端和消费端
- **自动声明交换机/队列/绑定**：消费端使用 `@RabbitMqConsumer` 后由框架注册监听容器
- **手动 ACK**：业务处理成功后框架 ACK，失败时进入 `onFailure` 和 reject 流程
- **延时消息**：基于死信交换机（DLX）和 TTL 实现延时队列
- **发送确认**：支持 `AbstractRabbitSenderConfirm`，发送失败抛出 `InfrastructureException`

## 快速接入

### 1. 引入依赖

```xml
<dependency>
    <groupId>io.github.jerryraf</groupId>
    <artifactId>raf-framework-rabbit-starter</artifactId>
</dependency>
```

### 2. 配置（`application.yml`）

```yaml
spring:
  application:
    name: raf-example-rabbit-starter

raf:
  rabbit:
    enabled: true
    addresses: ${RABBIT_HOST:127.0.0.1}:${RABBIT_PORT:5672}
    username: ${RABBIT_USERNAME:guest}
    password: ${RABBIT_PASSWORD:guest}
    virtualHost: /
    provider:
      ack: true
    consumer:
      group: order-service
      concurrentConsumers: 3
      maxConcurrentConsumers: 10
      
  redis:
    enabled: true
    host: ${REDIS_HOST:127.0.0.1}
    port: ${REDIS_PORT:6379}
```

## 配置项详解

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `raf.rabbit.enabled` | boolean | `false` | 是否启用 RabbitMQ |
| `raf.rabbit.addresses` | string | — | RabbitMQ 地址，集群用逗号分隔 |
| `raf.rabbit.username` | string | — | 用户名 |
| `raf.rabbit.password` | string | — | 密码 |
| `raf.rabbit.virtualHost` | string | `/` | 虚拟主机 |
| `raf.rabbit.provider.ack` | boolean | `true` | 是否启用 publisher confirm/return |
| `raf.rabbit.consumer.group` | string | `DEFAULT_RABBIT_GROUP` | 消费者组名 |
| `raf.rabbit.consumer.concurrentConsumers` | int | `3` | 初始并发消费者数 |
| `raf.rabbit.consumer.maxConcurrentConsumers` | int | `10` | 最大并发消费者数 |
| `raf.rabbit.security.strictTypeValidation` | boolean | `true` | 是否启用消息类型白名单校验 |
| `raf.rabbit.security.maxRetryCount` | int | `3` | 最大重试次数 |
| `raf.rabbit.ssl.enabled` | boolean | `false` | 是否启用 SSL |

## 核心用法

### 普通消息：交换机与队列声明

```java
@Configuration
public class RabbitNormalConfig {

    public static final String ORDER_NOTIFY_EXCHANGE = "order.notify.exchange";
    public static final String ORDER_NOTIFY_QUEUE = "order.notify.queue";
    public static final String ORDER_NOTIFY_ROUTING_KEY = "order.notify";

    @Bean
    public DirectExchange orderNotifyExchange() {
        return ExchangeBuilder.directExchange(ORDER_NOTIFY_EXCHANGE)
            .durable(true)
            .build();
    }

    @Bean
    public Queue orderNotifyQueue() {
        return QueueBuilder.durable(ORDER_NOTIFY_QUEUE).build();
    }

    @Bean
    public Binding orderNotifyBinding() {
        return BindingBuilder.bind(orderNotifyQueue())
            .to(orderNotifyExchange())
            .with(ORDER_NOTIFY_ROUTING_KEY);
    }
}
```

### 延时消息：DLX + TTL 模式

```java
@Configuration
public class RabbitDelayConfig {

    // 死信交换机（接收 TTL 过期的消息）
    public static final String ORDER_DELAY_DEAD_EXCHANGE = "order.delay.dead.exchange";
    // TTL 队列（消息在此等待，过期后转发到死信交换机）
    public static final String ORDER_DELAY_TTL_QUEUE = "order.delay.ttl.queue";
    // 实际处理交换机
    public static final String ORDER_DELAY_RECEIVE_EXCHANGE = "order.delay.receive.exchange";
    // 实际处理队列
    public static final String ORDER_DELAY_PROCESS_QUEUE = "order.delay.process.queue";

    @Bean
    public DirectExchange orderDelayDeadExchange() {
        return ExchangeBuilder.directExchange(ORDER_DELAY_DEAD_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue orderDelayTtlQueue() {
        return QueueBuilder.durable(ORDER_DELAY_TTL_QUEUE)
            .withArgument("x-dead-letter-exchange", ORDER_DELAY_DEAD_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", "order.timeout")
            .withArgument("x-message-ttl", 1800000)  // 30 分钟
            .build();
    }

    @Bean
    public DirectExchange orderDelayReceiveExchange() {
        return ExchangeBuilder.directExchange(ORDER_DELAY_RECEIVE_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue orderDelayProcessQueue() {
        return QueueBuilder.durable(ORDER_DELAY_PROCESS_QUEUE).build();
    }

    @Bean
    public Binding orderDelayDeadBinding() {
        return BindingBuilder.bind(orderDelayProcessQueue())
            .to(orderDelayReceiveExchange())
            .with("order.timeout");
    }
}
```

### 发送普通消息

```java
@Component
@RequiredArgsConstructor
public class OrderNotifyProducer {

    private final RabbitMqMessageSender sender;

    public void sendOrderNotify(String orderId, String content) {
        RabbitMqMessage message = new RabbitMqMessage();
        // 使用雪花算法生成唯一消息 ID，用于幂等性控制
        message.setMsgId(String.valueOf(SnowFlakeBuilder.generateId()));
        message.setMessage(content);

        sender.send(message, RabbitNormalConfig.ORDER_NOTIFY_EXCHANGE,
            RabbitNormalConfig.ORDER_NOTIFY_ROUTING_KEY);
        log.info("订单通知消息已发送，orderId={}", orderId);
    }
}
```

### 发送延时消息

```java
@Component
@RequiredArgsConstructor
public class OrderDelayProducer {

    private final RabbitMqMessageSender sender;

    /**
     * 发送订单超时检查消息（30 分钟后触发）
     */
    public void sendOrderTimeout(String orderId) {
        RabbitMqMessage message = new RabbitMqMessage();
        message.setMsgId(String.valueOf(SnowFlakeBuilder.generateId()));
        message.setMessage(orderId);

        // sendDelay 将消息发送到 TTL 队列，30 分钟后自动转发到处理队列
        sender.sendDelay(message, "order.timeout", 1800);
        log.info("订单超时检查消息已发送，orderId={}, delay=30min", orderId);
    }
}
```

### 消费普通消息（手动 ACK + 幂等）

```java
@RabbitMqConsumer(
    exchange = RabbitNormalConfig.ORDER_NOTIFY_EXCHANGE,
    routingKey = RabbitNormalConfig.ORDER_NOTIFY_ROUTING_KEY,
    queue = RabbitNormalConfig.ORDER_NOTIFY_QUEUE
)
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderNotifyConsumer extends AbstractRabbitConsumerListener {

    private final IdempotentService idempotentService;
    private final OrderService orderService;

    @Override
    public void onMessage(RabbitMqMessage message) {
        String msgId = message.getMsgId();

        // 幂等性检查：同一消息 ID 24 小时内只处理一次
        if (!idempotentService.tryProcess(msgId)) {
            log.warn("重复消息，跳过处理，msgId={}", msgId);
            return;
        }

        try {
            orderService.processNotify(message.getMessage());
            log.info("订单通知处理成功，msgId={}", msgId);
        } catch (Exception e) {
            // 处理失败时标记幂等 key 失效，允许重试
            idempotentService.markFailed(msgId);
            throw e;
        }
    }

    @Override
    public void onFailure(Message message, String error) {
        log.error("订单通知消费失败，error={}", error);
        // 记录到失败表，人工介入或定时补偿
    }
}
```

### 消费延时消息

```java
@RabbitMqDelayConsumer(businessName = "order.timeout")
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderTimeoutConsumer extends AbstractRabbitConsumerListener {

    private final OrderService orderService;

    @Override
    public void onMessage(RabbitMqMessage message) {
        String orderId = message.getMessage();
        log.info("处理订单超时，orderId={}", orderId);
        orderService.handleTimeout(orderId);
    }

    @Override
    public void onFailure(Message message, String error) {
        log.error("订单超时处理失败，error={}", error);
    }
}
```

### 幂等性服务

```java
@Service
@RequiredArgsConstructor
public class IdempotentService {

    private final RedisService redisService;

    private static final String IDEMPOTENT_PREFIX = "mq:idempotent:";
    private static final long EXPIRE_HOURS = 24;

    /**
     * 尝试处理：返回 true 表示首次处理，false 表示重复消息
     */
    public boolean tryProcess(String msgId) {
        String key = IDEMPOTENT_PREFIX + msgId;
        Boolean isFirst = redisService.setIfAbsent(key, "processing", EXPIRE_HOURS, TimeUnit.HOURS);
        return Boolean.TRUE.equals(isFirst);
    }

    /**
     * 标记处理失败，删除幂等 key 允许重试
     */
    public void markFailed(String msgId) {
        redisService.delete(IDEMPOTENT_PREFIX + msgId);
    }
}
```

## 延时队列工作原理

```
生产者
  │
  ▼
order.delay.ttl.queue（TTL=30min）
  │ 消息过期
  ▼
order.delay.dead.exchange（死信交换机）
  │ routing-key: order.timeout
  ▼
order.delay.receive.exchange
  │
  ▼
order.delay.process.queue
  │
  ▼
OrderTimeoutConsumer（消费者）
```

## 示例项目结构

```
raf-example-rabbit-starter/
├── src/main/java/io/github/jerryraf/examples/rabbit/
│   ├── RabbitExampleApplication.java
│   ├── config/
│   │   ├── RabbitNormalConfig.java     # 普通消息交换机/队列声明
│   │   └── RabbitDelayConfig.java      # 延时消息 DLX+TTL 配置
│   ├── producer/
│   │   ├── OrderNotifyProducer.java    # 普通消息生产者
│   │   └── OrderDelayProducer.java     # 延时消息生产者
│   ├── consumer/
│   │   ├── OrderNotifyConsumer.java    # 普通消息消费者（含幂等）
│   │   └── OrderTimeoutConsumer.java   # 延时消息消费者
│   ├── service/
│   │   └── IdempotentService.java      # 幂等性控制（Redis）
│   └── controller/
│       └── OrderController.java        # REST API 触发消息发送
└── src/main/resources/
    └── application.yml
```

## 最佳实践

1. **消息幂等**：消费逻辑必须幂等，使用 `msgId` + Redis `setIfAbsent` 做去重，防止重复消费
2. **发送失败处理**：发送失败会抛出 `InfrastructureException`，业务侧应让事务回滚或进入本地补偿
3. **DLX 配置**：生产环境为每个业务队列配置死信队列，避免消息丢失
4. **消费者并发**：根据业务量调整 `concurrentConsumers` 和 `maxConcurrentConsumers`
5. **消息大小**：单条消息建议不超过 1MB，大数据通过 ID 引用，消费者再查询
6. **密码加密**：RabbitMQ 密码使用 Jasypt 加密，格式 `ENC(加密后的密文)`

## 常见问题

**Q: 消息发送后消费者没有收到？**

A: 检查交换机名称、routing key、队列绑定是否一致。确认 `raf.rabbit.enabled=true` 且消费者 `@RabbitMqConsumer` 的 exchange/routingKey/queue 与声明的 Bean 一致。

**Q: 延时消息没有在预期时间触发？**

A: 检查 TTL 队列的 `x-message-ttl` 配置（单位毫秒）。注意 RabbitMQ 的 TTL 是队列级别的，消息到达队列后才开始计时。

**Q: 消费者重启后消息重复消费？**

A: 这是正常行为（at-least-once 语义）。确保消费逻辑幂等，使用 `msgId` 做去重。

**Q: 如何监控消息积压？**

A: 通过 RabbitMQ Management UI（默认 `http://localhost:15672`）查看队列深度，或集成 Prometheus + Grafana 监控。
