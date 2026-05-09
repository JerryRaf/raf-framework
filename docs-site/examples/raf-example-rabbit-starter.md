# raf-example-rabbit-starter

> 演示 `raf-framework-rabbit-starter` 的核心能力：配置驱动拓扑声明、普通消息发送/消费、延时消息（DLX+TTL）、手动 ACK、发送确认、幂等性控制。

## 功能概述

`raf-framework-rabbit-starter` 提供：

- **配置驱动拓扑**：Exchange/Queue/Binding 通过 `raf.rabbit.bindings` 配置声明，生产者服务启动时框架自动创建，无需在 Java 代码中写 `@Configuration` 声明 Bean
- **`@RabbitMqConsumer` 简化**：消费者只需指定 `queue` 名，支持 `${...}` 占位符，支持注解级并发参数覆盖全局配置
- **手动 ACK**：业务处理成功后框架自动 ACK；失败时按 `maxRetryTimes()` 重试，超限后调用 `onFailure` 并 reject
- **重试机制**：重试计数持久化在消息体中（`times` 字段），重新发布而非 basicNack requeue，彻底避免无限循环
- **延时消息**：基于死信交换机（DLX）和 TTL 实现延时队列，支持队列级 TTL 和消息级 TTL 两种模式
- **发送确认**：`AbstractRabbitSenderConfirm` 提供 `onConfirmFail`（未到达 Exchange）和 `giveUp`（无法路由到 Queue）两个回调钩子
- **Provider/Consumer 分离**：生产者配置 `bindings` 声明拓扑，消费者只需配置连接信息和并发参数

## 快速接入

### 1. 引入依赖

```xml
<dependency>
    <groupId>io.github.jerryraf</groupId>
    <artifactId>raf-framework-rabbit-starter</artifactId>
</dependency>
```

### 2. 配置（`application.yml`）

以下示例同时包含普通队列和延迟队列的完整配置：

```yaml
spring:
  application:
    name: raf-example-rabbit-starter

raf:
  rabbit:
    enabled: true
    addresses: ${RABBIT_ADDR:127.0.0.1:5672}
    username: ${RABBIT_USER:guest}
    password: ${RABBIT_PASS:guest}
    virtual-host: /

    provider:
      ack: false          # 是否启用 publisher confirm/return，默认 false

    consumer:
      concurrent-consumers: 3
      max-concurrent-consumers: 10

    # 拓扑声明（生产者服务配置，消费者服务不需要此项）
    bindings:
      # 普通队列：订单通知
      - name: order-notify
        exchange: order.notify.exchange
        exchange-type: direct
        queue: order.notify.queue
        routing-key: order.notify
        durable: true

      # 延迟队列：订单超时（消息级 TTL，每条消息可指定不同延迟时间）
      - name: order-timeout
        exchange: order.timeout.exchange
        exchange-type: direct
        queue: order.timeout.queue
        routing-key: order.timeout
        durable: true
        delay:
          dead-exchange: order.timeout.dead.exchange
          dead-exchange-type: direct
          dead-queue: order.timeout.dead.queue
          dead-routing-key: order.timeout.dead
          ttl: 0   # 0 = 消息级 TTL；> 0 = 队列级 TTL（毫秒）

  redis:
    enabled: true
    host: ${REDIS_HOST:127.0.0.1}
    port: ${REDIS_PORT:6379}
```

## 配置项详解

### 基础连接配置

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `raf.rabbit.enabled` | boolean | `false` | 是否启用 RabbitMQ |
| `raf.rabbit.addresses` | string | — | RabbitMQ 地址，集群用逗号分隔，如 `host1:5672,host2:5672` |
| `raf.rabbit.username` | string | — | 用户名 |
| `raf.rabbit.password` | string | — | 密码（支持 Jasypt 加密，格式 `ENC(密文)`） |
| `raf.rabbit.virtual-host` | string | `/` | 虚拟主机 |

### 生产者配置

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `raf.rabbit.provider.ack` | boolean | `false` | 是否启用 publisher confirm + mandatory return。开启后需实现 `AbstractRabbitSenderConfirm` |

