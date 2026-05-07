# raf-example-gateway-starter

> 演示 `raf-framework-gateway-starter` 的核心能力：Spring Cloud Gateway 路由、全局过滤器、限流熔断。

## 功能概述

`raf-framework-gateway-starter` 基于 Spring Cloud Gateway，提供：

- **请求路由**：基于 Predicate 的动态路由配置
- **全局过滤器**：统一鉴权、日志、限流
- **跨域处理**：网关层统一 CORS 配置
- **限流熔断**：集成 Spring Cloud Gateway 内置的 RequestRateLimiter 和 CircuitBreaker

## 快速接入

### 1. 引入依赖

```xml
<dependency>
    <groupId>io.github.jerryraf</groupId>
    <artifactId>raf-framework-gateway-starter</artifactId>
</dependency>
```

### 2. 配置（`application.yml`）

```yaml
spring:
  application:
    name: raf-example-gateway-starter
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
          filters:
            - StripPrefix=0

server:
  port: 8080
  shutdown: graceful
```

## 配置项详解

### 防重放配置

| 配置键 | 类型 | 说明 |
|---|---|---|
| `raf.gateway.enabled` | boolean | 是否启用 Gateway 增强（默认 false） |
| `raf.gateway.security.anti-replay.enabled` | boolean | 是否启用防重放攻击（默认 false） |
| `raf.gateway.security.anti-replay.window-seconds` | int | 时间窗口（秒），超出窗口的请求拒绝 |
| `raf.gateway.security.anti-replay.nonce-ttl-minutes` | int | Nonce 在 Redis 中的 TTL（分钟） |

## 核心用法

### 路由配置

```yaml
spring:
  cloud:
    gateway:
      routes:
        # 精确路径路由
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/user/**
          filters:
            - StripPrefix=0

        # 带权重的路由（灰度发布）
        - id: order-service-v1
          uri: lb://order-service
          predicates:
            - Path=/api/order/**
            - Weight=order-group, 90
          filters:
            - StripPrefix=0

        - id: order-service-v2
          uri: lb://order-service-v2
          predicates:
            - Path=/api/order/**
            - Weight=order-group, 10
          filters:
            - StripPrefix=0
```

### 自定义全局过滤器

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthGlobalFilter implements GlobalFilter {

    private static final Logger log = LoggerFactory.getLogger(AuthGlobalFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String token = request.getHeaders().getFirst("Authorization");

        if (StringUtils.isBlank(token)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // 验证 token，将用户信息传递给下游服务
        String userId = validateToken(token);
        ServerHttpRequest mutatedRequest = request.mutate()
            .header("X-User-Id", userId)
            .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }
}
```

### 防重放配置示例

```yaml
raf:
  gateway:
    enabled: true
    security:
      anti-replay:
        enabled: true
        window-seconds: 300
        nonce-ttl-minutes: 10
```

防重放攻击依赖 Redis 存储 Nonce，需同时启用 `raf.redis.enabled: true`。

### 限流配置

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
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 100    # 每秒补充令牌数
                redis-rate-limiter.burstCapacity: 200    # 令牌桶容量
                redis-rate-limiter.requestedTokens: 1    # 每次请求消耗令牌数
                key-resolver: "#{@ipKeyResolver}"        # 限流 key 解析器
```

```java
@Bean
public KeyResolver ipKeyResolver() {
    return exchange -> Mono.just(
        Objects.requireNonNull(exchange.getRequest().getRemoteAddress())
            .getAddress().getHostAddress()
    );
}
```

### 熔断降级

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
            - name: CircuitBreaker
              args:
                name: userServiceCircuitBreaker
                fallbackUri: forward:/fallback/user
```

```java
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/user")
    @ResponseResult
    public String userFallback() {
        throw new InfrastructureException("用户服务暂时不可用，请稍后重试");
    }
}
```

## 示例项目结构

```
raf-example-gateway-starter/
├── src/main/java/io/github/jerryraf/examples/gateway/
│   ├── GatewayExampleApplication.java
│   ├── filter/
│   │   └── AuthGlobalFilter.java       # 全局鉴权过滤器
│   ├── config/
│   │   └── RateLimiterConfig.java      # 限流配置
│   └── controller/
│       └── FallbackController.java     # 降级处理
└── src/main/resources/
    └── application.yml
```

## 最佳实践

1. **防重放攻击**：启用 `anti-replay` 并配合 Redis，防止请求被重放
2. **限流粒度**：根据业务场景选择 IP 限流、用户限流或接口限流
3. **熔断阈值**：根据下游服务 SLA 设置合理的熔断阈值，避免过于敏感
4. **日志脱敏**：网关日志中的 Authorization、Cookie 等敏感头自动脱敏
5. **优雅停机**：配置 `server.shutdown: graceful`，确保路由中的请求处理完毕再停机

## 常见问题

**Q: 路由配置后下游服务返回 404？**

A: 检查 `StripPrefix` 过滤器配置。`StripPrefix=1` 会去掉路径第一段，`StripPrefix=0` 保持原路径。确认下游服务的实际路径与路由后的路径匹配。

**Q: 限流不生效？**

A: `RequestRateLimiter` 过滤器依赖 Redis，确认 Redis 连接正常。检查 `key-resolver` Bean 是否正确注册。

**Q: 防重放校验失败？**

A: 确认 Redis 已启用（`raf.redis.enabled: true`），且客户端请求时间戳与服务器时间差在 `window-seconds` 范围内。检查 Nonce 是否重复使用。

**Q: 网关层如何传递用户信息给下游服务？**

A: 在全局过滤器中解析 Token，将用户 ID、角色等信息通过自定义请求头（如 `X-User-Id`、`X-User-Roles`）传递给下游服务。
