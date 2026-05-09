# RabbitMQ Starter 重构实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 raf-framework-rabbit-starter 重构为全配置驱动模式——Exchange/Queue/Binding 拓扑由生产者服务通过 Nacos/yaml 配置在启动时自动声明，消费者只需指定监听队列名，同时修复现有 8 个已知 bug。

**Architecture:** `RabbitMqProperties` 新增 `bindings` 列表，每个 binding 描述一个完整拓扑（普通/延迟/个性化队列）；`RabbitMqConfig` 在启动时读取 bindings 自动声明所有 Exchange/Queue/Binding Bean；`@RabbitMqConsumer` 注解简化为只需 `queue` 属性，支持 `\${...}` 占位符；移除内存重试缓存 `RabbitMessageCacheMgr`，改为标准 publisher confirm 模式；移除无效的类型白名单校验逻辑。

**Tech Stack:** Spring AMQP 3.x, Spring Boot 3.x, Java 17, Lombok

---

## 文件变更清单

| 操作 | 文件 | 说明 |
|------|------|------|
| **修改** | `RabbitMqProperties.java` | 新增 `bindings` 列表；移除 `Security` 内部类；`delay` 字段迁移进 binding |
| **修改** | `RabbitMqConfig.java` | 重写拓扑声明逻辑（读 bindings）；移除 delay 自动声明旧逻辑；修复 confirm 无条件开启 bug |
| **修改** | `RabbitMqConsumer.java` | 注解只保留 `queue` + `ackModel`，移除 `exchange`/`routingKey` |
| **修改** | `AbstractRabbitConsumerListener.java` | 移除无效类型白名单校验；移除 `sendToDLQ` 空实现；保留 `onMessage`/`onFailure`/`retry` |
| **删除** | `RabbitMessageCacheMgr.java` | 内存重试不可靠，移除 |
| **修改** | `RabbitMqMessageSender.java` | 移除对 `RabbitMessageCacheMgr` 的依赖 |
| **保留** | `RabbitMqMessage.java` | 无需改动 |
| **保留** | `AbstractRabbitSenderConfirm.java` | 移除对 `RabbitMessageCacheMgr` 的 `@Autowired` 依赖 |
| **保留** | `RabbitMqDelayConsumer.java` | 注解保留，消费者用 `@RabbitMqConsumer` 替代即可，后续可废弃 |
| **修改** | `docs-site/examples/raf-example-rabbit-starter.md` | 同步文档 |

---

## Task 1: 重构 RabbitMqProperties — 新增 bindings 配置模型

**Files:**
- Modify: `raf-framework-starter/raf-framework-rabbit-starter/src/main/java/com/raf/framework/rabbit/RabbitMqProperties.java`

- [ ] **Step 1: 替换 RabbitMqProperties.java 完整内容**

```java
package com.raf.framework.rabbit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("raf.rabbit")
public class RabbitMqProperties {

    private boolean enabled = false;
    private String username;
    private String password;
    private String addresses;
    private String virtualHost = "/";
    private Ssl ssl = new Ssl();
    private Provider provider = new Provider();
    private Consumer consumer = new Consumer();

    /** 拓扑声明列表，生产者服务配置，启动时自动声明 Exchange/Queue/Binding */
    private List<BindingDefinition> bindings = new ArrayList<>();

    @Data
    public static class Ssl {
        private boolean enabled = false;
        private String keyStore;
        private String keyStorePassword;
        private String trustStore;
        private String trustStorePassword;
        private String algorithm;
    }

    /** 生产者配置 */
    @Data
    public static class Provider {
        /** 是否开启 publisher confirm + return，默认 false，开启后需实现 AbstractRabbitSenderConfirm */
        private boolean ack = false;
    }

    /** 消费者配置 */
    @Data
    public static class Consumer {
        private String group = "DEFAULT_RABBIT_GROUP";
        private int concurrentConsumers = 3;
        private int maxConcurrentConsumers = 10;
    }

    /** 单条拓扑声明 */
    @Data
    public static class BindingDefinition {
        /** 逻辑名称，仅用于日志和 Bean 命名，不能重复 */
        private String name;

        /** Exchange 名称 */
        private String exchange;

        /**
         * Exchange 类型：direct（默认）/ topic / fanout / headers
         */
        private String exchangeType = "direct";

        /** 队列名称 */
        private String queue;

        /** Routing Key（fanout 类型忽略此字段） */
        private String routingKey = "";

        /** 是否持久化，默认 true */
        private boolean durable = true;

        /** 延迟队列配置，不为 null 时声明 DLX+TTL 拓扑 */
        private DelayConfig delay;

        /** 队列自定义参数，如 x-max-priority、x-queue-mode 等 */
        private Map<String, Object> arguments = new HashMap<>();
    }

    /** 延迟队列（DLX + TTL）配置 */
    @Data
    public static class DelayConfig {
        /** Dead Letter Exchange 名称（消息 TTL 到期后路由到此） */
        private String deadExchange;

        /** Dead Letter Exchange 类型，默认 direct */
        private String deadExchangeType = "direct";

        /** 死信队列名称（设置了 TTL 的等待队列） */
        private String deadQueue;

        /** 死信队列 routing key */
        private String deadRoutingKey = "";

        /**
         * 队列级 TTL（毫秒），设置后消息在死信队列等待此时长后转发到 receive queue。
         * 为 0 时不设置队列级 TTL，改用消息级 TTL（sendDelay 时传入 second 参数）。
         */
        private long ttl = 0;
    }
}
```

