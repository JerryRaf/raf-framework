# raf-example-rabbit-starter

> 演示 `raf-framework-rabbit-starter` 的核心能力：配置驱动拓扑声明、普通消息发送/消费、延时消息（DLX+TTL）、手动 ACK、发送确认、幂等性控制。

## 功能概述

`raf-framework-rabbit-starter` 提供：

- **配置驱动拓扑**：Exchange/Queue/Binding 通过 `raf.rabbit.bindings` 配置声明，生产者服务启动时框架自动创建，无需在 Java 代码中写 `@Configuration` 声明 Bean
- **`@RabbitMqConsumer` 简化**：消费者只需指定 `queue` 和 `ackModel`，不再需要在注解上声明 exchange/routingKey
- **手动 ACK**：业务处理成功后框架自动 ACK，失败时触发 `onFailure` 回调并 reject（可选进死信队列）
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
    addresses: ${RABBIT_HOST:127.0.0.1}:${RABBIT_PORT:5672}
    username: ${RABBIT_USERNAME:guest}
    password: ${RABBIT_PASSWORD:guest}
    virtualHost: /
    provider:
      ack: false          # 是否启用 publisher confirm/return，默认 false
    consumer:
      group: order-service
      concurrentConsumers: 3
      maxConcurrentConsumers: 10

    # 拓扑声明（生产者服务配置，消费者服务不需要此项）
    bindings:
      # 普通队列
      - name: order-notify
        exchange: order.notify.exchange
        exchangeType: direct
        queue: order.notify.queue
        routingKey: order.notify
        durable: true

      # 延迟队列（消息级 TTL，每条消息可指定不同延迟时间）
      - name: order-timeout
        exchange: order.timeout.exchange
        exchangeType: direct
        queue: order.timeout.queue
        routingKey: order.timeout
        durable: true
        delay:
          deadExchange: order.timeout.dead.exchange
          deadExchangeType: direct
          deadQueue: order.timeout.dead.queue
          deadRoutingKey: order.timeout.dead
          ttl: 0          # 0 表示使用消息级 TTL（sendDelay 时传入 second 参数）

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
| `raf.rabbit.virtualHost` | string | `/` | 虚拟主机 |

### 生产者配置

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `raf.rabbit.provider.ack` | boolean | `false` | 是否启用 publisher confirm + mandatory return。开启后需实现 `AbstractRabbitSenderConfirm` |

### 消费者配置

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `raf.rabbit.consumer.group` | string | `DEFAULT_RABBIT_GROUP` | 消费者组名 |
| `raf.rabbit.consumer.concurrentConsumers` | int | `3` | 初始并发消费者数 |
| `raf.rabbit.consumer.maxConcurrentConsumers` | int | `10` | 最大并发消费者数 |

### SSL 配置

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `raf.rabbit.ssl.enabled` | boolean | `false` | 是否启用 SSL/TLS |
| `raf.rabbit.ssl.keyStore` | string | — | KeyStore 路径 |
| `raf.rabbit.ssl.keyStorePassword` | string | — | KeyStore 密码 |
| `raf.rabbit.ssl.trustStore` | string | — | TrustStore 路径 |
| `raf.rabbit.ssl.trustStorePassword` | string | — | TrustStore 密码 |
| `raf.rabbit.ssl.algorithm` | string | — | SSL 算法，如 `TLSv1.2` |

### 拓扑声明（`raf.rabbit.bindings[]`）

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `bindings[].name` | string | — | 逻辑名称，同一服务内不能重复，用于日志和 Bean 命名 |
| `bindings[].exchange` | string | — | Exchange 名称 |
| `bindings[].exchangeType` | string | `direct` | Exchange 类型：`direct` / `topic` / `fanout` / `headers` |
| `bindings[].queue` | string | — | 队列名称 |
| `bindings[].routingKey` | string | `""` | Routing Key，`fanout` 类型时忽略 |
| `bindings[].durable` | boolean | `true` | 是否持久化 |
| `bindings[].arguments` | map | `{}` | 队列自定义参数，如 `x-max-priority: 10` |
| `bindings[].delay` | object | `null` | 延迟队列配置，不为 null 时框架额外声明 DLX 拓扑 |

