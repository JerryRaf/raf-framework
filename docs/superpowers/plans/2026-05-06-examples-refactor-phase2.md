# Examples 重构 Phase 2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新建 8 个 example（nacos/redis重写/rabbit/mybatis重写/mongodb/rocketmq/kafka/okhttp/elasticsearch/openapi），每个对应一个 starter，演示其核心功能。

**Architecture:** 每个 example 继承 `raf-framework-parent`，包名统一为 `io.github.jerryraf.examples.{shortname}`，artifactId 为 `raf-example-{name}-starter`，application.yml 中间件地址全部使用环境变量占位符。

**Tech Stack:** Java 17, Spring Boot 3.4.7, Maven, raf-framework starters

**Spec:** `docs/superpowers/specs/2026-05-06-examples-refactor-design.md`

**Prerequisites:** Phase 1 已完成（现有 5 个 example 已重命名）

---

## Task 1: raf-example-redis-starter（重写）

**Files:**
- Create: `examples/raf-example-redis-starter/pom.xml`
- Create: `examples/raf-example-redis-starter/src/main/resources/application.yml`
- Create: `examples/raf-example-redis-starter/src/main/java/io/github/jerryraf/examples/redis/RedisExampleApplication.java`
- Create: `examples/raf-example-redis-starter/src/main/java/io/github/jerryraf/examples/redis/service/RedisOpsService.java`
- Create: `examples/raf-example-redis-starter/src/main/java/io/github/jerryraf/examples/redis/service/StockService.java`
- Create: `examples/raf-example-redis-starter/src/main/java/io/github/jerryraf/examples/redis/controller/RedisOpsController.java`
- Create: `examples/raf-example-redis-starter/src/main/java/io/github/jerryraf/examples/redis/controller/StockController.java`
- Create: `examples/raf-example-redis-starter/src/main/java/io/github/jerryraf/examples/redis/dto/StockDeductReq.java`

- [ ] **Step 1: 创建 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>io.github.jerryraf</groupId>
        <artifactId>raf-framework-parent</artifactId>
        <version>${revision}</version>
        <relativePath>../../raf-framework-parent</relativePath>
    </parent>

    <groupId>io.github.jerryraf.examples</groupId>
    <artifactId>raf-example-redis-starter</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <name>RAF Example - Redis Starter</name>
    <description>Demonstrates RedisService common operations (String/Hash/List/Set/ZSet) and Redisson distributed lock</description>

    <dependencies>
        <dependency>
            <groupId>io.github.jerryraf</groupId>
            <artifactId>raf-framework-web-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>io.github.jerryraf</groupId>
            <artifactId>raf-framework-redis-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: 创建 application.yml**

```yaml
spring:
  application:
    name: raf-example-redis-starter

server:
  port: 8080

raf:
  redis:
    enabled: true
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}
    database: 0
  redisson:
    enabled: true
  log:
    enabled: true
    level: REQ_BODY
```

- [ ] **Step 3: 创建 Application 启动类**

文件：`src/main/java/io/github/jerryraf/examples/redis/RedisExampleApplication.java`

```java
package io.github.jerryraf.examples.redis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Redis Starter Example Application.
 *
 * <p>Demonstrates:
 * <ul>
 *   <li>RedisService: String/Hash/List/Set/ZSet common operations</li>
 *   <li>Redisson distributed lock: tryLock for stock deduction</li>
 * </ul>
 *
 * @author Jerry
 */
@SpringBootApplication
public class RedisExampleApplication {
    public static void main(String[] args) {
        SpringApplication.run(RedisExampleApplication.class, args);
    }
}
```

- [ ] **Step 4: 创建 RedisOpsService（常用操作演示）**

文件：`src/main/java/io/github/jerryraf/examples/redis/service/RedisOpsService.java`

```java
package io.github.jerryraf.examples.redis.service;

import com.raf.framework.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Demonstrates common Redis data structure operations via RedisService.
 *
 * <p>Covered operations:
 * <ul>
 *   <li>String: set/get/setIfAbsent/increment/decrement/expire</li>
 *   <li>Hash: hSet/hGet/hGetAll/hDel</li>
 *   <li>List: lPush/rPush/lPop/lRange</li>
 *   <li>Set: sAdd/sMembers/sIsMember/sRem</li>
 *   <li>ZSet: zAdd/zRange/zRangeByScore/zRem</li>
 * </ul>
 *
 * @author Jerry
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisOpsService {

    private final RedisService redisService;
    private final RedisTemplate<String, Object> redisTemplate;

    // ===== String =====

    public void stringDemo(String key, String value) {
        // set with TTL
        redisService.set(key, value, 10, TimeUnit.MINUTES);
        log.info("String set: key={}, value={}", key, value);

        // get
        String result = redisService.get(key);
        log.info("String get: key={}, result={}", key, result);

        // setIfAbsent (only set if key does not exist)
        boolean set = redisService.setIfAbsent(key + ":lock", "1", 30, TimeUnit.SECONDS);
        log.info("String setIfAbsent: set={}", set);

        // increment / decrement
        redisService.set("counter:" + key, 0L);
        long after = redisService.increment("counter:" + key);
        log.info("String increment: counter={}", after);
        after = redisService.decrement("counter:" + key);
        log.info("String decrement: counter={}", after);
    }

    // ===== Hash =====

    public void hashDemo(String hashKey) {
        // hSet
        redisTemplate.opsForHash().put(hashKey, "name", "Jerry");
        redisTemplate.opsForHash().put(hashKey, "age", "30");
        redisTemplate.expire(hashKey, 10, TimeUnit.MINUTES);
        log.info("Hash hSet: key={}", hashKey);

        // hGet
        Object name = redisTemplate.opsForHash().get(hashKey, "name");
        log.info("Hash hGet: name={}", name);

        // hGetAll
        Map<Object, Object> all = redisTemplate.opsForHash().entries(hashKey);
        log.info("Hash hGetAll: {}", all);

        // hDel
        redisTemplate.opsForHash().delete(hashKey, "age");
        log.info("Hash hDel: deleted field 'age'");
    }

    // ===== List =====

    public void listDemo(String listKey) {
        // lPush / rPush
        redisTemplate.opsForList().leftPush(listKey, "first");
        redisTemplate.opsForList().rightPush(listKey, "second");
        redisTemplate.opsForList().rightPush(listKey, "third");
        redisTemplate.expire(listKey, 10, TimeUnit.MINUTES);
        log.info("List push: key={}", listKey);

        // lRange (get all)
        List<Object> range = redisTemplate.opsForList().range(listKey, 0, -1);
        log.info("List lRange: {}", range);

        // lPop
        Object popped = redisTemplate.opsForList().leftPop(listKey);
        log.info("List lPop: popped={}", popped);
    }

    // ===== Set =====

    public void setDemo(String setKey) {
        // sAdd
        redisTemplate.opsForSet().add(setKey, "apple", "banana", "cherry");
        redisTemplate.expire(setKey, 10, TimeUnit.MINUTES);
        log.info("Set sAdd: key={}", setKey);

        // sMembers
        Set<Object> members = redisTemplate.opsForSet().members(setKey);
        log.info("Set sMembers: {}", members);

        // sIsMember
        Boolean isMember = redisTemplate.opsForSet().isMember(setKey, "apple");
        log.info("Set sIsMember 'apple': {}", isMember);

        // sRem
        redisTemplate.opsForSet().remove(setKey, "banana");
        log.info("Set sRem: removed 'banana'");
    }

    // ===== ZSet =====

    public void zsetDemo(String zsetKey) {
        // zAdd
        redisTemplate.opsForZSet().add(zsetKey, "player1", 100.0);
        redisTemplate.opsForZSet().add(zsetKey, "player2", 200.0);
        redisTemplate.opsForZSet().add(zsetKey, "player3", 150.0);
        redisTemplate.expire(zsetKey, 10, TimeUnit.MINUTES);
        log.info("ZSet zAdd: key={}", zsetKey);

        // zRange (ascending by score)
        Set<Object> range = redisTemplate.opsForZSet().range(zsetKey, 0, -1);
        log.info("ZSet zRange (asc): {}", range);

        // zRangeByScore
        Set<Object> byScore = redisTemplate.opsForZSet().rangeByScore(zsetKey, 100.0, 160.0);
        log.info("ZSet zRangeByScore [100,160]: {}", byScore);

        // zRem
        redisTemplate.opsForZSet().remove(zsetKey, "player1");
        log.info("ZSet zRem: removed 'player1'");
    }
}
```

- [ ] **Step 5: 创建 StockService（分布式锁演示）**

文件：`src/main/java/io/github/jerryraf/examples/redis/service/StockService.java`

