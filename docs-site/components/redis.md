# Redis 缓存与分布式锁（redis-starter）

## 功能概述

- **RedisService**：封装常用 Redis 操作（String、Hash、List、Set、ZSet、过期时间）
- **Redisson 分布式锁**：基于 RedissonService，支持单机和集群模式
- **自定义缓存 TTL**：通过 `raf.redis.customCache` 为不同 cacheName 配置独立 TTL
- **多级缓存**：MultiLevelCacheService 支持本地缓存 + Redis 二级缓存

## 配置项

### Redis 增强（raf.redis）

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `raf.redis.enabled` | boolean | `false` | 是否启用 Redis 增强功能 |
| `raf.redis.customCache.{name}.timeToLive` | Duration | — | 缓存 TTL，如 `30m`、`1h` |
| `raf.redis.customCache.{name}.cacheNullValues` | boolean | `true` | 是否缓存 null 值（防缓存穿透） |
| `raf.redis.customCache.{name}.keyPrefix` | string | — | Key 前缀 |
| `raf.redis.customCache.{name}.useKeyPrefix` | boolean | `true` | 是否使用 Key 前缀 |

### Redisson（raf.redisson）

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `raf.redisson.enabled` | boolean | `false` | 是否启用 Redisson |
| `raf.redisson.single.host` | string | — | 单机模式：Redis 主机 |
| `raf.redisson.single.port` | int | — | 单机模式：Redis 端口 |
| `raf.redisson.single.password` | string | — | 密码 |
| `raf.redisson.single.database` | int | — | 数据库编号 |
| `raf.redisson.single.ssl` | boolean | — | 是否启用 SSL |
| `raf.redisson.cluster.nodes` | string | — | 集群节点地址，逗号分隔 |
| `raf.redisson.cluster.password` | string | — | 集群密码 |
| `raf.redisson.cluster.scanInterval` | int | `1000` | 集群拓扑扫描间隔（毫秒） |
| `raf.redisson.timeout` | int | `3000` | 命令等待超时（毫秒） |
| `raf.redisson.retryAttempts` | int | `3` | 失败重试次数 |
| `raf.redisson.retryInterval` | int | `1500` | 重试间隔（毫秒） |
| `raf.redisson.threads` | int | CPU×2 | 内部线程池数量 |
| `raf.redisson.nettyThreads` | int | CPU×2 | Netty 线程池数量 |
| `raf.redisson.connectTimeout` | int | `10000` | 连接建立超时（毫秒） |
| `raf.redisson.idleConnectionTimeout` | int | `10000` | 连接空闲检测时间（毫秒） |
| `raf.redisson.sslEndpointIdentification` | boolean | `true` | SSL 端点校验（内网自签名证书可设为 false） |

## 快速接入

**1. 引入依赖**

```xml
<dependency>
    <groupId>io.github.jerryraf</groupId>
    <artifactId>raf-framework-redis-starter</artifactId>
</dependency>
```

**2. 配置**（`application.yml`）

```yaml
spring:
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      password: your_password
      database: 0

raf:
  redis:
    enabled: true
    customCache:
      userCache:
        timeToLive: 30m
        cacheNullValues: true
  redisson:
    enabled: true
    single:
      host: 127.0.0.1
      port: 6379
      password: your_password
      database: 0
```

**集群模式配置：**

```yaml
raf:
  redisson:
    enabled: true
    cluster:
      nodes: 192.168.1.1:6379,192.168.1.2:6379,192.168.1.3:6379
      password: your_password
      scanInterval: 1000
      masterConnectionPoolMaxSize: 64
```

## 核心用法

### RedisService 常用操作

```java
@Autowired
private RedisService redisService;

// String 操作
redisService.set("key", "value", 30, TimeUnit.MINUTES);
String value = (String) redisService.get("key");

// Hash 操作
redisService.hSet("hashKey", "field", "value");
Object field = redisService.hGet("hashKey", "field");

// 原子计数
long count = redisService.incr("counter", 1);

// 判断 key 是否存在
boolean exists = redisService.hasKey("key");

// 删除
redisService.del("key");
```

### Redisson 分布式锁

```java
@Autowired
private RedissonService redissonService;

String lockKey = "order:lock:" + orderId;
RLock lock = redissonService.getLock(lockKey);

try {
    // 等待 3 秒，锁持有 30 秒后自动释放
    boolean locked = lock.tryLock(3, 30, TimeUnit.SECONDS);
    if (!locked) {
        throw new BusinessException(10002, "操作频繁，请稍后重试");
    }
    // 业务逻辑
    processOrder(orderId);
} finally {
    if (lock.isHeldByCurrentThread()) {
        lock.unlock();
    }
}
```

### 自定义缓存 TTL（配合 @Cacheable）

```java
@Cacheable(cacheNames = "userCache", key = "#id")
public User getUserById(Long id) {
    return userMapper.selectById(id);
}
// userCache 的 TTL 由 raf.redis.customCache.userCache.timeToLive 控制
```

### 多级缓存

```java
@Autowired
private MultiLevelCacheService multiLevelCacheService;

// 先查本地缓存，未命中再查 Redis
User user = multiLevelCacheService.get("user:" + id, User.class, () -> userMapper.selectById(id));
```

## 常见问题

**Q: Redisson 和 Spring Data Redis 可以同时使用吗？**

A: 可以。两者独立配置，`spring.data.redis` 控制 Spring Cache / RedisTemplate，`raf.redisson` 控制 Redisson 客户端，互不干扰。

**Q: 分布式锁释放时报 `IllegalMonitorStateException`？**

A: 在 `finally` 块中先判断 `lock.isHeldByCurrentThread()` 再释放，避免锁超时后被其他线程持有时重复释放。

**Q: 自定义缓存 TTL 不生效？**

A: 确认 `raf.redis.enabled=true`，且 `@Cacheable` 的 `cacheNames` 与配置中的 key 完全一致（大小写敏感）。

**Q: 集群模式下 Redisson 连接失败？**

A: 检查 `cluster.nodes` 格式是否为 `host:port` 逗号分隔，且所有节点网络可达。内网自签名证书环境需设置 `sslEndpointIdentification: false`。