- [ ] **Step 2: 验证编译通过**

```bash
cd D:/Framework/raf-framework
mvn compile -pl raf-framework-starter/raf-framework-rabbit-starter -am -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add raf-framework-starter/raf-framework-rabbit-starter/src/main/java/com/raf/framework/rabbit/RabbitMqProperties.java
git commit -m "refactor(rabbit): 重构 RabbitMqProperties，新增 bindings 配置模型，移除 Security/旧 delay 字段"
```

---

## Task 2: 简化 @RabbitMqConsumer 注解 — 只保留 queue + ackModel

**Files:**
- Modify: `raf-framework-starter/raf-framework-rabbit-starter/src/main/java/com/raf/framework/rabbit/RabbitMqConsumer.java`

- [ ] **Step 1: 替换 RabbitMqConsumer.java 完整内容**

```java
package com.raf.framework.rabbit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.amqp.core.AcknowledgeMode;

/**
 * 标记消费者监听器，框架自动注册 SimpleMessageListenerContainer。
 *
 * <p>队列/交换机/绑定关系由生产者服务通过 raf.rabbit.bindings 配置声明，
 * 消费者只需指定监听的队列名，支持 \${...} 占位符从配置中心读取。
 *
 * <p>示例：
 * <pre>
 * {@code @RabbitMqConsumer(queue = "\${mq.order.notify.queue:order.notify.queue}")}
 * public class OrderNotifyConsumer extends AbstractRabbitConsumerListener { ... }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RabbitMqConsumer {

    /**
     * 监听的队列名，支持 \${...} 占位符。
     * 例如：\${mq.order.queue:order.notify.queue}
     */
    String queue();

    /**
     * ACK 模式，默认手动 ACK。
     */
    AcknowledgeMode ackModel() default AcknowledgeMode.MANUAL;
}
```

- [ ] **Step 2: 验证编译通过**

```bash
cd D:/Framework/raf-framework
mvn compile -pl raf-framework-starter/raf-framework-rabbit-starter -am -q
```

Expected: BUILD SUCCESS（此时 RabbitMqConfig 会编译报错，因为还引用了旧的 exchange/routingKey 属性，Task 3 修复）

- [ ] **Step 3: Commit**

```bash
git add raf-framework-starter/raf-framework-rabbit-starter/src/main/java/com/raf/framework/rabbit/RabbitMqConsumer.java
git commit -m "refactor(rabbit): 简化 @RabbitMqConsumer，只保留 queue + ackModel，支持占位符"
```

---

## Task 3: 删除 RabbitMessageCacheMgr — 移除内存重试

**Files:**
- Delete: `raf-framework-starter/raf-framework-rabbit-starter/src/main/java/com/raf/framework/rabbit/RabbitMessageCacheMgr.java`

- [ ] **Step 1: 删除文件**

```bash
rm "D:/Framework/raf-framework/raf-framework-starter/raf-framework-rabbit-starter/src/main/java/com/raf/framework/rabbit/RabbitMessageCacheMgr.java"
```

- [ ] **Step 2: Commit**

```bash
git add -A raf-framework-starter/raf-framework-rabbit-starter/src/main/java/com/raf/framework/rabbit/RabbitMessageCacheMgr.java
git commit -m "refactor(rabbit): 删除 RabbitMessageCacheMgr，内存重试不可靠，由业务侧 giveUp() 持久化兜底"
```

---

## Task 4: 重构 AbstractRabbitSenderConfirm — 移除 CacheMgr 依赖

**Files:**
- Modify: `raf-framework-starter/raf-framework-rabbit-starter/src/main/java/com/raf/framework/rabbit/AbstractRabbitSenderConfirm.java`

- [ ] **Step 1: 替换 AbstractRabbitSenderConfirm.java 完整内容**