```java
package io.github.jerryraf.examples.redis.service;

import com.raf.framework.core.common.exception.BusinessException;
import com.raf.framework.core.common.exception.InfrastructureException;
import com.raf.framework.core.common.result.RafResponseEnum;
import com.raf.framework.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Demonstrates Redisson distributed lock for stock deduction.
 *
 * <p>Key points:
 * <ul>
 *   <li>tryLock with wait timeout (3s) + lease timeout (10s) — prevents deadlock via WatchDog</li>
 *   <li>Lock key is per-product for fine-grained concurrency control</li>
 *   <li>Always release in finally, guarded by isHeldByCurrentThread()</li>
 * </ul>
 *
 * @author Jerry
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    private static final String STOCK_KEY = "stock:product:";
    private static final String LOCK_KEY = "lock:stock:deduct:";
    private static final int STOCK_TTL_MINUTES = 30;

    private final RedisService redisService;
    private final RedissonClient redissonClient;

    public void initStock(Long productId, int stock) {
        redisService.set(STOCK_KEY + productId, stock, STOCK_TTL_MINUTES, TimeUnit.MINUTES);
        log.info("Stock initialized: productId={}, stock={}", productId, stock);
    }

    public Integer getStock(Long productId) {
        Integer stock = redisService.get(STOCK_KEY + productId);
        if (stock == null) {
            throw new BusinessException(RafResponseEnum.NOT_FOUND, "Product not found: " + productId);
        }
        return stock;
    }

    /**
     * Deduct stock with distributed lock.
     * Demonstrates: Redisson tryLock — wait 3s, auto-release after 10s (WatchDog renews if held).
     */
    public void deductStock(Long productId, int quantity) {
        RLock lock = redissonClient.getLock(LOCK_KEY + productId);
        boolean locked = false;
        try {
            locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!locked) {
                log.warn("Failed to acquire lock: productId={}", productId);
                throw new BusinessException(RafResponseEnum.SERVER_ERROR, "Stock operation busy, please retry");
            }

            Integer current = redisService.get(STOCK_KEY + productId);
            if (current == null) {
                throw new BusinessException(RafResponseEnum.NOT_FOUND, "Product not found: " + productId);
            }
            if (current < quantity) {
                throw new BusinessException(RafResponseEnum.SERVER_ERROR,
                        "Insufficient stock: available=" + current + ", requested=" + quantity);
            }

            redisService.set(STOCK_KEY + productId, current - quantity, STOCK_TTL_MINUTES, TimeUnit.MINUTES);
            log.info("Stock deducted: productId={}, deducted={}, remaining={}", productId, quantity, current - quantity);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InfrastructureException("Lock interrupted: productId=" + productId, e);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

- [ ] **Step 6: 创建 RedisOpsController**

文件：`src/main/java/io/github/jerryraf/examples/redis/controller/RedisOpsController.java`

```java
package io.github.jerryraf.examples.redis.controller;

import com.raf.framework.core.common.result.RafResult;
import io.github.jerryraf.examples.redis.service.RedisOpsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * REST interface for Redis data structure operation demos.
 *
 * @author Jerry
 */
@RestController
@RequestMapping("/api/redis")
@RequiredArgsConstructor
public class RedisOpsController {

    private final RedisOpsService redisOpsService;

    @PostMapping("/string/{key}")
    public RafResult<Void> stringDemo(@PathVariable String key, @RequestParam String value) {
        redisOpsService.stringDemo(key, value);
        return RafResult.success();
    }

    @PostMapping("/hash/{key}")
    public RafResult<Void> hashDemo(@PathVariable String key) {
        redisOpsService.hashDemo(key);
        return RafResult.success();
    }

    @PostMapping("/list/{key}")
    public RafResult<Void> listDemo(@PathVariable String key) {
        redisOpsService.listDemo(key);
        return RafResult.success();
    }

    @PostMapping("/set/{key}")
    public RafResult<Void> setDemo(@PathVariable String key) {
        redisOpsService.setDemo(key);
        return RafResult.success();
    }

    @PostMapping("/zset/{key}")
    public RafResult<Void> zsetDemo(@PathVariable String key) {
        redisOpsService.zsetDemo(key);
        return RafResult.success();
    }
}
```

- [ ] **Step 7: 创建 StockController 和 StockDeductReq**

文件：`src/main/java/io/github/jerryraf/examples/redis/dto/StockDeductReq.java`

```java
package io.github.jerryraf.examples.redis.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StockDeductReq {
    @NotNull
    private Long productId;
    @Min(1)
    private int quantity;
}
```

文件：`src/main/java/io/github/jerryraf/examples/redis/controller/StockController.java`

```java
package io.github.jerryraf.examples.redis.controller;

import com.raf.framework.core.common.result.RafResult;
import io.github.jerryraf.examples.redis.dto.StockDeductReq;
import io.github.jerryraf.examples.redis.service.StockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * REST interface for distributed lock demo (stock deduction).
 *
 * @author Jerry
 */
@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @PostMapping("/init/{productId}")
    public RafResult<Void> initStock(@PathVariable Long productId,
                                     @RequestParam(defaultValue = "100") int stock) {
        stockService.initStock(productId, stock);
        return RafResult.success();
    }

    @GetMapping("/{productId}")
    public RafResult<Integer> getStock(@PathVariable Long productId) {
        return RafResult.success(stockService.getStock(productId));
    }

    @PostMapping("/deduct")
    public RafResult<Void> deductStock(@Valid @RequestBody StockDeductReq req) {
        stockService.deductStock(req.getProductId(), req.getQuantity());
        return RafResult.success();
    }
}
```

- [ ] **Step 8: 验证编译**

```bash
cd "D:/Framework/raf-framework/examples/raf-example-redis-starter"
mvn compile -s "D:/Program Files/apache-maven-3.9.10/conf/settings-raf.xml" -Dmaven.repo.local="D:/data/repository/local"
```

Expected: `BUILD SUCCESS`

- [ ] **Step 9: Commit**

```bash
cd "D:/Framework/raf-framework"
git add examples/raf-example-redis-starter/
git commit -m "feat(examples): add raf-example-redis-starter with common ops and distributed lock"
```

---

## Task 2: raf-example-rabbit-starter（重点）

**Files:**
- Create: `examples/raf-example-rabbit-starter/pom.xml`
- Create: `examples/raf-example-rabbit-starter/src/main/resources/application.yml`
- Create: `examples/raf-example-rabbit-starter/src/main/java/io/github/jerryraf/examples/rabbit/RabbitExampleApplication.java`
- Create: `examples/raf-example-rabbit-starter/src/main/java/io/github/jerryraf/examples/rabbit/config/RabbitNormalConfig.java`
- Create: `examples/raf-example-rabbit-starter/src/main/java/io/github/jerryraf/examples/rabbit/config/RabbitDelayConfig.java`
- Create: `examples/raf-example-rabbit-starter/src/main/java/io/github/jerryraf/examples/rabbit/dto/OrderMessage.java`
- Create: `examples/raf-example-rabbit-starter/src/main/java/io/github/jerryraf/examples/rabbit/producer/OrderNotifyProducer.java`
- Create: `examples/raf-example-rabbit-starter/src/main/java/io/github/jerryraf/examples/rabbit/producer/OrderDelayProducer.java`
- Create: `examples/raf-example-rabbit-starter/src/main/java/io/github/jerryraf/examples/rabbit/consumer/OrderNotifyConsumer.java`
- Create: `examples/raf-example-rabbit-starter/src/main/java/io/github/jerryraf/examples/rabbit/consumer/OrderTimeoutConsumer.java`
- Create: `examples/raf-example-rabbit-starter/src/main/java/io/github/jerryraf/examples/rabbit/service/IdempotentService.java`
- Create: `examples/raf-example-rabbit-starter/src/main/java/io/github/jerryraf/examples/rabbit/controller/OrderController.java`

- [ ] **Step 1: 创建 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>io.github.jerryraf</groupId>
        <artifactId>raf-framework-parent</artifactId>
        <version>${revision}</version>
        <relativePath>../../raf-framework-parent</relativePath>
    </parent>

    <groupId>io.github.jerryraf.examples</groupId>
    <artifactId>raf-example-rabbit-starter</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <name>RAF Example - RabbitMQ Starter</name>
    <description>Demonstrates normal message consumption with idempotency/exception handling, and delay queue via DLX+TTL</description>

    <dependencies>
        <dependency>
            <groupId>io.github.jerryraf</groupId>
            <artifactId>raf-framework-web-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>io.github.jerryraf</groupId>
            <artifactId>raf-framework-rabbit-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>io.github.jerryraf</groupId>
            <artifactId>raf-framework-redis-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: 创建 application.yml**

```yaml
spring:
  application:
    name: raf-example-rabbit-starter

server:
  port: 8080

raf:
  rabbit:
    addresses: ${RABBIT_ADDR:localhost:5672}
    username: ${RABBIT_USER:guest}
    password: ${RABBIT_PASS:guest}
    virtual-host: /
    provider:
      ack: true
    consumer:
      concurrent-consumers: 3
      max-concurrent-consumers: 10
    delay:
      dead-exchange: order.delay.dead.exchange
      receive-exchange: order.delay.receive.exchange
      queue-prefix:
        - order.timeout
    security:
      strict-type-validation: false
      max-retry-count: 3
  redis:
    enabled: true
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
  log:
    enabled: true
    level: REQ_BODY
