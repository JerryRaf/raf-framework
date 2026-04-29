# RabbitMQ 消息队列（rabbit-starter）

## 功能概述

`raf-framework-rabbit-starter` 提供 RabbitMQ 生产者、消费者、延时消息和发送确认封装。组件默认关闭，只有配置 `raf.rabbit.enabled=true` 后才会启用。

- **Provider/Consumer 分离**：通过 `raf.rabbit.provider`、`raf.rabbit.consumer` 分别启用发送端和消费端能力
- **自动声明交换机/队列/绑定**：消费端使用 `@RabbitMqConsumer` 后由框架注册监听容器
- **手动 ACK**：业务处理成功后框架 ACK，失败时进入 `onFailure` 和 reject 流程
- **延时消息**：基于死信交换机和 TTL 实现
- **发送确认**：支持 `AbstractRabbitSenderConfirm`，发送失败会进入缓存重试逻辑
- **安全加固**：SSL 配置失败会阻止启动；发送失败会抛出 `InfrastructureException`，调用方不会误判为成功

## 配置项

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `raf.rabbit.enabled` | boolean | `false` | 是否启用 RabbitMQ |
| `raf.rabbit.addresses` | string | — | RabbitMQ 地址，如 `127.0.0.1:5672` 或集群逗号分隔 |
| `raf.rabbit.username` | string | — | 用户名 |
| `raf.rabbit.password` | string | — | 密码 |
| `raf.rabbit.virtualHost` | string | `/` | 虚拟主机 |
| `raf.rabbit.provider.ack` | boolean | `true` | 是否启用 publisher confirm/return |
| `raf.rabbit.consumer.group` | string | `DEFAULT_RABBIT_GROUP` | 消费者组名，用于注册队列 Bean 名称 |
| `raf.rabbit.consumer.concurrentConsumers` | int | `3` | 初始并发消费者数 |
| `raf.rabbit.consumer.maxConcurrentConsumers` | int | `10` | 最大并发消费者数 |
| `raf.rabbit.security.strictTypeValidation` | boolean | `true` | 是否启用消息类型白名单校验 |
| `raf.rabbit.security.allowedMessageClasses` | set | 空 | 允许的 `@type` 类型白名单；为空时兼容历史消息 |
| `raf.rabbit.security.maxRetryCount` | int | `3` | 最大重试次数 |
| `raf.rabbit.ssl.enabled` | boolean | `false` | 是否启用 SSL |
| `raf.rabbit.ssl.keyStore` | string | — | 客户端证书路径 |
| `raf.rabbit.ssl.keyStorePassword` | string | — | 客户端证书密码 |
| `raf.rabbit.ssl.trustStore` | string | — | 信任证书路径 |
| `raf.rabbit.ssl.trustStorePassword` | string | — | 信任证书密码 |

## 快速接入

```xml
<dependency>
    <groupId>io.github.jerryraf</groupId>
    <artifactId>raf-framework-rabbit-starter</artifactId>
</dependency>
```

```yaml
raf:
  rabbit:
    enabled: true
    addresses: 127.0.0.1:5672
    username: guest
    password: guest
    virtualHost: /
    provider:
      ack: true
    consumer:
      group: order-service
      concurrentConsumers: 3
      maxConcurrentConsumers: 10
```

## 发送消息

```java
@Autowired
private RabbitMqMessageSender messageSender;

public void publishOrderCreated(String orderJson) {
    RabbitMqMessage message = new RabbitMqMessage();
    message.setMessage(orderJson);
    messageSender.send(message, "order.exchange", "order.created");
}

public void publishDelay(String orderJson) {
    RabbitMqMessage message = new RabbitMqMessage();
    message.setMessage(orderJson);
    messageSender.sendDelay(message, "order.timeout", 30);
}
```

发送失败会抛出 `InfrastructureException`，业务侧应让事务回滚或进入本地补偿流程，不要在调用点吞掉异常。

## 消费消息

```java
@RabbitMqConsumer(
        exchange = "order.exchange",
        routingKey = "order.created",
        queue = "order.created.queue")
public class OrderCreatedConsumer extends AbstractRabbitConsumerListener {

    @Override
    public void onMessage(RabbitMqMessage message) {
        orderService.process(message.getMessage());
    }

    @Override
    public void onFailure(Message message, String error) {
        failureRecorder.record(message.getBody(), error);
    }
}
```

最佳实践：

- 消费逻辑必须幂等，使用业务唯一键或 `msgId` 做去重。
- 生产环境建议配置 DLX/重试队列，不建议无限 requeue。
- 开启 SSL 时必须保证证书路径和密码正确，配置错误会 fail-fast。
- 敏感地址、密码使用 Jasypt 加密，不在框架层硬编码。