### 延迟队列配置（`raf.rabbit.bindings[].delay`）

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `delay.deadExchange` | string | — | 死信 Exchange 名称（消息先投递到此） |
| `delay.deadExchangeType` | string | `direct` | 死信 Exchange 类型 |
| `delay.deadQueue` | string | — | 死信队列名称（设置了 TTL 的等待队列） |
| `delay.deadRoutingKey` | string | `""` | 死信队列 routing key |
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
        routingKey: order.notify
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
      concurrentConsumers: 5
      maxConcurrentConsumers: 20
```

### 2. 发送普通消息

注入 `RabbitMqMessageSender`，调用 `send(message, exchange, routingKey)`：

```java
@Service
@RequiredArgsConstructor
public class OrderNotifyProducer {

    private final RabbitMqMessageSender sender;

    public void sendOrderNotify(String orderId, String content) {
        RabbitMqMessage message = new RabbitMqMessage();
        message.setMessage(content);
        // msgId 不设置时框架自动生成雪花 ID
        sender.send(message, "order.notify.exchange", "order.notify");
    }
}
```

也可以使用字符串重载，直接传入 JSON 字符串：

```java
sender.send(null, "{\"orderId\":\"123\"}", "order.notify.exchange", "order.notify");
```

### 3. 发送延迟消息

`sendDelay` 签名为 `sendDelay(message, exchange, routingKey, second)`，其中：
- `exchange`：死信 Exchange 名称（对应 `bindings[].delay.dead-exchange`）
- `routingKey`：死信队列 routing key（对应 `bindings[].delay.dead-routing-key`）
- `second`：消息级延迟秒数；若已在 `bindings` 中配置了队列级 TTL，此处传 `0`

```java
@Service
@RequiredArgsConstructor
public class OrderDelayProducer {

    private final RabbitMqMessageSender sender;

    /**
     * 发送延迟消息，30 分钟后触发订单超时检查。
     * 使用消息级 TTL（bindings[].delay.ttl = 0）。
     */
    public void sendOrderTimeout(String orderId) {
        RabbitMqMessage message = new RabbitMqMessage();
        message.setMessage(orderId);
        // 投递到死信 Exchange，30 分钟后由 DLX 转发到 order.timeout.queue
        sender.sendDelay(message, "order.timeout.dead.exchange", "order.timeout.dead", 1800);
    }

    /**
     * 使用队列级 TTL 时，second 传 0（延迟时间由 bindings[].delay.ttl 决定）。
     */
    public void sendWithQueueTtl(String orderId) {
        RabbitMqMessage message = new RabbitMqMessage();
        message.setMessage(orderId);
        sender.sendDelay(message, "order.timeout.dead.exchange", "order.timeout.dead", 0);
    }
}
```

### 4. 消费普通消息

继承 `AbstractRabbitConsumerListener`，在类上加 `@RabbitMqConsumer(queue = "队列名")`：

```java
@Component
@RabbitMqConsumer(queue = "order.notify.queue")
public class OrderNotifyConsumer extends AbstractRabbitConsumerListener {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public void onMessage(RabbitMqMessage message) throws Exception {
        String msgId = message.getMsgId();

        // 幂等性控制：同一 msgId 只处理一次
        Boolean isNew = redisTemplate.opsForValue()
                .setIfAbsent("mq:consumed:" + msgId, "1", 24, TimeUnit.HOURS);
        if (Boolean.FALSE.equals(isNew)) {
            log.info("Duplicate message, skip. msgId={}", msgId);
            return;
        }

        // 业务处理
        String content = message.getMessage();
        log.info("Processing order notify, msgId={}, content={}", msgId, content);
        // ... 业务逻辑
    }