```

- [ ] **Step 3: 创建 Application 启动类**

文件：`src/main/java/io/github/jerryraf/examples/rabbit/RabbitExampleApplication.java`

```java
package io.github.jerryraf.examples.rabbit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * RabbitMQ Starter Example Application.
 *
 * <p>Demonstrates:
 * <ul>
 *   <li>Normal message: Direct Exchange, manual ACK, idempotency (Redis msgId dedup), exception handling</li>
 *   <li>Delay queue: DLX + TTL pattern for order timeout cancellation</li>
 * </ul>
 *
 * @author Jerry
 */
@SpringBootApplication
public class RabbitExampleApplication {
    public static void main(String[] args) {
        SpringApplication.run(RabbitExampleApplication.class, args);
    }
}
```

- [ ] **Step 4: 创建 OrderMessage DTO**

文件：`src/main/java/io/github/jerryraf/examples/rabbit/dto/OrderMessage.java`

```java
package io.github.jerryraf.examples.rabbit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Order message payload.
 *
 * @author Jerry
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Order ID */
    private Long orderId;

    /** User ID */
    private Long userId;

    /** Order amount */
    private BigDecimal amount;

    /** Order status: CREATED / PAID / CANCELLED */
    private String status;
}
```

- [ ] **Step 5: 创建 RabbitNormalConfig（普通队列声明）**

文件：`src/main/java/io/github/jerryraf/examples/rabbit/config/RabbitNormalConfig.java`

```java
package io.github.jerryraf.examples.rabbit.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Normal queue configuration: Direct Exchange + queue + binding.
 *
 * <p>Topology:
 * <pre>
 * Producer → order.notify.exchange (Direct) → order.notify.queue → Consumer
 * </pre>
 *
 * @author Jerry
 */
@Configuration
public class RabbitNormalConfig {

    public static final String ORDER_NOTIFY_EXCHANGE = "order.notify.exchange";
    public static final String ORDER_NOTIFY_QUEUE    = "order.notify.queue";
    public static final String ORDER_NOTIFY_ROUTE    = "order.notify.route";

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
        return BindingBuilder
                .bind(orderNotifyQueue())
                .to(orderNotifyExchange())
                .with(ORDER_NOTIFY_ROUTE);
    }
}
```

- [ ] **Step 6: 创建 RabbitDelayConfig（延迟队列声明）**

文件：`src/main/java/io/github/jerryraf/examples/rabbit/config/RabbitDelayConfig.java`

```java
package io.github.jerryraf.examples.rabbit.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Delay queue configuration using DLX (Dead Letter Exchange) + TTL pattern.
 *
 * <p>Topology:
 * <pre>
 * Producer → order.delay.dead.exchange (Direct) → order.timeout.dead.queue (TTL=30min)
 *                                                        ↓ (on expiry, DLX routes to)
 *                                          order.delay.receive.exchange → order.timeout.queue → Consumer
 * </pre>
 *
 * @author Jerry
 */
@Configuration
public class RabbitDelayConfig {

    // Dead-letter exchange: messages are sent here initially with TTL
    public static final String DELAY_DEAD_EXCHANGE    = "order.delay.dead.exchange";
    // Receive exchange: messages arrive here after TTL expires
    public static final String DELAY_RECEIVE_EXCHANGE = "order.delay.receive.exchange";

    // TTL queue (30 min): messages sit here until they expire
    public static final String ORDER_TIMEOUT_DEAD_QUEUE    = "order.timeout.dead.queue";
    // Processing queue: receives expired messages for actual handling
    public static final String ORDER_TIMEOUT_QUEUE         = "order.timeout.queue";
    public static final String ORDER_TIMEOUT_ROUTE         = "order.timeout";

    /** 30 minutes in milliseconds */
    private static final int TTL_30_MIN_MS = 30 * 60 * 1000;

    @Bean
    public DirectExchange delayDeadExchange() {
        return ExchangeBuilder.directExchange(DELAY_DEAD_EXCHANGE).durable(true).build();
    }

    @Bean
    public DirectExchange delayReceiveExchange() {
        return ExchangeBuilder.directExchange(DELAY_RECEIVE_EXCHANGE).durable(true).build();
    }

    /**
     * TTL queue with DLX configured.
     * Messages that expire here are routed to DELAY_RECEIVE_EXCHANGE.
     */
    @Bean
    public Queue orderTimeoutDeadQueue() {
        Map<String, Object> args = new HashMap<>(4);
        // When message expires, route to receive exchange
        args.put("x-dead-letter-exchange", DELAY_RECEIVE_EXCHANGE);
        args.put("x-dead-letter-routing-key", ORDER_TIMEOUT_ROUTE);
        // Default TTL: 30 minutes (can be overridden per-message)
        args.put("x-message-ttl", TTL_30_MIN_MS);
        return QueueBuilder.durable(ORDER_TIMEOUT_DEAD_QUEUE).withArguments(args).build();
    }

    /** Actual processing queue */
    @Bean
    public Queue orderTimeoutQueue() {
        return QueueBuilder.durable(ORDER_TIMEOUT_QUEUE).build();
    }

    @Bean
    public Binding orderTimeoutDeadBinding() {
        return BindingBuilder
                .bind(orderTimeoutDeadQueue())
                .to(delayDeadExchange())
                .with(ORDER_TIMEOUT_ROUTE + ".dead.route");
    }

    @Bean
    public Binding orderTimeoutBinding() {
        return BindingBuilder
                .bind(orderTimeoutQueue())
                .to(delayReceiveExchange())
                .with(ORDER_TIMEOUT_ROUTE);
    }
}
```

- [ ] **Step 7: 创建 IdempotentService（幂等校验）**

文件：`src/main/java/io/github/jerryraf/examples/rabbit/service/IdempotentService.java`

```java
package io.github.jerryraf.examples.rabbit.service;

import com.raf.framework.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Message idempotency service using Redis setIfAbsent.
 *
 * <p>Pattern: before processing, try to set a key with the msgId.
 * If the key already exists, the message was already processed — skip it.
 * TTL is set to 24h to cover any reasonable redelivery window.
 *
 * @author Jerry
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotentService {

    private static final String IDEMPOTENT_KEY_PREFIX = "mq:idempotent:";
    private static final long IDEMPOTENT_TTL_HOURS = 24;

    private final RedisService redisService;

    /**
     * Try to mark a message as being processed.
     *
     * @param msgId unique message ID
     * @return true if this is the first time processing (proceed), false if duplicate (skip)
     */
    public boolean tryMark(String msgId) {
        String key = IDEMPOTENT_KEY_PREFIX + msgId;
        boolean marked = redisService.setIfAbsent(key, "1", IDEMPOTENT_TTL_HOURS, TimeUnit.HOURS);
        if (!marked) {
            log.warn("Duplicate message detected, skipping: msgId={}", msgId);
        }
        return marked;
    }
}
```

- [ ] **Step 8: 创建 OrderNotifyConsumer（普通消费者：手动ACK + 幂等 + 异常处理）**

文件：`src/main/java/io/github/jerryraf/examples/rabbit/consumer/OrderNotifyConsumer.java`

```java
package io.github.jerryraf.examples.rabbit.consumer;

import com.raf.framework.core.jackson.JsonService;
import com.raf.framework.rabbit.AbstractRabbitConsumerListener;
import com.raf.framework.rabbit.RabbitMqConsumer;
import com.raf.framework.rabbit.RabbitMqMessage;
import io.github.jerryraf.examples.rabbit.config.RabbitNormalConfig;
import io.github.jerryraf.examples.rabbit.dto.OrderMessage;
import io.github.jerryraf.examples.rabbit.service.IdempotentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.stereotype.Component;

/**
 * Order notification consumer.
 *
 * <p>Demonstrates:
 * <ul>
 *   <li>Manual ACK via AbstractRabbitConsumerListener</li>
 *   <li>Idempotency: Redis setIfAbsent dedup by msgId</li>
 *   <li>Exception handling: BusinessException → nack no-requeue (→ DLQ); others → retry up to 3 times</li>
 * </ul>
 *
 * @author Jerry
 */
@Slf4j
@Component
@RabbitMqConsumer(
        exchange   = RabbitNormalConfig.ORDER_NOTIFY_EXCHANGE,
        routingKey = RabbitNormalConfig.ORDER_NOTIFY_ROUTE,
        queue      = RabbitNormalConfig.ORDER_NOTIFY_QUEUE,
        ackModel   = AcknowledgeMode.MANUAL
)
@RequiredArgsConstructor
public class OrderNotifyConsumer extends AbstractRabbitConsumerListener {

    private final IdempotentService idempotentService;
    private final JsonService jsonService;

