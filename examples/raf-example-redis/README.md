# raf-example-redis

Redis 缓存与 Redisson 分布式锁示例，演示 raf-framework-redis-starter 的核心功能。

## 功能演示

- RedisService 常用操作（String、Hash、List、Set）
- Redisson 分布式锁（防并发重复操作）
- 自定义缓存 TTL（`@Cacheable` + `raf.redis.customCache`）
- 缓存穿透防护（cacheNullValues）

## 环境要求

- JDK 17+
- Maven 3.8.8+
- Redis 6.x+（本地或 Docker）

## 快速启动

**1. 启动 Redis**

```bash
docker run -d --name redis -p 6379:6379 redis:7-alpine
```

**2. 启动应用**

```bash
cd examples/raf-example-redis
mvn spring-boot:run
```

## 核心配置说明

```yaml
spring:
  data:
    redis:
      host: 127.0.0.1
      port: 6379

raf:
  redis:
    enabled: true
    customCache:
      userCache:
        timeToLive: 5m
        cacheNullValues: true
  redisson:
    enabled: true
    single:
      host: 127.0.0.1
      port: 6379
```

## API 接口列表

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/redis/set?key=k&value=v` | 设置 String 值（30 分钟过期） |
| GET | `/api/redis/get?key=k` | 获取 String 值 |
| GET | `/api/redis/incr?key=counter` | 原子计数 |
| POST | `/api/lock/order/{orderId}` | 分布式锁示例（模拟订单处理） |
| GET | `/api/cache/user/{id}` | 带缓存的用户查询（5 分钟 TTL） |