### 消费者配置

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `raf.rabbit.consumer.concurrent-consumers` | int | `3` | 初始并发消费者数（全局默认，可被 `@RabbitMqConsumer` 注解覆盖） |
| `raf.rabbit.consumer.max-concurrent-consumers` | int | `10` | 最大并发消费者数（全局默认，可被 `@RabbitMqConsumer` 注解覆盖） |

### SSL 配置

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `raf.rabbit.ssl.enabled` | boolean | `false` | 是否启用 SSL/TLS |
| `raf.rabbit.ssl.key-store` | string | — | KeyStore 路径 |
| `raf.rabbit.ssl.key-store-password` | string | — | KeyStore 密码 |
| `raf.rabbit.ssl.trust-store` | string | — | TrustStore 路径 |
| `raf.rabbit.ssl.trust-store-password` | string | — | TrustStore 密码 |
| `raf.rabbit.ssl.algorithm` | string | `TLSv1.2` | SSL 协议版本 |

### 拓扑声明（`raf.rabbit.bindings[]`）

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `bindings[].name` | string | — | 逻辑名称，同一服务内不能重复，用于日志和 Bean 命名 |
| `bindings[].exchange` | string | — | Exchange 名称 |
| `bindings[].exchange-type` | string | `direct` | Exchange 类型：`direct` / `topic` / `fanout` / `headers` |
| `bindings[].queue` | string | — | 队列名称 |
| `bindings[].routing-key` | string | `""` | Routing Key，`fanout` 类型时忽略 |
| `bindings[].durable` | boolean | `true` | 是否持久化 |
| `bindings[].arguments` | map | `{}` | 队列自定义参数，如 `x-max-priority: 10` |
| `bindings[].delay` | object | `null` | 延迟队列配置，不为 null 时框架额外声明 DLX 拓扑 |

### 延迟队列配置（`raf.rabbit.bindings[].delay`）

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `delay.dead-exchange` | string | — | 死信 Exchange 名称（消息先投递到此） |
| `delay.dead-exchange-type` | string | `direct` | 死信 Exchange 类型 |
| `delay.dead-queue` | string | — | 死信队列名称（设置了 TTL 的等待队列） |
| `delay.dead-routing-key` | string | `""` | 死信队列 routing key |
| `delay.ttl` | long | `0` | 队列级 TTL（毫秒）。`> 0` 时在死信队列设置 `x-message-ttl`；`= 0` 时使用消息级 TTL（`sendDelay` 的 `second` 参数） |

## 核心用法

### 1. 配置驱动拓扑声明

拓扑声明完全由 `application.yml` 驱动，**不需要**在 Java 代码中声明 Exchange/Queue/Binding Bean。生产者服务在启动时，框架会根据 `raf.rabbit.bindings` 自动创建所有 Exchange、Queue 和 Binding。

消费者服务只需配置连接信息和并发参数，**不需要**配置 `bindings`。

```yaml
# 生产者服务 application.yml
raf:
  rabbit:
    enabled: true
    addresses: 127.0.0.1:5672
    username: guest
    password: guest
    bindings:
      - name: order-notify
        exchange: order.notify.exchange
        queue: order.notify.queue
        routing-key: order.notify
```

```yaml
# 消费者服务 application.yml（无需 bindings）
raf:
  rabbit:
    enabled: true
    addresses: 127.0.0.1:5672
    username: guest
    password: guest
    consumer:
      concurrent-consumers: 5
      max-concurrent-consumers: 20
```

### 2. 发送普通消息

注入 `RabbitMqMessageSender`，调用 `send(message, exchange, routingKey)`：

```java
@Component
@RequiredArgsConstructor
public class OrderNotifyProducer {

    private static final String EXCHANGE    = "order.notify.exchange";
    private static final String ROUTING_KEY = "order.notify";

    private final RabbitMqMessageSender sender;
    private final JsonService jsonService;

    public void send(OrderMessage order) {
        RabbitMqMessage msg = new RabbitMqMessage();
        msg.setMsgId(SnowFlakeBuilder.generateId()); // 不设置时框架自动生成雪花 ID
        msg.setMessage(jsonService.toJson(order));
        sender.send(msg, EXCHANGE, ROUTING_KEY);
    }
}
```