    @Override
    public void onMessage(RabbitMqMessage rabbitMqMessage) {
        String msgId = rabbitMqMessage.getMsgId();
        log.info("Received order notify message: msgId={}", msgId);

        // Idempotency check: skip if already processed
        if (!idempotentService.tryMark(msgId)) {
            log.warn("Duplicate message, skipping: msgId={}", msgId);
            return;
        }

        // Parse payload
        OrderMessage order = jsonService.parse(rabbitMqMessage.getMessage(), OrderMessage.class);
        log.info("Processing order notify: orderId={}, status={}", order.getOrderId(), order.getStatus());

        // Business logic (simulated)
        processOrderNotification(order);

        log.info("Order notify processed successfully: msgId={}, orderId={}", msgId, order.getOrderId());
    }

    /**
     * Whether to retry on exception.
     * Return true to nack+requeue (retry), false to nack+no-requeue (→ DLQ).
     * BusinessException is non-retryable (data issue); others are retryable (transient).
     */
    @Override
    public boolean retry(RabbitMqMessage message) {
        // Retry on infrastructure/transient errors; give up on business errors
        return !message.isOverTimes();
    }

    private void processOrderNotification(OrderMessage order) {
        // Simulate: throw on invalid status to demonstrate exception handling
        if ("INVALID".equals(order.getStatus())) {
            throw new IllegalArgumentException("Invalid order status: " + order.getStatus());
        }
        log.info("Order notification sent to user: userId={}, orderId={}", order.getUserId(), order.getOrderId());
    }
}
```

- [ ] **Step 9: 创建 OrderTimeoutConsumer（延迟队列消费者）**

文件：`src/main/java/io/github/jerryraf/examples/rabbit/consumer/OrderTimeoutConsumer.java`

```java
package io.github.jerryraf.examples.rabbit.consumer;

import com.raf.framework.core.jackson.JsonService;
import com.raf.framework.rabbit.AbstractRabbitConsumerListener;
import com.raf.framework.rabbit.RabbitMqDelayConsumer;
import com.raf.framework.rabbit.RabbitMqMessage;
import io.github.jerryraf.examples.rabbit.config.RabbitDelayConfig;
import io.github.jerryraf.examples.rabbit.dto.OrderMessage;
import io.github.jerryraf.examples.rabbit.service.IdempotentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.stereotype.Component;

/**
 * Order timeout consumer — triggered after TTL expires in the dead-letter queue.
 *
 * <p>Demonstrates the DLX+TTL delay queue pattern:
 * message is sent to dead-letter queue with TTL, expires after 30 min,
 * then routed here for timeout cancellation processing.
 *
 * @author Jerry
 */
@Slf4j
@Component
@RabbitMqDelayConsumer(
        businessName = "order.timeout",
        exchange     = RabbitDelayConfig.DELAY_RECEIVE_EXCHANGE,
        ackModel     = AcknowledgeMode.MANUAL
)
@RequiredArgsConstructor
public class OrderTimeoutConsumer extends AbstractRabbitConsumerListener {

    private final IdempotentService idempotentService;
    private final JsonService jsonService;

    @Override
    public void onMessage(RabbitMqMessage rabbitMqMessage) {
        String msgId = rabbitMqMessage.getMsgId();
        log.info("Received order timeout message: msgId={}", msgId);

        if (!idempotentService.tryMark(msgId)) {
            log.warn("Duplicate timeout message, skipping: msgId={}", msgId);
            return;
        }

        OrderMessage order = jsonService.parse(rabbitMqMessage.getMessage(), OrderMessage.class);
        log.info("Processing order timeout cancellation: orderId={}", order.getOrderId());

        cancelOrder(order);

        log.info("Order timeout cancelled: msgId={}, orderId={}", msgId, order.getOrderId());
    }

    @Override
    public boolean retry(RabbitMqMessage message) {
        return !message.isOverTimes();
    }

    private void cancelOrder(OrderMessage order) {
        // Simulate order cancellation logic
        log.info("Order {} cancelled due to payment timeout (userId={})",
                order.getOrderId(), order.getUserId());
    }
}
```

- [ ] **Step 10: 创建 OrderNotifyProducer 和 OrderDelayProducer**

文件：`src/main/java/io/github/jerryraf/examples/rabbit/producer/OrderNotifyProducer.java`

```java
package io.github.jerryraf.examples.rabbit.producer;

import com.raf.framework.core.jackson.JsonService;
import com.raf.framework.core.snowflake.SnowFlakeBuilder;
import com.raf.framework.rabbit.RabbitMqMessage;
import com.raf.framework.rabbit.RabbitMqMessageSender;
import io.github.jerryraf.examples.rabbit.config.RabbitNormalConfig;
import io.github.jerryraf.examples.rabbit.dto.OrderMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Sends order notification messages to the normal queue.
 *
 * @author Jerry
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderNotifyProducer {

    private final RabbitMqMessageSender sender;
    private final JsonService jsonService;

    public void send(OrderMessage order) {
        RabbitMqMessage msg = new RabbitMqMessage();
        msg.setMsgId(SnowFlakeBuilder.generateId());
        msg.setMessage(jsonService.toJson(order));

        log.info("Sending order notify: msgId={}, orderId={}", msg.getMsgId(), order.getOrderId());
        sender.send(msg, RabbitNormalConfig.ORDER_NOTIFY_EXCHANGE, RabbitNormalConfig.ORDER_NOTIFY_ROUTE);
    }
}
```

文件：`src/main/java/io/github/jerryraf/examples/rabbit/producer/OrderDelayProducer.java`

```java
package io.github.jerryraf.examples.rabbit.producer;

import com.raf.framework.core.jackson.JsonService;
import com.raf.framework.core.snowflake.SnowFlakeBuilder;
import com.raf.framework.rabbit.RabbitMqMessage;
import com.raf.framework.rabbit.RabbitMqMessageSender;
import io.github.jerryraf.examples.rabbit.dto.OrderMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Sends order timeout delay messages via DLX+TTL pattern.
 *
 * @author Jerry
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderDelayProducer {

    /** businessName must match the queue prefix configured in raf.rabbit.delay.queue-prefix */
    private static final String BUSINESS_NAME = "order.timeout";
    /** Default payment timeout: 30 minutes */
    private static final int DEFAULT_TIMEOUT_SECONDS = 30 * 60;

    private final RabbitMqMessageSender sender;
    private final JsonService jsonService;

    public void sendTimeout(OrderMessage order) {
        sendTimeout(order, DEFAULT_TIMEOUT_SECONDS);
    }

    public void sendTimeout(OrderMessage order, int delaySeconds) {
        RabbitMqMessage msg = new RabbitMqMessage();
        msg.setMsgId(SnowFlakeBuilder.generateId());
        msg.setMessage(jsonService.toJson(order));

        log.info("Sending order timeout delay: msgId={}, orderId={}, delaySeconds={}",
                msg.getMsgId(), order.getOrderId(), delaySeconds);
        sender.sendDelay(msg, BUSINESS_NAME, delaySeconds);
    }
}
```

- [ ] **Step 11: 创建 OrderController（触发发送的 REST 接口）**

文件：`src/main/java/io/github/jerryraf/examples/rabbit/controller/OrderController.java`

```java
package io.github.jerryraf.examples.rabbit.controller;