```java
package com.raf.framework.rabbit;

import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate.ConfirmCallback;

/**
 * 生产者发送确认基类。
 *
 * <p>当 raf.rabbit.provider.ack=true 时，框架自动将此 Bean 注册为
 * RabbitTemplate 的 ConfirmCallback 和 ReturnsCallback。
 *
 * <p>业务侧继承此类并实现 {@link #giveUp}，在消息彻底无法投递时持久化到 DB 做补偿。
 *
 * <p>示例：
 * <pre>
 * {@code
 * @Component
 * public class OrderSenderConfirm extends AbstractRabbitSenderConfirm {
 *     @Override
 *     public void giveUp(RabbitMqMessage message, Integer replyCode,
 *                        String replyText, String exchange, String routingKey) {
 *         // 持久化失败消息到 DB
 *     }
 * }
 * }
 * </pre>
 */
@Slf4j
public abstract class AbstractRabbitSenderConfirm
        implements ConfirmCallback, RabbitTemplate.ReturnsCallback {

    /**
     * confirm 回调：消息是否成功到达 Exchange。
     * ack=false 时记录日志，由业务侧决定是否重发。
     */
    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        if (correlationData == null) {
            return;
        }
        String msgId = correlationData.getId();
        if (!ack) {
            log.error("Message failed to reach exchange, msgId={}, cause={}", msgId, cause);
            onConfirmFail(msgId, cause);
        }
    }

    /**
     * return 回调：消息到达 Exchange 但无法路由到任何 Queue。
     * 调用 {@link #giveUp} 通知业务侧处理。
     */
    @Override
    public void returnedMessage(ReturnedMessage returnedMessage) {
        String body = new String(returnedMessage.getMessage().getBody(), StandardCharsets.UTF_8);
        log.error("Message returned from exchange, exchange={}, routingKey={}, replyCode={}, replyText={}",
                returnedMessage.getExchange(),
                returnedMessage.getRoutingKey(),
                returnedMessage.getReplyCode(),
                returnedMessage.getReplyText());
        try {
            // 尝试解析消息体，解析失败时传 null
            RabbitMqMessage message = tryParseMessage(body);
            giveUp(message,
                    returnedMessage.getReplyCode(),
                    returnedMessage.getReplyText(),
                    returnedMessage.getExchange(),
                    returnedMessage.getRoutingKey());
        } catch (Exception ex) {
            log.error("giveUp callback threw exception, body={}", body, ex);
        }
    }

    /**
     * confirm 失败回调（消息未到达 Exchange）。
     * 默认只打日志，子类可重写做告警或重发。
     *
     * @param msgId 消息 ID
     * @param cause 失败原因
     */
    protected void onConfirmFail(String msgId, String cause) {
        // 默认空实现，子类按需重写
    }

    /**
     * 消息彻底无法投递时的兜底处理（到达 Exchange 但无法路由到 Queue）。
     * 业务侧必须实现此方法，将失败消息持久化到 DB 以便人工补偿。
     *
     * @param message    消息体（解析失败时为 null）
     * @param replyCode  AMQP reply code
     * @param replyText  AMQP reply text
     * @param exchange   目标 Exchange
     * @param routingKey 路由 Key
     */
    public abstract void giveUp(
            RabbitMqMessage message,
            Integer replyCode,
            String replyText,
            String exchange,
            String routingKey);

    private RabbitMqMessage tryParseMessage(String body) {
        try {
            // 简单 JSON 解析，避免引入额外依赖
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(body, RabbitMqMessage.class);
        } catch (Exception e) {
            log.warn("Failed to parse message body: {}", body);
            return null;
        }
    }
}
```

- [ ] **Step 2: 验证编译**

```bash
cd D:/Framework/raf-framework
mvn compile -pl raf-framework-starter/raf-framework-rabbit-starter -am -q
```

- [ ] **Step 3: Commit**

```bash
git add raf-framework-starter/raf-framework-rabbit-starter/src/main/java/com/raf/framework/rabbit/AbstractRabbitSenderConfirm.java
git commit -m "refactor(rabbit): 重构 AbstractRabbitSenderConfirm，移除 CacheMgr 依赖，新增 onConfirmFail 钩子"
```

---

## Task 5: 重构 RabbitMqMessageSender — 移除 CacheMgr 依赖

**Files:**
- Modify: `raf-framework-starter/raf-framework-rabbit-starter/src/main/java/com/raf/framework/rabbit/RabbitMqMessageSender.java`

- [ ] **Step 1: 替换 RabbitMqMessageSender.java 完整内容**