    @Override
    public void onFailure(Message message, String error) throws Exception {
        // 最终失败处理：持久化到 DB，人工补偿
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        log.error("Order notify consume failed, body={}, error={}", body, error);
        // failedMessageRepo.save(body, error);
    }

    /**
     * 按需重写：是否重试。默认不重试（false）。
     * 返回 true 时消息重新入队（basicNack requeue=true），注意防止无限循环。
     */
    @Override
    public boolean retry(RabbitMqMessage message) {
        // 最多重试 3 次
        return !message.isOverTimes();
    }
}
```

### 5. 消费延迟消息

延迟消息最终投递到 receive queue（`bindings[].queue`），消费方式与普通消息完全一致：

```java
@Component
@RabbitMqConsumer(queue = "${mq.order.timeout.queue:order.timeout.queue}")
public class OrderTimeoutConsumer extends AbstractRabbitConsumerListener {

    @Override
    public void onMessage(RabbitMqMessage message) throws Exception {
        String orderId = message.getMessage();
        log.info("Order timeout triggered, orderId={}", orderId);
        // 检查订单状态，若仍未支付则关闭订单
    }

    @Override
    public void onFailure(Message message, String error) throws Exception {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        log.error("Order timeout consume failed, body={}, error={}", body, error);
    }
}
```

`queue` 属性支持 `${...}` 占位符，便于多环境（dev/test/prod）使用不同队列名：

```yaml
# application-dev.yml
mq:
  order:
    timeout:
      queue: order.timeout.queue.dev
```

### 6. 发送确认（`AbstractRabbitSenderConfirm`）

当 `raf.rabbit.provider.ack=true` 时，框架自动将实现类注册为 RabbitTemplate 的回调。继承 `AbstractRabbitSenderConfirm` 并实现 `giveUp`，按需重写 `onConfirmFail`：

```java
@Component
public class OrderSenderConfirm extends AbstractRabbitSenderConfirm {

    @Autowired
    private FailedMessageRepository failedMessageRepo;

    /**
     * 消息到达 Exchange 但无法路由到任何 Queue 时触发（mandatory return）。
     * 必须实现：将失败消息持久化到 DB，以便人工补偿或重发。
     */
    @Override
    public void giveUp(RabbitMqMessage message, Integer replyCode,
                       String replyText, String exchange, String routingKey) {
        log.error("Message unroutable, exchange={}, routingKey={}, replyCode={}, replyText={}",
                exchange, routingKey, replyCode, replyText);
        if (message != null) {
            failedMessageRepo.save(FailedMessage.of(message, exchange, routingKey, replyText));
        }
    }

    /**
     * 消息未到达 Exchange 时触发（网络问题、Broker 不可用等）。
     * 默认空实现，按需重写做告警或重发。
     */
    @Override
    protected void onConfirmFail(String msgId, String cause) {
        log.error("Message failed to reach exchange, msgId={}, cause={}", msgId, cause);
        // 可选：触发告警或将 msgId 加入重发队列
    }
}
```

两个回调的触发场景区别：

| 回调 | 触发时机 | 是否有消息体 |
|---|---|---|
| `onConfirmFail(msgId, cause)` | 消息未到达 Exchange（网络/Broker 问题） | 无（只有 msgId） |
| `giveUp(message, ...)` | 消息到达 Exchange 但无法路由到 Queue（routing key 错误等） | 有 |

### 7. 幂等性控制

消费者必须实现幂等，防止消息重复消费（at-least-once 语义）。推荐使用 Redis `setIfAbsent`：

```java
@Service
@RequiredArgsConstructor
public class IdempotentService {

    private final StringRedisTemplate redisTemplate;
    private static final String KEY_PREFIX = "mq:consumed:";
    private static final long EXPIRE_HOURS = 24;