import com.raf.framework.autoconfigure.common.result.RafResult;
import io.github.jerryraf.examples.rabbit.dto.OrderMessage;
import io.github.jerryraf.examples.rabbit.producer.OrderDelayProducer;
import io.github.jerryraf.examples.rabbit.producer.OrderNotifyProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * REST interface to trigger RabbitMQ message sending for demo purposes.
 *
 * @author Jerry
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderNotifyProducer notifyProducer;
    private final OrderDelayProducer delayProducer;

    /**
     * Send a normal order notification message.
     * Consumer will process it immediately with idempotency check.
     */
    @PostMapping("/{orderId}/notify")
    public RafResult<Void> sendNotify(@PathVariable Long orderId) {
        OrderMessage order = OrderMessage.builder()
                .orderId(orderId)
                .userId(1001L)
                .amount(new BigDecimal("99.00"))
                .status("PAID")
                .build();
        notifyProducer.send(order);
        return RafResult.success();
    }

    /**
     * Send a delay message for order timeout cancellation.
     * Consumer will be triggered after TTL expires (default 30 min, demo uses 10s).
     */
    @PostMapping("/{orderId}/timeout")
    public RafResult<Void> sendTimeout(@PathVariable Long orderId,
                                       @RequestParam(defaultValue = "10") int delaySeconds) {
        OrderMessage order = OrderMessage.builder()
                .orderId(orderId)
                .userId(1001L)
                .amount(new BigDecimal("99.00"))
                .status("CREATED")
                .build();
        delayProducer.sendTimeout(order, delaySeconds);
        return RafResult.success();
    }
}
```

- [ ] **Step 12: 验证编译**

```bash
cd "D:/Framework/raf-framework/examples/raf-example-rabbit-starter"
mvn compile -s "D:/Program Files/apache-maven-3.9.10/conf/settings-raf.xml" -Dmaven.repo.local="D:/data/repository/local"
```

Expected: `BUILD SUCCESS`

- [ ] **Step 13: Commit**

```bash
cd "D:/Framework/raf-framework"
git add examples/raf-example-rabbit-starter/
git commit -m "feat(examples): add raf-example-rabbit-starter with normal/delay queue, idempotency, exception handling"
```

---

## Task 3: raf-example-mybatis-starter（重写包名 + 内容增强）

**Files:**
- Modify: `examples/raf-example-mybatis-starter/pom.xml`
- Modify: `examples/raf-example-mybatis-starter/src/main/resources/bootstrap.yml`
- Rename+Modify: all `.java` files (package `com.github.raf.examples.mybatis` → `io.github.jerryraf.examples.mybatis`)
- Modify: `examples/raf-example-mybatis-starter/src/main/java/io/github/jerryraf/examples/mybatis/service/UserService.java`
- Modify: `examples/raf-example-mybatis-starter/src/main/resources/mapper/UserMapper.xml`
- Create: `examples/raf-example-mybatis-starter/src/main/java/io/github/jerryraf/examples/mybatis/dto/UserPageReq.java`

- [ ] **Step 1: 查看现有 pom.xml 结构**

```bash
cat "D:/Framework/raf-framework/examples/raf-example-mybatis-starter/pom.xml"
```

Expected: 看到 `<parent>` 是否已正确继承 `raf-framework-parent`，`<artifactId>` 是否已改为 `raf-example-mybatis-starter`。
- [ ] **Step 2: 修正 pom.xml（去掉独立 dependencyManagement，改继承 parent）**

文件：`examples/raf-example-mybatis-starter/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>io.github.jerryraf</groupId>
        <artifactId>raf-framework-parent</artifactId>
        <version>${revision}</version>
        <relativePath>../../raf-framework-parent</relativePath>
    </parent>

    <groupId>io.github.jerryraf.examples</groupId>
    <artifactId>raf-example-mybatis-starter</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <name>RAF Example - MyBatis Starter</name>
    <description>Demonstrates CRUD, pagination, transaction propagation, and read-write splitting</description>

    <dependencies>
        <dependency>
            <groupId>io.github.jerryraf</groupId>
            <artifactId>raf-framework-web-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>io.github.jerryraf</groupId>
            <artifactId>raf-framework-mybatis-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>io.github.jerryraf</groupId>
            <artifactId>raf-framework-datasource-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 3: 修正所有 Java 文件包名**

将所有 `package com.github.raf.examples.mybatis` 改为 `package io.github.jerryraf.examples.mybatis`，
将所有 `import com.github.raf.examples.mybatis` 改为 `import io.github.jerryraf.examples.mybatis`。

涉及文件（逐一用 Edit 工具修改）：
- `MybatisExampleApplication.java`
- `config/DataSourceConfig.java`
- `config/MyBatisConfig.java`
- `controller/UserController.java`
- `service/UserService.java`
- `dao/UserMapper.java`
- `entity/User.java`
- `dto/UserCreateReq.java`
- `dto/UserUpdateReq.java`

同时将 Java 目录从 `src/main/java/com/github/raf/examples/mybatis/` 重命名为 `src/main/java/io/github/jerryraf/examples/mybatis/`。

- [ ] **Step 4: 修正 bootstrap.yml 中的 spring.application.name**

文件：`examples/raf-example-mybatis-starter/src/main/resources/bootstrap.yml`

```yaml
spring:
  application:
    name: raf-example-mybatis-starter
  datasource:
    master:
      url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL
      username: sa
      password:
      driver-class-name: org.h2.Driver
    slave:
      url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL
      username: sa
      password:
      driver-class-name: org.h2.Driver
  h2:
    console:
      enabled: true
      path: /h2-console
  sql:
    init:
      mode: always
      schema-locations: classpath:schema.sql
      data-locations: classpath:data.sql

server:
  port: 8080

mybatis-plus:
  mapper-locations: classpath:mapper/*.xml
  global-config:
    db-config:
      logic-delete-field: isDeleted
      logic-delete-value: 1
      logic-not-delete-value: 0

raf:
  log:
    enabled: true
    level: REQ_BODY
```

- [ ] **Step 5: 创建 schema.sql 和 data.sql（H2 初始化）**

文件：`src/main/resources/schema.sql`