```java
package com.raf.framework.rabbit;

import com.raf.framework.core.common.exception.BusinessException;
import com.raf.framework.core.common.exception.InfrastructureException;
import com.raf.framework.core.common.result.RafResponseEnum;
import com.raf.framework.core.jackson.JsonService;
import com.raf.framework.core.snowflake.SnowFlakeBuilder;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * RabbitMQ 消息发送门面。
 *
 * <p>普通消息：{@link #send(RabbitMqMessage, String, String)}
 * <p>延迟消息：{@link #sendDelay(RabbitMqMessage, String, String, int)}
 *   - exchange: 死信 Exchange（消息先投递到此，TTL 到期后由 DLX 转发到 receive queue）
 *   - routingKey: 死信队列的 routing key
 *   - second: 消息级 TTL（秒），队列级 TTL 在 bindings 配置中设置时此参数可传 0
 */
@Slf4j
@RequiredArgsConstructor
public class RabbitMqMessageSender {

    private final RabbitTemplate rabbitTemplate;
    private final JsonService json;

    /**
     * 发送普通消息。
     *
     * @param message  消息体
     * @param exchange 目标 Exchange
     * @param routeKey Routing Key
     */
    public void send(RabbitMqMessage message, String exchange, String routeKey) {
        String msgId = Optional.ofNullable(message.getMsgId())
                .orElseGet(SnowFlakeBuilder::generateId);
        message.setMsgId(msgId);
        CorrelationData data = new CorrelationData(msgId);
        try {
            rabbitTemplate.convertAndSend(exchange, routeKey, json.toJson(message), data);
            log.debug("Message sent. exchange={}, routeKey={}, msgId={}", exchange, routeKey, msgId);
        } catch (AmqpException ex) {
            log.error("RabbitMQ send failed. exchange={}, routeKey={}, msgId={}", exchange, routeKey, msgId, ex);
            throw new InfrastructureException(RafResponseEnum.SERVER_ERROR, "RabbitMQ send failed", ex);
        }
    }

    /**
     * 发送延迟消息（消息级 TTL）。
     *
     * @param message    消息体
     * @param exchange   死信 Exchange 名称
     * @param routingKey 死信队列 routing key
     * @param second     延迟秒数（消息级 TTL）
     */
    public void sendDelay(RabbitMqMessage message, String exchange, String routingKey, int second) {
        String msgId = Optional.ofNullable(message.getMsgId())
                .orElseGet(SnowFlakeBuilder::generateId);
        message.setMsgId(msgId);
        CorrelationData data = new CorrelationData(msgId);
        MessagePostProcessor ttlProcessor = msg -> {
            if (second > 0) {
                msg.getMessageProperties().setExpiration(String.valueOf((long) second * 1000));
            }
            return msg;
        };
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, json.toJson(message), ttlProcessor, data);
            log.debug("Delay message sent. exchange={}, routingKey={}, msgId={}, ttl={}s",
                    exchange, routingKey, msgId, second);
        } catch (AmqpException ex) {
            log.error("RabbitMQ delay send failed. exchange={}, routingKey={}, msgId={}",
                    exchange, routingKey, msgId, ex);
            throw new InfrastructureException(RafResponseEnum.SERVER_ERROR, "RabbitMQ delay send failed", ex);
        }
    }

    /**
     * 发送普通消息（字符串重载）。
     */
    public void send(String msgId, String messageBody, String exchange, String routeKey) {
        if (StringUtils.isBlank(messageBody)) {
            throw new BusinessException(RafResponseEnum.PARAM_ERROR, "message body must not be blank");
        }
        RabbitMqMessage msg = new RabbitMqMessage();
        msg.setMsgId(msgId);
        msg.setMessage(messageBody);
        send(msg, exchange, routeKey);
    }

    /**
     * 发送延迟消息（字符串重载）。
     */
    public void sendDelay(String msgId, String messageBody, String exchange, String routingKey, int second) {
        if (StringUtils.isBlank(messageBody)) {
            throw new BusinessException(RafResponseEnum.PARAM_ERROR, "message body must not be blank");
        }
        RabbitMqMessage msg = new RabbitMqMessage();
        msg.setMsgId(msgId);
        msg.setMessage(messageBody);
        sendDelay(msg, exchange, routingKey, second);
    }
}
```

- [ ] **Step 2: 验证编译**

```bash
cd D:/Framework/raf-framework
mvn compile -pl raf-framework-starter/raf-framework-rabbit-starter -am -q
```

- [ ] **Step 3: Commit**

```bash
git add raf-framework-starter/raf-framework-rabbit-starter/src/main/java/com/raf/framework/rabbit/RabbitMqMessageSender.java
git commit -m "refactor(rabbit): 重构 RabbitMqMessageSender，移除 CacheMgr，sendDelay 参数改为 exchange+routingKey+second"
```

---

## Task 6: 重构 AbstractRabbitConsumerListener — 移除无效校验

**Files:**
- Modify: `raf-framework-starter/raf-framework-rabbit-starter/src/main/java/com/raf/framework/rabbit/AbstractRabbitConsumerListener.java`

- [ ] **Step 1: 替换 AbstractRabbitConsumerListener.java 完整内容**

