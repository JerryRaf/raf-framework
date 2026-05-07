# raf-example-redis-starter

> 演示 `raf-framework-redis-starter` 的核心能力：RedisTemplate 五种数据结构操作、Redisson 分布式锁、自定义缓存 TTL。

## 功能概述

`raf-framework-redis-starter` 提供：

- **RedisService**：封装 String/Hash/List/Set/ZSet 五种数据结构的常用操作
- **Redisson 分布式锁**：基于 Redisson 的 `tryLock`，支持超时自动释放
- **自定义缓存 TTL**：`@Cacheable` 注解支持按 key 前缀配置不同 TTL
- **多级缓存**：本地缓存（Caffeine）+ Redis 二级缓存

## 快速接入

### 1. 引入依赖

```xml
<!-- Redis + Redisson -->
<dependency>
    <groupId>io.github.jerryraf</groupId>
    <artifactId>raf-framework-redis-starter</artifactId>
</dependency>
```

### 2. 配置（`application.yml`）

```yaml
spring:
  application:
    name: raf-example-redis-starter
  data:
    redis:
      host: ${REDIS_HOST:127.0.0.1}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      database: 0
      timeout: 3000ms
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 2
          max-wait: 1000ms

raf:
  redis:
    enabled: true   # 启用框架 Redis 增强（RedisService、CacheManager）

  redisson:
    enabled: true
    single:
      host: ${REDIS_HOST:127.0.0.1}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      database: 0
```

## 配置项详解

### Redis 连接（spring.data.redis）

Redis 连接使用 Spring Boot 标准配置，`raf.redis.enabled` 仅控制框架增强功能（`RedisService`、`CacheManager`）的开关。

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `raf.redis.enabled` | boolean | `false` | 是否启用框架 Redis 增强 |
| `spring.data.redis.host` | string | `127.0.0.1` | Redis 主机地址 |
| `spring.data.redis.port` | int | `6379` | Redis 端口 |
| `spring.data.redis.password` | string | — | 密码（无密码留空） |
| `spring.data.redis.database` | int | `0` | 数据库索引 |
| `spring.data.redis.timeout` | Duration | `3000ms` | 连接超时 |
| `spring.data.redis.lettuce.pool.max-active` | int | `8` | 最大连接数 |
| `spring.data.redis.lettuce.pool.max-idle` | int | `8` | 最大空闲连接数 |
| `spring.data.redis.lettuce.pool.min-idle` | int | `0` | 最小空闲连接数 |
| `spring.data.redis.lettuce.pool.max-wait` | Duration | `-1ms` | 获取连接最大等待时间（-1 表示无限等待） |

### Redisson 分布式锁（raf.redisson）

单机模式：

| 配置键 | 类型 | 说明 |
|---|---|---|
| `raf.redisson.enabled` | boolean | 是否启用 Redisson（默认 false） |
| `raf.redisson.single.host` | string | Redis 主机地址 |
| `raf.redisson.single.port` | int | Redis 端口 |
| `raf.redisson.single.password` | string | 密码 |
| `raf.redisson.single.database` | int | 数据库索引 |
| `raf.redisson.single.ssl` | boolean | 是否启用 SSL |

集群模式：

```yaml
raf:
  redisson:
    enabled: true
    cluster:
      nodes: redis://192.0.2.1:7001,redis://192.0.2.2:7002,redis://192.0.2.3:7003
      password: ${REDIS_PASSWORD:}
      master-connection-pool-max-size: 64
      scan-interval: 1000
```

### 自定义缓存 TTL（raf.redis.custom-cache）

```yaml
raf:
  redis:
    custom-cache:
      user:
        time-to-live: 3600s
        cache-null-values: true
      product:
        time-to-live: 1800s
      session:
        time-to-live: 86400s
```

## 核心用法

### RedisService 操作

注入 `RedisService` 使用封装好的操作方法：

```java
@Autowired
private RedisService redisService;

// String 操作
redisService.set("user:1001", userJson, 3600, TimeUnit.SECONDS);
String value = redisService.get("user:1001");
redisService.delete("user:1001");

// 原子计数
Long count = redisService.increment("page:view:count", 1);

// 判断存在
boolean exists = redisService.hasKey("user:1001");
```

### RedisTemplate 直接操作

```java
@Autowired
private RedisTemplate<String, Object> redisTemplate;

// Hash 操作
redisTemplate.opsForHash().put("user:profile:1001", "name", "张三");
redisTemplate.opsForHash().put("user:profile:1001", "age", 25);
Map<Object, Object> profile = redisTemplate.opsForHash().entries("user:profile:1001");

// List 操作（消息队列场景）
redisTemplate.opsForList().rightPush("task:queue", taskJson);
String task = (String) redisTemplate.opsForList().leftPop("task:queue");

// Set 操作（去重场景）
redisTemplate.opsForSet().add("online:users", "user:1001", "user:1002");
Boolean isMember = redisTemplate.opsForSet().isMember("online:users", "user:1001");

// ZSet 操作（排行榜场景）
redisTemplate.opsForZSet().add("leaderboard", "player:1001", 9500.0);
redisTemplate.opsForZSet().add("leaderboard", "player:1002", 8800.0);
Set<Object> top10 = redisTemplate.opsForZSet().reverseRange("leaderboard", 0, 9);
```