### 3. 发送延迟消息

`sendDelay(message, exchange, routingKey, second)` 中：
- `exchange`：**死信 Exchange** 名称（对应 `bindings[].delay.dead-exchange`）
- `routingKey`：死信队列 routing key（对应 `bindings[].delay.dead-routing-key`）
- `second`：消息级延迟秒数；若已在 `bindings` 中配置了队列级 TTL，此处传 `0`

```java
@Component
@RequiredArgsConstructor
public class OrderDelayProducer {

    // 投递目标是死信 Exchange，不是 receive exchange
    private static final String DEAD_EXCHANGE    = "order.timeout.dead.exchange";
    private static final String DEAD_ROUTING_KEY = "order.timeout.dead";

    private final RabbitMqMessageSender sender;
    private final JsonService jsonService;

    /** 发送延迟消息，30 分钟后触发订单超时检查（消息级 TTL） */
    public void sendTimeout(OrderMessage order) {
        RabbitMqMessage msg = new RabbitMqMessage();
        msg.setMsgId(SnowFlakeBuilder.generateId());
        msg.setMessage(jsonService.toJson(order));
        sender.sendDelay(msg, DEAD_EXCHANGE, DEAD_ROUTING_KEY, 1800);
    }

    /** 使用队列级 TTL 时，second 传 0（延迟时间由 bindings[].delay.ttl 决定） */
    public void sendWithQueueTtl(OrderMessage order) {
        RabbitMqMessage msg = new RabbitMqMessage();
        msg.setMsgId(SnowFlakeBuilder.generateId());
        msg.setMessage(jsonService.toJson(order));
        sender.sendDelay(msg, DEAD_EXCHANGE, DEAD_ROUTING_KEY, 0);
    }
}
```

### 4. 消费普通消息

继承 `AbstractRabbitConsumerListener`，在类上加 `@RabbitMqConsumer(queue = "队列名")`：

```java
@Slf4j
@Component
@RabbitMqConsumer(queue = "order.notify.queue")
@RequiredArgsConstructor
public class OrderNotifyConsumer extends AbstractRabbitConsumerListener {

    private final IdempotentService idempotentService;

    @Autowired
    private JsonService jsonService;  // 基类已注入，子类按需使用

    @Override
    public void onMessage(RabbitMqMessage message) throws Exception {
        String msgId = message.getMsgId();

        // 幂等性控制：同一 msgId 只处理一次
        if (!idempotentService.tryMark(msgId)) {
            log.warn("Duplicate message, skipping: msgId={}", msgId);
            return;
        }

        OrderMessage order = jsonService.parse(message.getMessage(), OrderMessage.class);
        // 业务处理...
        log.info("Order notify processed: orderId={}", order.getOrderId());
    }

    @Override
    public void onFailure(Message message, String error) {
        // 超过最大重试次数后持久化到 DB，人工补偿
        log.error("Order notify permanently failed, manual intervention required. error={}", error);
    }

    /**
     * 最大重试次数，默认 3 次。子类可重写。
     * 重试时框架先 ACK 当前消息，再将 times+1 的消息重新发布到原队列，
     * 确保重试计数持久化，不会因重新入队而丢失。
     */
    @Override
    public int maxRetryTimes() {
        return 3;
    }
}
```

### 5. 消费延迟消息

延迟消费者监听的是 **receive queue**（`bindings[].queue`），不是 dead queue。写法与普通消费者完全一致：

