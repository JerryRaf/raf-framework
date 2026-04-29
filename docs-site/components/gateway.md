# API 网关（gateway-starter）

## 功能概述

`raf-framework-gateway-starter` 基于 Spring Cloud Gateway，内置企业级安全机制：

- **ECIES 非对称加密**：请求体加密传输，防止中间人攻击
- **ECDSA 签名验证**：防篡改，验证请求来源合法性
- **防重放攻击**：时间戳窗口（±300 秒）+ Nonce 唯一性校验（Redis TTL 10 分钟）
- **路由配置**：标准 Spring Cloud Gateway 路由配置

## 安全通信协议

详见 [App API 安全设计](./api-security.md)。

### 请求头规范

| 请求头 | 说明 |
|---|---|
| `X-App-Key` | 客户端 EC 公钥（注册时下发） |
| `X-App-Ts` | 13 位 Unix 时间戳（毫秒） |
| `X-App-Nonce` | 32 位 UUID，一次性使用 |
| `X-App-Sign` | ECDSA 签名，覆盖 `clientKey+ts+nonce+content` |

## 快速接入

**1. 引入依赖**

```xml
<dependency>
    <groupId>io.github.jerryraf</groupId>
    <artifactId>raf-framework-gateway-starter</artifactId>
</dependency>
```

**2. 配置**（`application.yml`）

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/user/**
          filters:
            - StripPrefix=0
        - id: order-service
          uri: lb://order-service
          predicates:
            - Path=/api/order/**

raf:
  gateway:
    enabled: true
    security:
      enabled: true          # 启用安全验证
      timestamp-tolerance: 300  # 时间戳容忍秒数
      nonce-ttl: 600         # Nonce 缓存 TTL（秒）
```

## 核心用法

### 自定义全局过滤器

```java
@Component
@Order(-1)
public class AuthGlobalFilter implements GlobalFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String token = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (StringUtils.isBlank(token)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        // 验证 token，写入用户信息到请求头
        return chain.filter(exchange);
    }
}
```

### 路由断言与过滤器

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: order-service
          uri: lb://order-service
          predicates:
            - Path=/api/order/**
            - Header=X-App-Key, .+   # 必须包含 X-App-Key 请求头
          filters:
            - AddRequestHeader=X-Gateway-Source, raf-gateway
            - RequestRateLimiter=10, 20, #{@redisRateLimiter}  # 限流
```

## 常见问题

**Q: 网关启动时报 `spring-webmvc` 冲突？**

A: Gateway 基于 WebFlux，与 spring-webmvc 不兼容。确保项目中没有引入 `spring-boot-starter-web`，改用 `spring-boot-starter-webflux`。

**Q: 签名验证失败，但客户端签名逻辑正确？**

A: 检查签名字符串的拼接顺序是否为 `clientKey + ts + nonce + content`，以及时间戳是否为 13 位毫秒级。

**Q: 防重放校验依赖 Redis，Redis 不可用时网关会拒绝所有请求吗？**

A: 默认行为是 Redis 不可用时跳过 Nonce 校验（降级策略），仅依赖时间戳窗口。可通过配置调整为严格模式。