### Redisson 分布式锁

```java
@Autowired
private RedissonClient redissonClient;

public boolean deductStock(Long productId, int quantity) {
    String lockKey = "lock:stock:" + productId;
    RLock lock = redissonClient.getLock(lockKey);

    // 尝试获取锁：等待 3 秒，持有 10 秒后自动释放
    boolean acquired = false;
    try {
        acquired = lock.tryLock(3, 10, TimeUnit.SECONDS);
        if (!acquired) {
            throw new BusinessException(StockErrorCode.STOCK_LOCK_FAILED);
        }

        // 查询库存
        int stock = stockMapper.selectStock(productId);
        if (stock < quantity) {
            throw new BusinessException(StockErrorCode.INSUFFICIENT_STOCK);
        }

        // 扣减库存
        stockMapper.deductStock(productId, quantity);
        log.info("库存扣减成功，productId={}, quantity={}", productId, quantity);
        return true;

    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new SystemException("获取分布式锁被中断", e);
    } finally {
        if (acquired && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
```

### 自定义缓存 TTL

```java
// 使用 @Cacheable，缓存名称对应 raf.redis.custom-cache 中的 key
@Cacheable(cacheNames = "user", key = "#userId")
public UserDTO getUserById(Long userId) {
    return userMapper.selectById(userId);
}

@CacheEvict(cacheNames = "user", key = "#userId")
public void updateUser(Long userId, UserUpdateReq req) {
    userMapper.updateById(userId, req);
}
```

### 幂等性控制（setIfAbsent）

```java
public boolean processOnce(String bizId) {
    String key = "idempotent:" + bizId;
    // 设置成功说明是第一次处理，24 小时内不重复处理
    Boolean isFirst = redisService.setIfAbsent(key, "1", 24, TimeUnit.HOURS);
    return Boolean.TRUE.equals(isFirst);
}
```

## 示例项目结构

```
raf-example-redis-starter/
├── src/main/java/io/github/jerryraf/examples/redis/
│   ├── RedisExampleApplication.java
│   ├── controller/
│   │   ├── RedisOpsController.java     # 五种数据结构演示接口
│   │   └── StockController.java        # 分布式锁库存扣减接口
│   ├── service/
│   │   ├── RedisOpsService.java        # RedisTemplate 操作封装
│   │   └── StockService.java           # 分布式锁业务逻辑
│   └── dto/
│       └── StockDeductReq.java         # 请求 DTO
└── src/main/resources/
    └── application.yml
```

## API 接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/redis/string/{key}` | 获取 String 值 |
| POST | `/api/redis/string/{key}` | 设置 String 值 |
| GET | `/api/redis/hash/{key}` | 获取 Hash 所有字段 |
| GET | `/api/redis/list/{key}` | 获取 List 所有元素 |
| GET | `/api/redis/set/{key}` | 获取 Set 所有成员 |
| GET | `/api/redis/zset/{key}` | 获取 ZSet 排行榜（降序） |
| POST | `/api/stock/init` | 初始化库存 |
| GET | `/api/stock/{productId}` | 查询库存 |
| POST | `/api/stock/deduct` | 扣减库存（分布式锁保护） |

## 最佳实践

1. **Key 命名规范**：使用 `业务:实体:ID` 格式，如 `user:profile:1001`，便于管理和清理
2. **TTL 必须设置**：所有 key 必须设置过期时间，防止内存泄漏
3. **分布式锁超时**：`tryLock` 的持有时间要大于业务执行时间，避免锁提前释放
4. **锁释放保护**：`finally` 块中释放锁，且检查 `isHeldByCurrentThread()` 防止释放他人的锁
5. **缓存穿透防护**：对不存在的 key 也缓存空值（TTL 设短），防止缓存穿透
6. **序列化一致性**：框架默认使用 Jackson 序列化，确保存取时使用相同的序列化配置

## 常见问题

**Q: Redisson 连接报错 `Unable to connect to Redis server`？**

A: 检查 `address` 格式是否为 `redis://host:port`（注意 `redis://` 前缀），以及 Redis 服务是否可达。

**Q: 分布式锁在单元测试中如何 Mock？**

A: 使用 `@MockBean RedissonClient` 并 Mock `getLock()` 返回值，或使用嵌入式 Redis（如 `embedded-redis`）进行集成测试。

**Q: `@Cacheable` 缓存不生效？**

A: 确认 `raf.redis.enabled=true`，且缓存名称与 `raf.redis.custom-cache` 中的 key 一致。同类内部调用不走 AOP，需通过 Spring 代理调用。

**Q: Redis 集群模式下 Lua 脚本报错？**

A: Redis 集群要求 Lua 脚本中所有 key 在同一个 slot，使用 `{hash_tag}` 确保相关 key 落在同一 slot，如 `lock:{order}:1001`。