    /**
     * 检查消息是否已处理。
     * @return true 表示首次处理（可继续），false 表示重复消息（应跳过）
     */
    public boolean tryConsume(String msgId) {
        Boolean isNew = redisTemplate.opsForValue()
                .setIfAbsent(KEY_PREFIX + msgId, "1", EXPIRE_HOURS, TimeUnit.HOURS);
        return Boolean.TRUE.equals(isNew);
    }
}
```

在消费者中使用：

```java
@Override
public void onMessage(RabbitMqMessage message) throws Exception {
    if (!idempotentService.tryConsume(message.getMsgId())) {
        log.info("Duplicate message ignored, msgId={}", message.getMsgId());
        return;
    }
    // 执行业务逻辑
}
```

## 延时队列工作原理

延时队列基于 RabbitMQ 的死信交换机（DLX）和 TTL 机制实现，无需安装额外插件：

```
生产者
  │
  │  sendDelay(msg, deadExchange, deadRoutingKey, second)
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
           消费者
```

**TTL 两种模式：**

- **队列级 TTL**（`bindings[].delay.ttl > 0`）：所有消息等待相同时长，适合固定延迟场景（如 30 分钟超时）
- **消息级 TTL**（`bindings[].delay.ttl = 0`，`sendDelay` 传 `second > 0`）：每条消息可指定不同延迟时间，适合动态延迟场景

## 最佳实践

1. **拓扑声明只在生产者服务配置**：消费者服务不需要配置 `bindings`，避免重复声明导致配置冲突
2. **消息幂等**：消费逻辑必须幂等，使用 `msgId` + Redis `setIfAbsent` 做去重，防止重复消费
3. **`provider.ack` 默认关闭**：仅在对消息可靠性要求极高的场景开启，开启后必须实现 `AbstractRabbitSenderConfirm.giveUp` 持久化失败消息
4. **发送失败处理**：发送失败会抛出 `InfrastructureException`，业务侧应让事务回滚或进入本地补偿
5. **DLX 配置**：生产环境为每个业务队列配置死信队列（`bindings[].delay`），避免消费失败的消息丢失
6. **消费者并发**：根据业务量调整 `concurrentConsumers` 和 `maxConcurrentConsumers`，避免消费积压
7. **消息大小**：单条消息建议不超过 1MB，大数据通过 ID 引用，消费者再查询
8. **密码加密**：RabbitMQ 密码使用 Jasypt 加密，格式 `ENC(加密后的密文)`
9. **重试防无限循环**：重写 `retry()` 时配合 `message.isOverTimes()` 限制重试次数（默认上限 3 次）

## 常见问题

**Q: 消息发送后消费者没有收到？**

A: 检查以下几点：
- `raf.rabbit.enabled=true` 是否已配置
- 生产者服务的 `bindings` 中 exchange/queue/routingKey 是否与消费者 `@RabbitMqConsumer(queue=...)` 一致
- 生产者服务是否已启动并完成拓扑声明（查看启动日志）
- RabbitMQ Management UI（`http://localhost:15672`）中确认 Exchange 和 Queue 是否存在

**Q: 延时消息没有在预期时间触发？**

A: 检查以下几点：
- `bindings[].delay.ttl` 单位是毫秒，`sendDelay` 的 `second` 参数单位是秒，注意区分
- 若同时配置了队列级 TTL 和消息级 TTL，RabbitMQ 取两者中较小值
- 死信队列的 `x-dead-letter-exchange` 和 `x-dead-letter-routing-key` 是否正确指向 receive exchange

**Q: 消费者重启后消息重复消费？**

A: 这是正常行为（at-least-once 语义）。确保消费逻辑幂等，使用 `msgId` 做去重。

**Q: `provider.ack=true` 后 `giveUp` 没有被调用？**

A: `giveUp` 对应 mandatory return 回调，仅在消息到达 Exchange 但无法路由到 Queue 时触发。若消息根本没到达 Exchange（网络问题），触发的是 `onConfirmFail`。确认 Exchange 和 Queue 的绑定关系正确。

**Q: 如何监控消息积压？**

A: 通过 RabbitMQ Management UI（默认 `http://localhost:15672`）查看队列深度，或集成 Prometheus + Grafana 监控（需安装 `rabbitmq_prometheus` 插件）。