```java
package com.raf.framework.rabbit;

import com.raf.framework.core.jackson.JsonService;
import com.raf.framework.core.spring.bean.SpringContext;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;

/**
 * RabbitMQ 消费者基类，封装手动 ACK、重试、失败回调。
 *
 * <p>使用方式：
 * <ol>
 *   <li>继承此类，实现 {@link #onMessage} 处理业务逻辑</li>
 *   <li>实现 {@link #onFailure} 处理最终失败（持久化到 DB）</li>
 *   <li>按需重写 {@link #retry} 控制是否重试（默认不重试）</li>
 *   <li>在类上加 {@link RabbitMqConsumer} 注解指定监听队列</li>
 * </ol>
 *
 * <p>消费流程：
 * <pre>
 * onMessage(RabbitMqMessage)
 *   ├── 成功 → basicAck
 *   └── 异常
 *         ├── retry() == true → basicNack(requeue=true)  重新入队
 *         └── retry() == false → onFailure() → basicReject(requeue=false)  进死信队列
 * </pre>
 */
@Slf4j
public abstract class AbstractRabbitConsumerListener implements ChannelAwareMessageListener {

    @Override
    public void onMessage(Message message, Channel channel) throws Exception {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        RabbitMqMessage rabbitMqMessage = null;

        try {
            JsonService json = SpringContext.getBean(JsonService.class);
            rabbitMqMessage = json.parse(body, RabbitMqMessage.class);
            this.onMessage(rabbitMqMessage);
            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            log.error("Message consumption failed, msgId={}, error={}",
                    rabbitMqMessage != null ? rabbitMqMessage.getMsgId() : "unknown",
                    ex.getMessage(), ex);

            if (rabbitMqMessage != null && retry(rabbitMqMessage)) {
                log.warn("Retrying message, msgId={}", rabbitMqMessage.getMsgId());
                channel.basicNack(deliveryTag, false, true);
            } else {
                doGiveUp(message, channel, deliveryTag, ex.getMessage());
            }
        }
    }

    private void doGiveUp(Message message, Channel channel, long deliveryTag, String error) {
        try {
            onFailure(message, error);
        } catch (Exception ex) {
            log.error("onFailure callback threw exception, body={}",
                    new String(message.getBody(), StandardCharsets.UTF_8), ex);
        } finally {
            try {
                channel.basicReject(deliveryTag, false);
            } catch (IOException ex) {
                log.error("basicReject failed: {}", ex.getMessage());
            }
        }
    }

    /**
     * 处理业务消息。
     *
     * @param message 反序列化后的消息
     * @throws Exception 抛出异常时触发重试或失败回调
     */
    public abstract void onMessage(RabbitMqMessage message) throws Exception;

    /**
     * 消息最终消费失败的兜底处理（不再重试时调用）。
     * 业务侧应将失败消息持久化到 DB，以便人工补偿。
     *
     * @param message 原始 AMQP 消息
     * @param error   错误描述
     * @throws Exception 允许抛出，框架会捕获并继续 reject
     */
    public abstract void onFailure(Message message, String error) throws Exception;

    /**
     * 是否重试。默认不重试，子类按需重写。
     *
     * <p>注意：返回 true 会将消息重新入队（basicNack requeue=true），
     * 如果消费逻辑存在 bug 会导致无限循环，建议配合 {@link RabbitMqMessage#isOverTimes()} 限制次数。
     *
     * @param message 失败的消息
     * @return true 重新入队，false 进死信队列
     */
    public boolean retry(RabbitMqMessage message) {
        return false;
    }
}
```

- [ ] **Step 2: 验证编译**

```bash
cd D:/Framework/raf-framework
mvn compile -pl raf-framework-starter/raf-framework-rabbit-starter -am -q
```

- [ ] **Step 3: Commit**

```bash
git add raf-framework-starter/raf-framework-rabbit-starter/src/main/java/com/raf/framework/rabbit/AbstractRabbitConsumerListener.java
git commit -m "refactor(rabbit): 重构 AbstractRabbitConsumerListener，移除无效类型白名单校验，精简消费流程"
```

---

## Task 7: 核心重构 RabbitMqConfig — 全配置驱动拓扑声明

**Files:**
- Modify: `raf-framework-starter/raf-framework-rabbit-starter/src/main/java/com/raf/framework/rabbit/RabbitMqConfig.java`

- [ ] **Step 1: 替换 RabbitMqConfig.java 完整内容**

将文件内容替换为以下代码（分三段说明逻辑，实际写入一个完整文件）：

**第一段：类声明 + 连接工厂**