```sql
CREATE TABLE IF NOT EXISTS t_user (
    id          BIGINT       NOT NULL PRIMARY KEY,
    username    VARCHAR(64)  NOT NULL,
    email       VARCHAR(128) NOT NULL,
    age         INT          NOT NULL DEFAULT 0,
    is_deleted  TINYINT      NOT NULL DEFAULT 0,
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

文件：`src/main/resources/data.sql`

```sql
INSERT INTO t_user (id, username, email, age) VALUES
(1, 'alice', 'alice@example.com', 28),
(2, 'bob',   'bob@example.com',   32),
(3, 'carol', 'carol@example.com', 25);
```

- [ ] **Step 6: 增强 UserService（事务传播行为演示）**

文件：`src/main/java/io/github/jerryraf/examples/mybatis/service/UserService.java`

```java
package io.github.jerryraf.examples.mybatis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.raf.framework.core.common.exception.BusinessException;
import com.raf.framework.core.common.result.RafResponseEnum;
import io.github.jerryraf.examples.mybatis.dao.UserMapper;
import io.github.jerryraf.examples.mybatis.dto.UserCreateReq;
import io.github.jerryraf.examples.mybatis.dto.UserPageReq;
import io.github.jerryraf.examples.mybatis.dto.UserUpdateReq;
import io.github.jerryraf.examples.mybatis.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * User service demonstrating CRUD, pagination, and transaction propagation.
 *
 * @author Jerry
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    // ===== CRUD =====

    @Transactional(rollbackFor = Exception.class)
    public Long createUser(UserCreateReq req) {
        User user = new User();
        user.setUsername(req.getUsername());
        user.setEmail(req.getEmail());
        user.setAge(req.getAge());
        userMapper.insert(user);
        log.info("User created: id={}", user.getId());
        return user.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateUser(UserUpdateReq req) {
        User user = userMapper.selectById(req.getId());
        if (user == null) {
            throw new BusinessException(RafResponseEnum.PARAM_ERROR, "User not found: " + req.getId());
        }
        user.setEmail(req.getEmail());
        user.setAge(req.getAge());
        userMapper.updateById(user);
        log.info("User updated: id={}", req.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        int rows = userMapper.deleteById(id);
        if (rows == 0) {
            throw new BusinessException(RafResponseEnum.PARAM_ERROR, "User not found: " + id);
        }
        log.info("User deleted (logical): id={}", id);
    }

    public User getUser(Long id) {
        return userMapper.selectById(id);
    }

    // ===== Pagination =====

    public Page<User> pageUsers(UserPageReq req) {
        Page<User> page = new Page<>(req.getPageNum(), req.getPageSize());
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .like(req.getUsername() != null, User::getUsername, req.getUsername())
                .orderByDesc(User::getId);
        return userMapper.selectPage(page, wrapper);
    }

    // ===== Transaction Propagation Demo =====

    /**
     * REQUIRED (default): joins the outer transaction.
     * If this method throws, the outer transaction also rolls back.
     */
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void createUserRequired(UserCreateReq req) {
        createUser(req);
        log.info("createUserRequired: joined outer transaction");
    }

    /**
     * REQUIRES_NEW: suspends the outer transaction and starts a new one.
     * Even if the outer transaction rolls back, this commit is preserved.
     */
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void createUserRequiresNew(UserCreateReq req) {
        createUser(req);
        log.info("createUserRequiresNew: independent transaction committed");
    }

    /**
     * Demonstrates REQUIRES_NEW isolation:
     * - createUserRequiresNew commits independently
     * - then throws to roll back the outer transaction
     * - result: the REQUIRES_NEW user persists, the outer user does not
     */
    @Transactional(rollbackFor = Exception.class)
    public void demonstratePropagation(UserCreateReq outerReq, UserCreateReq innerReq) {
        createUserRequired(outerReq);          // joins this transaction
        createUserRequiresNew(innerReq);       // independent transaction, commits immediately
        log.info("About to throw — outer transaction will roll back, inner already committed");
        throw new BusinessException(RafResponseEnum.SERVER_ERROR, "Simulated failure to demonstrate REQUIRES_NEW");
    }
}
```

- [ ] **Step 7: 创建 UserPageReq DTO**

文件：`src/main/java/io/github/jerryraf/examples/mybatis/dto/UserPageReq.java`

```java
package io.github.jerryraf.examples.mybatis.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * Pagination request for user list.
 *
 * @author Jerry
 */
@Data
public class UserPageReq {

    @Min(1)
    private int pageNum = 1;

    @Min(1)
    @Max(100)
    private int pageSize = 10;

    /** Optional: filter by username (LIKE) */
    private String username;
}
```

- [ ] **Step 8: 增强 UserMapper.xml（自定义分页 SQL）**

文件：`src/main/resources/mapper/UserMapper.xml`

在现有 mapper 文件中追加以下 SQL（如文件不存在则新建）：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="io.github.jerryraf.examples.mybatis.dao.UserMapper">

    <!-- Custom pagination query with dynamic conditions -->
    <select id="selectPageByCondition" resultType="io.github.jerryraf.examples.mybatis.entity.User">
        SELECT id, username, email, age, is_deleted, create_time, update_time
        FROM t_user
        WHERE is_deleted = 0
        <if test="username != null and username != ''">
            AND username LIKE CONCAT('%', #{username}, '%')
        </if>
        ORDER BY id DESC
    </select>

</mapper>
```

- [ ] **Step 9: 验证编译**

```bash
cd "D:/Framework/raf-framework/examples/raf-example-mybatis-starter"
mvn compile -s "D:/Program Files/apache-maven-3.9.10/conf/settings-raf.xml" -Dmaven.repo.local="D:/data/repository/local"
```

Expected: `BUILD SUCCESS`

- [ ] **Step 10: Commit**

```bash
cd "D:/Framework/raf-framework"
git add examples/raf-example-mybatis-starter/
git commit -m "refactor(examples): fix mybatis package name, rewrite pom, add pagination and tx propagation demo"
```

---

## Task 4: raf-example-nacos-starter（新建）

**Files:**
- Create: `examples/raf-example-nacos-starter/pom.xml`
- Create: `examples/raf-example-nacos-starter/src/main/resources/bootstrap.yml`
- Create: `examples/raf-example-nacos-starter/src/main/java/io/github/jerryraf/examples/nacos/NacosExampleApplication.java`
- Create: `examples/raf-example-nacos-starter/src/main/java/io/github/jerryraf/examples/nacos/config/DynamicConfig.java`
- Create: `examples/raf-example-nacos-starter/src/main/java/io/github/jerryraf/examples/nacos/controller/ConfigController.java`

- [ ] **Step 1: 创建 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>io.github.jerryraf</groupId>
        <artifactId>raf-framework-parent</artifactId>
        <version>${revision}</version>
        <relativePath>../../raf-framework-parent</relativePath>
    </parent>

    <groupId>io.github.jerryraf.examples</groupId>
    <artifactId>raf-example-nacos-starter</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <name>RAF Example - Nacos Starter</name>
    <description>Demonstrates Nacos config center (dynamic refresh) and service discovery</description>

    <dependencies>
        <dependency>
            <groupId>io.github.jerryraf</groupId>
            <artifactId>raf-framework-web-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>io.github.jerryraf</groupId>
            <artifactId>raf-framework-nacos-config-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>io.github.jerryraf</groupId>
            <artifactId>raf-framework-nacos-discovery-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: 创建 bootstrap.yml**

```yaml
spring:
  application:
    name: raf-example-nacos-starter
  cloud:
    nacos:
      config:
        server-addr: ${NACOS_ADDR:localhost:8848}
        namespace: ${NACOS_NAMESPACE:}
        group: DEFAULT_GROUP
        file-extension: yaml
        refresh-enabled: true
      discovery:
        server-addr: ${NACOS_ADDR:localhost:8848}
        namespace: ${NACOS_NAMESPACE:}

server:
  port: 8080

# Local fallback config (overridden by Nacos when connected)
app:
  title: "RAF Example (local fallback)"
  version: "1.0.0"

raf:
  log:
    enabled: true
    level: REQ_BODY
```

- [ ] **Step 3: 创建 Application 启动类**

文件：`src/main/java/io/github/jerryraf/examples/nacos/NacosExampleApplication.java`

```java
package io.github.jerryraf.examples.nacos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Nacos Starter Example Application.
 *
 * <p>Demonstrates:
 * <ul>
 *   <li>Nacos Config: dynamic property refresh via @RefreshScope</li>
 *   <li>Nacos Discovery: service registration and discovery</li>
 * </ul>
 *
 * @author Jerry
 */
@SpringBootApplication
@EnableDiscoveryClient
public class NacosExampleApplication {
    public static void main(String[] args) {
        SpringApplication.run(NacosExampleApplication.class, args);
    }
}
```

- [ ] **Step 4: 创建 DynamicConfig（@RefreshScope 演示）**

文件：`src/main/java/io/github/jerryraf/examples/nacos/config/DynamicConfig.java`

```java
package io.github.jerryraf.examples.nacos.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * Holds properties that are dynamically refreshed from Nacos config center.
 *
 * <p>When the Nacos config is updated, Spring Cloud refreshes this bean
 * automatically without restarting the application.
 *
 * @author Jerry
 */
@Getter
@Component
@RefreshScope
public class DynamicConfig {

    @Value("${app.title:RAF Example (local fallback)}")
    private String title;

    @Value("${app.version:1.0.0}")
    private String version;
}
```

- [ ] **Step 5: 创建 ConfigController**

文件：`src/main/java/io/github/jerryraf/examples/nacos/controller/ConfigController.java`

```java
package io.github.jerryraf.examples.nacos.controller;

import com.raf.framework.core.common.result.RafResult;
import io.github.jerryraf.examples.nacos.config.DynamicConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Exposes dynamic config values for verification.
 *
 * <p>Test hot-reload: update app.title in Nacos console, then call GET /config/info again.
 *
 * @author Jerry
 */
@RestController
@RequestMapping("/config")
@RequiredArgsConstructor
public class ConfigController {

    private final DynamicConfig dynamicConfig;

    @GetMapping("/info")
    public RafResult<Map<String, String>> getConfigInfo() {
        return RafResult.success(Map.of(
                "title",   dynamicConfig.getTitle(),
                "version", dynamicConfig.getVersion()
        ));
    }
}
```

- [ ] **Step 6: 验证编译**

```bash
cd "D:/Framework/raf-framework/examples/raf-example-nacos-starter"
mvn compile -s "D:/Program Files/apache-maven-3.9.10/conf/settings-raf.xml" -Dmaven.repo.local="D:/data/repository/local"
```

Expected: `BUILD SUCCESS`

- [ ] **Step 7: Commit**

```bash
cd "D:/Framework/raf-framework"
git add examples/raf-example-nacos-starter/
git commit -m "feat(examples): add raf-example-nacos-starter with dynamic config refresh and service discovery"
```

---

## Task 5: raf-example-mongodb-starter（新建）

**Files:**
- Create: `examples/raf-example-mongodb-starter/pom.xml`
- Create: `examples/raf-example-mongodb-starter/src/main/resources/application.yml`
- Create: `examples/raf-example-mongodb-starter/src/main/java/io/github/jerryraf/examples/mongodb/MongodbExampleApplication.java`
- Create: `examples/raf-example-mongodb-starter/src/main/java/io/github/jerryraf/examples/mongodb/document/ArticleDocument.java`
- Create: `examples/raf-example-mongodb-starter/src/main/java/io/github/jerryraf/examples/mongodb/repository/ArticleRepository.java`
- Create: `examples/raf-example-mongodb-starter/src/main/java/io/github/jerryraf/examples/mongodb/service/ArticleService.java`
- Create: `examples/raf-example-mongodb-starter/src/main/java/io/github/jerryraf/examples/mongodb/controller/ArticleController.java`

- [ ] **Step 1: 创建 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>io.github.jerryraf</groupId>
        <artifactId>raf-framework-parent</artifactId>
        <version>${revision}</version>
        <relativePath>../../raf-framework-parent</relativePath>
    </parent>
    <groupId>io.github.jerryraf.examples</groupId>
    <artifactId>raf-example-mongodb-starter</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>
    <name>RAF Example - MongoDB Starter</name>
    <description>Demonstrates MongoDB document CRUD and query via Spring Data MongoDB</description>
    <dependencies>
        <dependency>
            <groupId>io.github.jerryraf</groupId>
            <artifactId>raf-framework-web-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-mongodb</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: 创建 application.yml**

```yaml
spring:
  application:
    name: raf-example-mongodb-starter
  data:
    mongodb:
      uri: ${MONGO_URI:mongodb://localhost:27017/raf_example}
server:
  port: 8080
raf:
  log:
    enabled: true
    level: REQ_BODY
```

- [ ] **Step 3: 创建 Application 启动类**

```java
package io.github.jerryraf.examples.mongodb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MongodbExampleApplication {
    public static void main(String[] args) {
        SpringApplication.run(MongodbExampleApplication.class, args);
    }
}
```

- [ ] **Step 4: 创建 ArticleDocument**

```java
package io.github.jerryraf.examples.mongodb.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Document(collection = "articles")
public class ArticleDocument {
    @Id private String id;
    @Indexed(unique = true) private String title;
    private String content;
    private String author;
    private List<String> tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 5: 创建 ArticleRepository**

```java
package io.github.jerryraf.examples.mongodb.repository;

import io.github.jerryraf.examples.mongodb.document.ArticleDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface ArticleRepository extends MongoRepository<ArticleDocument, String> {
    Optional<ArticleDocument> findByTitle(String title);
    List<ArticleDocument> findByAuthor(String author);
    List<ArticleDocument> findByTagsContaining(String tag);
}
```

- [ ] **Step 6: 创建 ArticleService**

文件：`src/main/java/io/github/jerryraf/examples/mongodb/service/ArticleService.java`

```java
package io.github.jerryraf.examples.mongodb.service;

import com.raf.framework.core.common.exception.BusinessException;
import com.raf.framework.core.common.result.RafResponseEnum;
import io.github.jerryraf.examples.mongodb.document.ArticleDocument;
import io.github.jerryraf.examples.mongodb.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Article service demonstrating MongoDB CRUD operations.
 *
 * @author Jerry
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;

    public ArticleDocument create(String title, String content, String author, List<String> tags) {
        ArticleDocument doc = ArticleDocument.builder()
                .title(title)
                .content(content)
                .author(author)
                .tags(tags)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        ArticleDocument saved = articleRepository.save(doc);
        log.info("Article created: id={}, title={}", saved.getId(), saved.getTitle());
        return saved;
    }

    public ArticleDocument getById(String id) {
        return articleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(RafResponseEnum.PARAM_ERROR, "Article not found: " + id));
    }

    public List<ArticleDocument> getByAuthor(String author) {
        return articleRepository.findByAuthor(author);
    }

    public List<ArticleDocument> getByTag(String tag) {
        return articleRepository.findByTagsContaining(tag);
    }

    public void delete(String id) {
        if (!articleRepository.existsById(id)) {
            throw new BusinessException(RafResponseEnum.PARAM_ERROR, "Article not found: " + id);
        }
        articleRepository.deleteById(id);
        log.info("Article deleted: id={}", id);
    }
}
```

- [ ] **Step 7: 创建 ArticleController**

文件：`src/main/java/io/github/jerryraf/examples/mongodb/controller/ArticleController.java`

```java
package io.github.jerryraf.examples.mongodb.controller;