```java
@Slf4j
@Component
@RabbitMqConsumer(queue = "order.timeout.queue")  // receive queue，不是 dead queue
@RequiredArgsConstructor
public class OrderTimeoutConsumer extends AbstractRabbitConsumerListener {

    private final IdempotentService idempotentService;

    @Autowired
    private JsonService jsonService;

    @Override
    public void onMessage(RabbitMqMessage message) throws Exception {
        String msgId = message.getMsgId();
        if (!idempotentService.tryMark(msgId)) {
            return;
        }
        OrderMessage order = jsonService.parse(message.getMessage(), OrderMessage.class);
        // 执行超时取消逻辑...
        log.info("Order {} cancelled due to payment timeout", order.getOrderId());
    }

    @Override
    public void onFailure(Message message, String error) {
        log.error("Order timeout processing permanently failed. error={}", error);
    }
}
```

### 6. 注解级并发参数（按队列独立配置）

高优先级队列可在注解上覆盖全局并发配置，无需修改全局参数：

```java
// 高优先级队列：支付回调，需要更多消费者
@RabbitMqConsumer(
    queue = "payment.callback.queue",
    concurrentConsumers = 5,
    maxConcurrentConsumers = 20
)
public class PaymentCallbackConsumer extends AbstractRabbitConsumerListener { ... }

// 低优先级队列：日志归档，1 个消费者即可
@RabbitMqConsumer(
    queue = "log.archive.queue",
    concurrentConsumers = 1,
    maxConcurrentConsumers = 2
)
public class LogArchiveConsumer extends AbstractRabbitConsumerListener { ... }
```

注解值为 `-1`（默认）时回退到 `raf.rabbit.consumer` 全局配置。

### 7. 发送确认（publisher confirm）

当 `raf.rabbit.provider.ack=true` 时，需实现 `AbstractRabbitSenderConfirm`：

```java
@Component
public class OrderSenderConfirm extends AbstractRabbitSenderConfirm {

    @Autowired
    private FailedMessageRepository failedMessageRepo;

    /**
     * 消息到达 Exchange 但无法路由到 Queue 时触发（mandatory return）。
     * 必须实现：将失败消息持久化到 DB，人工补偿。
     */
    @Override
    public void giveUp(RabbitMqMessage message, Integer replyCode,
                       String replyText, String exchange, String routingKey) {
        log.error("Message unroutable: exchange={}, routingKey={}, replyCode={}, replyText={}",
                exchange, routingKey, replyCode, replyText);
        failedMessageRepo.save(FailedMessage.of(message, exchange, routingKey, replyText));
    }

    /**
     * 消息未到达 Exchange 时触发（网络问题等）。
     * 默认只打日志，可重写做告警或重发。
     */
    @Override
    protected void onConfirmFail(String msgId, String cause) {
        log.error("Message failed to reach exchange: msgId={}, cause={}", msgId, cause);
        // 可选：触发重发或告警
    }
}
```

## 延迟队列原理

```
生产者
  │
  │ sendDelay(msg, deadExchange, deadRoutingKey, second)
  ▼
┌─────────────────────────────┐
│  Dead Exchange               │  order.timeout.dead.exchange
│  (死信 Exchange)             │
└─────────────┬───────────────┘
              │ deadRoutingKey
              ▼
┌─────────────────────────────┐
│  Dead Queue                  │  order.timeout.dead.queue
│  (等待队列，设置了 TTL)       │
│                              │
│  x-dead-letter-exchange      │──► order.timeout.exchange
│  x-dead-letter-routing-key  │──► order.timeout
│  x-message-ttl (可选)        │
└─────────────────────────────┘
              │
              │ TTL 到期，消息转发
              ▼
┌─────────────────────────────┐
│  Receive Exchange            │  order.timeout.exchange
│  (接收 Exchange)             │
└─────────────┬───────────────┘
              │ routingKey
              ▼
┌─────────────────────────────┐
│  Receive Queue               │  order.timeout.queue
│  (业务消费队列)               │
└─────────────────────────────┘
              │
              ▼
           消费者（@RabbitMqConsumer(queue = "order.timeout.queue")）
```

**TTL 两种模式：**