```java
package com.raf.framework.rabbit;

import javax.net.ssl.SSLContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ResourceUtils;

@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "raf.rabbit", name = "enabled", havingValue = "true")
@ConditionalOnClass(CachingConnectionFactory.class)
@EnableConfigurationProperties(RabbitMqProperties.class)
public class RabbitMqConfig implements BeanFactoryPostProcessor, ApplicationContextAware {

    private ConfigurableListableBeanFactory beanFactory;
    private ApplicationContext applicationContext;

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Bean
    public CachingConnectionFactory connectionFactory(RabbitMqProperties props) {
        CachingConnectionFactory factory = new CachingConnectionFactory();
        factory.setAddresses(props.getAddresses());
        factory.setUsername(props.getUsername());
        factory.setPassword(props.getPassword());
        factory.setVirtualHost(props.getVirtualHost());
        String appName = applicationContext.getEnvironment()
                .getProperty("spring.application.name", "unknown");
        factory.setConnectionNameStrategy(c -> appName + "-" + c.getHost());
        // 修复 bug：publisher confirm 仅在 raf.rabbit.provider.ack=true 时开启
        if (props.getProvider() != null && props.getProvider().isAck()) {
            factory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
            factory.setPublisherReturns(true);
        }
        if (props.getSsl() != null && props.getSsl().isEnabled()) {
            applySsl(factory, props.getSsl());
        }
        return factory;
    }

    private void applySsl(CachingConnectionFactory factory, RabbitMqProperties.Ssl ssl) {
        try {
            SSLContext ctx = SSLContextBuilder.create()
                    .loadKeyMaterial(ResourceUtils.getFile(ssl.getKeyStore()),
                            ssl.getKeyStorePassword().toCharArray(),
                            ssl.getKeyStorePassword().toCharArray())
                    .loadTrustMaterial(ResourceUtils.getFile(ssl.getTrustStore()),
                            ssl.getTrustStorePassword().toCharArray())
                    .build();
            factory.getRabbitConnectionFactory().useSslProtocol(ctx);
        } catch (Exception e) {
            throw new BeanInitializationException("RabbitMQ SSL configuration failed", e);
        }
    }
```


**第二段：RabbitTemplate + RabbitAdmin + RabbitMqMessageSender Bean**

```java
    @Bean
    public RabbitTemplate rabbitTemplate(CachingConnectionFactory factory, RabbitMqProperties props) {
        RabbitTemplate template = new RabbitTemplate(factory);
        if (props.getProvider() != null && props.getProvider().isAck()) {
            template.setMandatory(true);
            Map<String, AbstractRabbitSenderConfirm> confirms =
                    applicationContext.getBeansOfType(AbstractRabbitSenderConfirm.class);
            if (!confirms.isEmpty()) {
                AbstractRabbitSenderConfirm confirm = confirms.values().iterator().next();
                template.setConfirmCallback(confirm);
                template.setReturnsCallback(confirm);
                log.info("RabbitMQ confirm callback: {}", confirm.getClass().getSimpleName());
            } else {
                log.warn("raf.rabbit.provider.ack=true but no AbstractRabbitSenderConfirm bean found. " +
                        "Implement AbstractRabbitSenderConfirm to handle send failures.");
            }
        }
        return template;
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setAutoStartup(true);
        return admin;
    }

    @Bean
    public RabbitMqMessageSender rabbitMqMessageSender(RabbitTemplate rabbitTemplate,
                                                        com.raf.framework.core.jackson.JsonService json) {
        return new RabbitMqMessageSender(rabbitTemplate, json);
    }
```

**第三段：拓扑声明（bindings 配置驱动）+ 消费者监听容器注册**