import com.raf.framework.core.common.result.RafResult;
import io.github.jerryraf.examples.mongodb.document.ArticleDocument;
import io.github.jerryraf.examples.mongodb.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Article REST controller.
 *
 * @author Jerry
 */
@RestController
@RequestMapping("/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @PostMapping
    public RafResult<ArticleDocument> create(@RequestParam String title,
                                              @RequestParam String content,
                                              @RequestParam String author,
                                              @RequestParam(required = false) List<String> tags) {
        return RafResult.success(articleService.create(title, content, author, tags));
    }

    @GetMapping("/{id}")
    public RafResult<ArticleDocument> getById(@PathVariable String id) {
        return RafResult.success(articleService.getById(id));
    }

    @GetMapping("/author/{author}")
    public RafResult<List<ArticleDocument>> getByAuthor(@PathVariable String author) {
        return RafResult.success(articleService.getByAuthor(author));
    }

    @GetMapping("/tag/{tag}")
    public RafResult<List<ArticleDocument>> getByTag(@PathVariable String tag) {
        return RafResult.success(articleService.getByTag(tag));
    }

    @DeleteMapping("/{id}")
    public RafResult<Void> delete(@PathVariable String id) {
        articleService.delete(id);
        return RafResult.success();
    }
}
```

- [ ] **Step 8: 验证编译**

```bash
cd "D:/Framework/raf-framework/examples/raf-example-mongodb-starter"
mvn compile -s "D:/Program Files/apache-maven-3.9.10/conf/settings-raf.xml" -Dmaven.repo.local="D:/data/repository/local"
```

Expected: `BUILD SUCCESS`

- [ ] **Step 9: Commit**

```bash
cd "D:/Framework/raf-framework"
git add examples/raf-example-mongodb-starter/
git commit -m "feat(examples): add raf-example-mongodb-starter with document CRUD"
```

---

## Task 6: raf-example-elasticsearch-starter（新建）

**Files:**
- Create: `examples/raf-example-elasticsearch-starter/pom.xml`
- Create: `examples/raf-example-elasticsearch-starter/src/main/resources/application.yml`
- Create: `examples/raf-example-elasticsearch-starter/src/main/java/io/github/jerryraf/examples/elasticsearch/ElasticsearchExampleApplication.java`
- Create: `examples/raf-example-elasticsearch-starter/src/main/java/io/github/jerryraf/examples/elasticsearch/document/ProductDocument.java`
- Create: `examples/raf-example-elasticsearch-starter/src/main/java/io/github/jerryraf/examples/elasticsearch/repository/ProductRepository.java`
- Create: `examples/raf-example-elasticsearch-starter/src/main/java/io/github/jerryraf/examples/elasticsearch/service/ProductSearchService.java`
- Create: `examples/raf-example-elasticsearch-starter/src/main/java/io/github/jerryraf/examples/elasticsearch/controller/ProductSearchController.java`

- [ ] **Step 1: 创建 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>io.github.jerryraf</groupId>
        <artifactId>raf-framework-parent</artifactId>
        <version>${revision}</version>
        <relativePath>../../raf-framework-parent</relativePath>
    </parent>
    <groupId>io.github.jerryraf.examples</groupId>
    <artifactId>raf-example-elasticsearch-starter</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>
    <name>RAF Example - Elasticsearch Starter</name>
    <description>Demonstrates full-text search and index management via Spring Data Elasticsearch</description>
    <dependencies>
        <dependency>
            <groupId>io.github.jerryraf</groupId>
            <artifactId>raf-framework-web-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>io.github.jerryraf</groupId>
            <artifactId>raf-framework-elasticsearch-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: 创建 application.yml**

```yaml
spring:
  application:
    name: raf-example-elasticsearch-starter
  elasticsearch:
    uris: ${ES_URI:http://localhost:9200}
    username: ${ES_USER:}
    password: ${ES_PASS:}
server:
  port: 8080
raf:
  log:
    enabled: true
    level: REQ_BODY
```

- [ ] **Step 3: 创建 Application 启动类**

```java
package io.github.jerryraf.examples.elasticsearch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ElasticsearchExampleApplication {
    public static void main(String[] args) {
        SpringApplication.run(ElasticsearchExampleApplication.class, args);
    }
}
```

- [ ] **Step 4: 创建 ProductDocument**

```java
package io.github.jerryraf.examples.elasticsearch.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Document(indexName = "products")
public class ProductDocument {
    @Id private String id;
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String name;
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String description;
    @Field(type = FieldType.Keyword) private String category;
    @Field(type = FieldType.Double)  private BigDecimal price;
    @Field(type = FieldType.Integer) private Integer stock;
}
```

- [ ] **Step 5: 创建 ProductRepository**

```java
package io.github.jerryraf.examples.elasticsearch.repository;

import io.github.jerryraf.examples.elasticsearch.document.ProductDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import java.util.List;

public interface ProductRepository extends ElasticsearchRepository<ProductDocument, String> {
    List<ProductDocument> findByCategory(String category);
    List<ProductDocument> findByNameContaining(String keyword);
}
```

- [ ] **Step 6: 创建 ProductSearchService**

```java
package io.github.jerryraf.examples.elasticsearch.service;

import com.raf.framework.core.common.exception.BusinessException;
import com.raf.framework.core.common.result.RafResponseEnum;
import io.github.jerryraf.examples.elasticsearch.document.ProductDocument;
import io.github.jerryraf.examples.elasticsearch.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSearchService {

    private final ProductRepository productRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    public ProductDocument save(String name, String desc, String category, BigDecimal price, int stock) {
        ProductDocument doc = ProductDocument.builder()
                .name(name).description(desc).category(category)
                .price(price).stock(stock).build();
        ProductDocument saved = productRepository.save(doc);
        log.info("Product indexed: id={}", saved.getId());
        return saved;
    }

    public ProductDocument getById(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(RafResponseEnum.PARAM_ERROR, "Product not found: " + id));
    }

    public List<ProductDocument> searchByKeyword(String keyword) {
        CriteriaQuery query = new CriteriaQuery(
                new Criteria("name").contains(keyword)
                        .or(new Criteria("description").contains(keyword))
        );
        return elasticsearchOperations.search(query, ProductDocument.class)
                .stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
    }

    public List<ProductDocument> getByCategory(String category) {
        return productRepository.findByCategory(category);
    }

    public void delete(String id) {
        productRepository.deleteById(id);
        log.info("Product deleted from index: id={}", id);
    }
}
```

- [ ] **Step 7: 创建 ProductSearchController**

```java
package io.github.jerryraf.examples.elasticsearch.controller;