- **队列级 TTL**（`bindings[].delay.ttl > 0`）：所有消息等待相同时长，适合固定延迟场景（如 30 分钟超时）
- **消息级 TTL**（`bindings[].delay.ttl = 0`，`sendDelay` 传 `second > 0`）：每条消息可指定不同延迟时间，适合动态延迟场景

两者同时存在时，RabbitMQ 取较小值。

## 重试机制

```
onMessage(RabbitMqMessage)
  ├── 成功 → basicAck
  └── 异常
        ├── times < maxRetryTimes() → basicAck + 重新发布（times+1 写入消息体）
        └── times >= maxRetryTimes() → onFailure() → basicReject(requeue=false)
```

重试计数（`times` 字段）持久化在消息体中，重新发布而非 `basicNack(requeue=true)`，确保计数不会因重新入队而丢失。

## 最佳实践

1. **拓扑声明只在生产者服务配置**：消费者服务不需要配置 `bindings`，避免重复声明导致配置冲突
2. **消息幂等**：消费逻辑必须幂等，使用 `msgId` + Redis `setIfAbsent` 做去重，防止重复消费
3. **`provider.ack` 默认关闭**：仅在对消息可靠性要求极高的场景开启，开启后必须实现 `AbstractRabbitSenderConfirm.giveUp` 持久化失败消息
4. **发送失败处理**：发送失败会抛出 `InfrastructureException`，业务侧应让事务回滚或进入本地补偿
5. **DLX 配置**：生产环境为每个业务队列配置死信队列（`bindings[].delay`），避免消费失败的消息丢失
6. **消费者并发**：高优先级队列通过 `@RabbitMqConsumer(concurrentConsumers=N)` 单独配置，低优先级队列使用全局默认值
7. **消息大小**：单条消息建议不超过 1MB，大数据通过 ID 引用，消费者再查询
8. **密码加密**：RabbitMQ 密码使用 Jasypt 加密，格式 `ENC(加密后的密文)`

## 常见问题

**Q: 消息发送后消费者没有收到？**

A: 检查以下几点：
- `raf.rabbit.enabled=true` 是否已配置
- 生产者服务的 `bindings` 中 exchange/queue/routing-key 是否与消费者 `@RabbitMqConsumer(queue=...)` 一致
- 生产者服务是否已启动并完成拓扑声明（查看启动日志中的 `Declared topology` 输出）
- RabbitMQ Management UI（`http://localhost:15672`）中确认 Exchange 和 Queue 是否存在

**Q: 延时消息没有在预期时间触发？**

A: 检查以下几点：
- `bindings[].delay.ttl` 单位是毫秒，`sendDelay` 的 `second` 参数单位是秒，注意区分
- 若同时配置了队列级 TTL 和消息级 TTL，RabbitMQ 取两者中较小值
- 死信队列的 `x-dead-letter-exchange` 和 `x-dead-letter-routing-key` 是否正确指向 receive exchange（框架自动配置，检查 `bindings[].delay` 中的 exchange/routing-key 是否与 `bindings[].exchange`/`routing-key` 对应）

**Q: 消费者重启后消息重复消费？**

A: 这是正常行为（at-least-once 语义）。确保消费逻辑幂等，使用 `msgId` 做去重。

**Q: `provider.ack=true` 后 `giveUp` 没有被调用？**

A: `giveUp` 对应 mandatory return 回调，仅在消息到达 Exchange 但无法路由到 Queue 时触发。若消息根本没到达 Exchange（网络问题），触发的是 `onConfirmFail`。确认 Exchange 和 Queue 的绑定关系正确。

**Q: 如何监控消息积压？**

A: 通过 RabbitMQ Management UI（默认 `http://localhost:15672`）查看队列深度，或集成 Prometheus + Grafana 监控（需安装 `rabbitmq_prometheus` 插件）。

**Q: 消费者抛出异常后消息去哪了？**

A: 取决于重试次数：
- `times < maxRetryTimes()`：消息被重新发布到原队列，`times` 递增
- `times >= maxRetryTimes()`：调用 `onFailure`，然后 `basicReject(requeue=false)`，消息进入该队列的死信队列（如果配置了 DLX）或被丢弃