```java
    /**
     * 根据 raf.rabbit.bindings 配置自动声明 Exchange/Queue/Binding。
     * 生产者服务配置此项，消费者服务不需要。
     * RabbitAdmin 会在连接建立后自动执行声明。
     */
    @Bean
    public List<Declarable> rabbitTopologyDeclarations(RabbitMqProperties props) {
        List<RabbitMqProperties.BindingDefinition> bindings = props.getBindings();
        if (CollectionUtils.isEmpty(bindings)) {
            return List.of();
        }
        return bindings.stream()
                .flatMap(def -> buildDeclarables(def).stream())
                .collect(Collectors.toList());
    }

    private List<Declarable> buildDeclarables(RabbitMqProperties.BindingDefinition def) {
        List<Declarable> result = new java.util.ArrayList<>();

        // 1. 声明 Exchange
        Exchange exchange = buildExchange(def.getExchange(), def.getExchangeType(), def.isDurable());
        result.add(exchange);

        // 2. 声明 Queue（带自定义参数）
        Map<String, Object> args = new HashMap<>(def.getArguments());
        if (def.getDelay() != null && def.getDelay().getTtl() > 0) {
            args.put("x-message-ttl", def.getDelay().getTtl());
        }
        Queue queue = QueueBuilder.durable(def.getQueue()).withArguments(args).build();
        result.add(queue);

        // 3. 声明 Binding（receive queue → exchange）
        result.add(buildBinding(queue, exchange, def.getRoutingKey()));

        // 4. 延迟队列：额外声明 dead exchange + dead queue + dead binding
        if (def.getDelay() != null) {
            RabbitMqProperties.DelayConfig delay = def.getDelay();

            Exchange deadExchange = buildExchange(
                    delay.getDeadExchange(), delay.getDeadExchangeType(), def.isDurable());
            result.add(deadExchange);

            // dead queue：设置 DLX 参数，消息过期后路由到 receive exchange
            Map<String, Object> deadArgs = new HashMap<>();
            deadArgs.put("x-dead-letter-exchange", def.getExchange());
            deadArgs.put("x-dead-letter-routing-key", def.getRoutingKey());
            Queue deadQueue = QueueBuilder.durable(delay.getDeadQueue())
                    .withArguments(deadArgs).build();
            result.add(deadQueue);

            result.add(buildBinding(deadQueue, deadExchange, delay.getDeadRoutingKey()));

            log.info("Declared delay topology: deadExchange={}, deadQueue={} -> exchange={}, queue={}",
                    delay.getDeadExchange(), delay.getDeadQueue(), def.getExchange(), def.getQueue());
        } else {
            log.info("Declared topology: exchange={}, queue={}, routingKey={}",
                    def.getExchange(), def.getQueue(), def.getRoutingKey());
        }
        return result;
    }

    private Exchange buildExchange(String name, String type, boolean durable) {
        switch (type.toLowerCase()) {
            case "topic":   return ExchangeBuilder.topicExchange(name).durable(durable).build();
            case "fanout":  return ExchangeBuilder.fanoutExchange(name).durable(durable).build();
            case "headers": return ExchangeBuilder.headersExchange(name).durable(durable).build();
            default:        return ExchangeBuilder.directExchange(name).durable(durable).build();
        }
    }

    private Binding buildBinding(Queue queue, Exchange exchange, String routingKey) {
        if (exchange instanceof FanoutExchange) {
            return BindingBuilder.bind(queue).to((FanoutExchange) exchange);
        }
        if (exchange instanceof TopicExchange) {
            return BindingBuilder.bind(queue).to((TopicExchange) exchange).with(routingKey);
        }
        if (exchange instanceof HeadersExchange) {
            return BindingBuilder.bind(queue).to((HeadersExchange) exchange).whereAny(new HashMap<>()).match();
        }
        // default: DirectExchange
        return BindingBuilder.bind(queue).to((DirectExchange) exchange).with(routingKey);
    }

    /**
     * 扫描所有带 @RabbitMqConsumer 注解的 Bean，注册 SimpleMessageListenerContainer。
     * 消费者只需指定 queue 名，不再负责声明拓扑。
     */
    @Bean
    public List<SimpleMessageListenerContainer> listenerContainers(
            CachingConnectionFactory factory, RabbitMqProperties props) {
        String[] beanNames = applicationContext.getBeanNamesForAnnotation(RabbitMqConsumer.class);
        return java.util.Arrays.stream(beanNames)
                .map(name -> buildListenerContainer(name, factory, props))
                .collect(Collectors.toList());
    }

    private SimpleMessageListenerContainer buildListenerContainer(
            String beanName, CachingConnectionFactory factory, RabbitMqProperties props) {
        AbstractRabbitConsumerListener listener =
                applicationContext.getBean(beanName, AbstractRabbitConsumerListener.class);
        Class<?> clazz = AopUtils.isAopProxy(listener) ? AopUtils.getTargetClass(listener) : listener.getClass();
        RabbitMqConsumer annotation = clazz.getAnnotation(RabbitMqConsumer.class);

        // 支持 ${...} 占位符解析
        String queueName = applicationContext.getEnvironment()
                .resolvePlaceholders(annotation.queue());

        Queue queue = new Queue(queueName, true);
        beanFactory.registerSingleton(queueName + "_queue", queue);

        RabbitMqProperties.Consumer consumer = props.getConsumer();
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(factory);
        container.setQueues(queue);
        container.setExposeListenerChannel(true);
        container.setConcurrentConsumers(consumer != null ? consumer.getConcurrentConsumers() : 3);
        container.setMaxConcurrentConsumers(consumer != null ? consumer.getMaxConcurrentConsumers() : 10);
        container.setAcknowledgeMode(annotation.ackModel());
        container.setMessageListener(listener);

        beanFactory.registerSingleton(queueName + "_container", container);
        log.info("Registered listener container: queue={}, consumer={}", queueName, clazz.getSimpleName());
        return container;
    }
}
```

- [ ] **Step 2: 验证编译通过**

```bash
cd D:/Framework/raf-framework
mvn compile -pl raf-framework-starter/raf-framework-rabbit-starter -am -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add raf-framework-starter/raf-framework-rabbit-starter/src/main/java/com/raf/framework/rabbit/RabbitMqConfig.java
git commit -m "refactor(rabbit): 重构 RabbitMqConfig，全配置驱动拓扑声明，消费者只需指定队列名，修复 confirm 无条件开启 bug"
```

---

## Task 8: 全量编译验证

**Files:** 无新文件，验证整个 starter 模块编译通过。

- [ ] **Step 1: 全量编译**

```bash
cd D:/Framework/raf-framework
mvn compile -pl raf-framework-starter/raf-framework-rabbit-starter -am
```

Expected: BUILD SUCCESS，无编译错误。

- [ ] **Step 2: 检查是否有遗漏的旧引用**