import com.raf.framework.core.common.result.RafResult;
import io.github.jerryraf.examples.elasticsearch.document.ProductDocument;
import io.github.jerryraf.examples.elasticsearch.service.ProductSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductSearchController {

    private final ProductSearchService productSearchService;

    @PostMapping
    public RafResult<ProductDocument> save(@RequestParam String name,
                                            @RequestParam String description,
                                            @RequestParam String category,
                                            @RequestParam BigDecimal price,
                                            @RequestParam int stock) {
        return RafResult.success(productSearchService.save(name, description, category, price, stock));
    }

    @GetMapping("/{id}")
    public RafResult<ProductDocument> getById(@PathVariable String id) {
        return RafResult.success(productSearchService.getById(id));
    }

    @GetMapping("/search")
    public RafResult<List<ProductDocument>> search(@RequestParam String keyword) {
        return RafResult.success(productSearchService.searchByKeyword(keyword));
    }

    @GetMapping("/category/{category}")
    public RafResult<List<ProductDocument>> getByCategory(@PathVariable String category) {
        return RafResult.success(productSearchService.getByCategory(category));
    }

    @DeleteMapping("/{id}")
    public RafResult<Void> delete(@PathVariable String id) {
        productSearchService.delete(id);
        return RafResult.success();
    }
}
```

- [ ] **Step 8: 验证编译**

```bash
cd "D:/Framework/raf-framework/examples/raf-example-elasticsearch-starter"
mvn compile -s "D:/Program Files/apache-maven-3.9.10/conf/settings-raf.xml" -Dmaven.repo.local="D:/data/repository/local"
```

Expected: `BUILD SUCCESS`

- [ ] **Step 9: Commit**

```bash
cd "D:/Framework/raf-framework"
git add examples/raf-example-elasticsearch-starter/
git commit -m "feat(examples): add raf-example-elasticsearch-starter with full-text search"
```

---

## Task 7: raf-example-openapi-starter（新建）

**Files:**
- Create: `examples/raf-example-openapi-starter/pom.xml`
- Create: `examples/raf-example-openapi-starter/src/main/resources/application.yml`
- Create: `examples/raf-example-openapi-starter/src/main/java/io/github/jerryraf/examples/openapi/OpenApiExampleApplication.java`
- Create: `examples/raf-example-openapi-starter/src/main/java/io/github/jerryraf/examples/openapi/config/OpenApiConfig.java`
- Create: `examples/raf-example-openapi-starter/src/main/java/io/github/jerryraf/examples/openapi/controller/UserApiController.java`
- Create: `examples/raf-example-openapi-starter/src/main/java/io/github/jerryraf/examples/openapi/dto/UserDto.java`

- [ ] **Step 1: 创建 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>io.github.jerryraf</groupId>
        <artifactId>raf-framework-parent</artifactId>
        <version>${revision}</version>
        <relativePath>../../raf-framework-parent</relativePath>
    </parent>
    <groupId>io.github.jerryraf.examples</groupId>
    <artifactId>raf-example-openapi-starter</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>
    <name>RAF Example - OpenAPI Starter</name>
    <description>Demonstrates Springdoc OpenAPI 3 integration with raf-framework-web-starter</description>
    <dependencies>
        <dependency>
            <groupId>io.github.jerryraf</groupId>
            <artifactId>raf-framework-web-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: 创建 application.yml**

```yaml
spring:
  application:
    name: raf-example-openapi-starter
server:
  port: 8080
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
    operations-sorter: alpha
raf:
  log:
    enabled: true
    level: REQ_BODY
```

- [ ] **Step 3: 创建 Application 启动类**

```java
package io.github.jerryraf.examples.openapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OpenApiExampleApplication {
    public static void main(String[] args) {
        SpringApplication.run(OpenApiExampleApplication.class, args);
    }
}
```

- [ ] **Step 4: 创建 OpenApiConfig**

```java
package io.github.jerryraf.examples.openapi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI rafExampleOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("RAF Framework Example API")
                        .description("Demonstrates Springdoc OpenAPI 3 integration with raf-framework-web-starter")
                        .version("1.0.0")
                        .contact(new Contact().name("Jerry").url("https://github.com/JerryRaf"))
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
```

- [ ] **Step 5: 创建 UserDto**

```java
package io.github.jerryraf.examples.openapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Schema(description = "User data transfer object")
public class UserDto {

    @Schema(description = "User ID", example = "1001")
    private Long id;

    @NotBlank
    @Size(min = 2, max = 64)
    @Schema(description = "Username", example = "alice", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank
    @Email
    @Schema(description = "Email address", example = "alice@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @Min(1) @Max(150)
    @Schema(description = "Age", example = "28")
    private Integer age;
}
```

- [ ] **Step 6: 创建 UserApiController**

```java
package io.github.jerryraf.examples.openapi.controller;

import com.raf.framework.core.common.result.RafResult;
import io.github.jerryraf.examples.openapi.dto.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@RestController
@RequestMapping("/users")
@Tag(name = "User API", description = "User management endpoints")
public class UserApiController {

    private final Map<Long, UserDto> store = new ConcurrentHashMap<>();
    private final AtomicLong idGen = new AtomicLong(1000);

    @PostMapping
    @Operation(summary = "Create user", description = "Creates a new user and returns the created resource")
    public RafResult<UserDto> create(@Valid @RequestBody UserDto req) {
        req.setId(idGen.incrementAndGet());
        store.put(req.getId(), req);
        log.info("User created: id={}", req.getId());
        return RafResult.success(req);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    public RafResult<UserDto> getById(
            @Parameter(description = "User ID", required = true) @PathVariable Long id) {
        UserDto user = store.get(id);
        if (user == null) {
            return RafResult.fail("User not found: " + id);
        }
        return RafResult.success(user);
    }

    @GetMapping
    @Operation(summary = "List all users")
    public RafResult<List<UserDto>> listAll() {
        return RafResult.success(List.copyOf(store.values()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user by ID")
    public RafResult<Void> delete(@PathVariable Long id) {
        store.remove(id);
        return RafResult.success();
    }
}
```

- [ ] **Step 7: 验证编译**

```bash
cd "D:/Framework/raf-framework/examples/raf-example-openapi-starter"
mvn compile -s "D:/Program Files/apache-maven-3.9.10/conf/settings-raf.xml" -Dmaven.repo.local="D:/data/repository/local"
```

Expected: `BUILD SUCCESS`

- [ ] **Step 8: 验证 Swagger UI**

启动应用后访问 http://localhost:8080/swagger-ui.html，确认：
- API 文档正常渲染
- User API 的 4 个接口均可见
- Try it out 功能正常

- [ ] **Step 9: Commit**

```bash
cd "D:/Framework/raf-framework"
git add examples/raf-example-openapi-starter/
git commit -m "feat(examples): add raf-example-openapi-starter with Springdoc OpenAPI 3"
```

---

## Task 8: 更新根 pom.xml 的 modules 列表

将新建的 example 模块注册到根 `pom.xml` 的 `<modules>` 中（如果根 pom 管理 examples）。

- [ ] **Step 1: 检查根 pom.xml 是否包含 examples 子模块**

```bash
grep -n "raf-example" "D:/Framework/raf-framework/pom.xml"
```

- [ ] **Step 2: 若根 pom 管理 examples，追加新模块**

在 `<modules>` 中追加（使用 Edit 工具）：

```xml
<module>examples/raf-example-redis-starter</module>
<module>examples/raf-example-rabbit-starter</module>
<module>examples/raf-example-nacos-starter</module>
<module>examples/raf-example-mongodb-starter</module>
<module>examples/raf-example-elasticsearch-starter</module>
<module>examples/raf-example-openapi-starter</module>
```

- [ ] **Step 3: 验证根 pom 编译**

```bash
cd "D:/Framework/raf-framework"
mvn compile -pl examples/raf-example-redis-starter,examples/raf-example-rabbit-starter,examples/raf-example-nacos-starter,examples/raf-example-mongodb-starter,examples/raf-example-elasticsearch-starter,examples/raf-example-openapi-starter -am \
    -s "D:/Program Files/apache-maven-3.9.10/conf/settings-raf.xml" \
    -Dmaven.repo.local="D:/data/repository/local"
```

Expected: `BUILD SUCCESS` for all modules

- [ ] **Step 4: Final Commit**

```bash
cd "D:/Framework/raf-framework"
git add pom.xml
git commit -m "build: register new example modules in root pom"
```

---

## Summary

| Task | Module | 核心演示点 |
|------|--------|-----------|
| 1 | raf-example-redis-starter | String/Hash/List/Set/ZSet 操作 + Redisson 分布式锁库存扣减 |
| 2 | raf-example-rabbit-starter | 普通队列（幂等+异常处理）+ DLX 延迟队列（订单超时取消）|
| 3 | raf-example-mybatis-starter | CRUD + 分页 + 事务传播（REQUIRED vs REQUIRES_NEW）|
| 4 | raf-example-nacos-starter | @RefreshScope 动态配置热更新 + 服务注册发现 |
| 5 | raf-example-mongodb-starter | Document CRUD + Spring Data MongoDB Repository |
| 6 | raf-example-elasticsearch-starter | 全文检索 + 索引管理 + Spring Data ES |
| 7 | raf-example-openapi-starter | Springdoc OpenAPI 3 + Swagger UI |
| 8 | 根 pom.xml | 注册所有新 example 模块 |