```bash
grep -r "RabbitMessageCacheMgr\|rabbitMqDelayProvider\|allowedMessageClasses\|strictTypeValidation" \
  D:/Framework/raf-framework/raf-framework-starter/raf-framework-rabbit-starter/src/
```

Expected: 无输出（所有旧引用已清除）。

- [ ] **Step 3: Commit**

```bash
git add -A raf-framework-starter/raf-framework-rabbit-starter/
git commit -m "refactor(rabbit): 全量编译验证通过，清理所有旧引用"
```

---

## Task 9: 更新 raf-example-rabbit-starter.md 文档

**Files:**
- Modify: `docs-site/examples/raf-example-rabbit-starter.md`

- [ ] **Step 1: 重写文档，反映新的全配置驱动设计**

文档需覆盖以下内容（按顺序）：

1. **核心设计原则表格**：生产者负责拓扑声明，消费者只指定队列名
2. **生产者服务完整 yaml 配置**：包含 bindings（普通/延迟/topic/个性化队列示例）
3. **消费者服务 yaml 配置**：只有连接信息 + consumer 并发配置，无 bindings
4. **生产者代码**：`AbstractRabbitSenderConfirm` 实现示例（含 `giveUp` 持久化到 DB）、`RabbitMqMessageSender.send()` 和 `sendDelay()` 用法
5. **消费者代码**：`@RabbitMqConsumer(queue="${...}")` + `AbstractRabbitConsumerListener` 实现示例（含 `onMessage`、`onFailure`、`retry`）
6. **配置项详解表格**：所有 `raf.rabbit.*` 配置项、类型、默认值、说明
7. **延迟队列拓扑说明图**：文字描述 Producer → dead exchange → dead queue (TTL) → DLX → receive exchange → queue → Consumer
8. **最佳实践**：幂等、失败持久化、多环境队列名隔离、并发调优
9. **常见问题**：消息丢失排查、延迟不准确、消费者重复消费

- [ ] **Step 2: Commit**

```bash
git add docs-site/examples/raf-example-rabbit-starter.md
git commit -m "docs(rabbit): 同步文档，反映全配置驱动重构后的设计"
```

---

## 配置项完整参考

以下是重构后 `raf.rabbit.*` 所有配置项：

| 配置键 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `raf.rabbit.enabled` | boolean | `false` | 是否启用 RabbitMQ |
| `raf.rabbit.addresses` | string | — | 地址，集群用逗号分隔，如 `host1:5672,host2:5672` |
| `raf.rabbit.username` | string | — | 用户名 |
| `raf.rabbit.password` | string | — | 密码（支持 Jasypt 加密：`ENC(密文)`） |
| `raf.rabbit.virtual-host` | string | `/` | 虚拟主机 |
| `raf.rabbit.provider.ack` | boolean | `false` | 是否开启 publisher confirm + return |
| `raf.rabbit.consumer.group` | string | `DEFAULT_RABBIT_GROUP` | 消费者组名（用于 Bean 命名） |
| `raf.rabbit.consumer.concurrent-consumers` | int | `3` | 初始并发消费者数 |
| `raf.rabbit.consumer.max-concurrent-consumers` | int | `10` | 最大并发消费者数 |
| `raf.rabbit.ssl.enabled` | boolean | `false` | 是否启用 SSL |
| `raf.rabbit.ssl.key-store` | string | — | KeyStore 路径 |
| `raf.rabbit.ssl.key-store-password` | string | — | KeyStore 密码 |
| `raf.rabbit.ssl.trust-store` | string | — | TrustStore 路径 |
| `raf.rabbit.ssl.trust-store-password` | string | — | TrustStore 密码 |
| `raf.rabbit.bindings[].name` | string | — | 拓扑逻辑名（唯一，用于日志） |
| `raf.rabbit.bindings[].exchange` | string | — | Exchange 名称 |
| `raf.rabbit.bindings[].exchange-type` | string | `direct` | Exchange 类型：direct/topic/fanout/headers |
| `raf.rabbit.bindings[].queue` | string | — | 队列名称 |
| `raf.rabbit.bindings[].routing-key` | string | `""` | Routing Key（fanout 忽略） |
| `raf.rabbit.bindings[].durable` | boolean | `true` | 是否持久化 |
| `raf.rabbit.bindings[].arguments` | map | `{}` | 队列自定义参数（x-max-priority 等） |
| `raf.rabbit.bindings[].delay.dead-exchange` | string | — | 死信 Exchange 名称 |
| `raf.rabbit.bindings[].delay.dead-exchange-type` | string | `direct` | 死信 Exchange 类型 |
| `raf.rabbit.bindings[].delay.dead-queue` | string | — | 死信队列名称（TTL 等待队列） |
| `raf.rabbit.bindings[].delay.dead-routing-key` | string | `""` | 死信队列 Routing Key |
| `raf.rabbit.bindings[].delay.ttl` | long | `0` | 队列级 TTL（毫秒），0 表示用消息级 TTL |

---

