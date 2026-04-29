# Web 基础（web-starter）

## 功能概述

`raf-framework-web-starter` 是所有 Web 服务的基础 Starter，提供：

- **统一响应封装**：`@ResponseResult` 注解自动包装返回值为 `RafResult<T>`
- **全局异常处理**：四层异常体系（Business / Infrastructure / System / Protocol）
- **HTTP 请求日志**：可配置的访问日志级别（OFF → RSP_BODY）
- **CORS 跨域**：统一跨域配置
- **API 版本路由**：`@ApiVersion` 注解支持 URL 版本路由
- **异步线程池**：多线程池配置，自动传播 traceId

## 配置项

### 请求日志（raf.log）

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `raf.log.level` | enum | `RSP_HEADERS` | 日志级别：OFF / BASIC / REQ_HEADERS / REQ_BODY / RSP_HEADERS / RSP_BODY |

### CORS 跨域（raf.cors）

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `raf.cors.enabled` | boolean | `false` | 是否启用跨域 |
| `raf.cors.path` | string | — | 跨域路径，如 `/**` |
| `raf.cors.allowOrigins` | list | `["*"]` | 允许的来源 |
| `raf.cors.allowHeaders` | list | `["*"]` | 允许的请求头 |
| `raf.cors.allowMethods` | list | `["*"]` | 允许的 HTTP 方法 |
| `raf.cors.allowExposeHeaders` | list | — | 暴露给前端的响应头 |

### 异步线程池（raf.async）

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `raf.async.enabled` | boolean | `false` | 是否启用多线程池 |
| `raf.async.pools.{name}.coreSize` | int | `10` | 核心线程数 |
| `raf.async.pools.{name}.maxSize` | int | `50` | 最大线程数 |
| `raf.async.pools.{name}.queueCapacity` | int | `100` | 队列容量 |
| `raf.async.pools.{name}.keepAliveSeconds` | int | `60` | 空闲线程存活时间（秒） |
| `raf.async.pools.{name}.threadNamePrefix` | string | `raf-async-` | 线程名前缀 |

## 快速接入

**1. 引入依赖**

```xml
<dependency>
    <groupId>io.github.jerryraf</groupId>
    <artifactId>raf-framework-web-starter</artifactId>
</dependency>
```

**2. 最小配置**（`application.yml`）

```yaml
raf:
  log:
    level: REQ_BODY   # 开发环境建议 REQ_BODY，生产环境建议 RSP_HEADERS
  cors:
    enabled: true
    path: /**
```

## 核心用法

### 统一响应封装

在 Controller 类或方法上加 `@ResponseResult`，框架自动将返回值包装为 `RafResult<T>`：

```java
@ResponseResult
@RestController
@RequestMapping("/api/user")
public class UserController {

    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.getById(id);
        // 自动包装为 {"code":200,"msg":"成功！","data":{...}}
    }
}
```

### 异常使用规范

```java
// 业务异常（code >= 10000，HTTP 200，WARN 日志，无堆栈）
throw new BusinessException(10001, "用户不存在");

// 基础设施异常（第三方服务/数据库错误，HTTP 500）
throw new InfrastructureException("Redis 连接失败", e);

// 系统异常（未预期错误，HTTP 500，完整堆栈）
throw new SystemException("未知错误", e);

// 协议异常（认证失败，HTTP 401）
throw new ProtocolException("Token 已过期");
```

### API 版本路由

```java
@ApiVersion("v1")
@RestController
@RequestMapping("/api/user")
public class UserV1Controller { ... }

@ApiVersion("v2")
@RestController
@RequestMapping("/api/user")
public class UserV2Controller { ... }
// 访问 /v1/api/user 和 /v2/api/user 分别路由到不同 Controller
```

### 异步线程池

```yaml
raf:
  async:
    enabled: true
    pools:
      order:
        coreSize: 20
        maxSize: 100
        queueCapacity: 500
        threadNamePrefix: order-async-
      report:
        coreSize: 5
        maxSize: 20
        queueCapacity: 200
        threadNamePrefix: report-async-
```

```java
@Autowired
@Qualifier("orderThreadPoolTaskExecutor")
private ThreadPoolTaskExecutor orderExecutor;

// traceId 自动传播到子线程
orderExecutor.execute(() -> processOrder(orderId));
```

## 常见问题

**Q: `@ResponseResult` 不生效，返回值没有被包装？**

A: 检查是否在 Controller 类或方法上加了注解。如果 Controller 继承了其他基类，注解需要加在具体方法上。

**Q: 全局异常处理器捕获不到自定义异常？**

A: 确保自定义异常继承了框架的四层异常之一（`BusinessException` / `InfrastructureException` / `SystemException` / `ProtocolException`）。

**Q: 日志级别设置了 `REQ_BODY` 但看不到请求体？**

A: 框架使用 `ContentCachingRequestWrapper` 包装请求体，确保没有其他 Filter 提前消费了请求流。

**Q: 异步线程池的 Bean 名称是什么？**

A: 命名规则为 `{poolName}ThreadPoolTaskExecutor`，例如配置了 `pools.order`，Bean 名称为 `orderThreadPoolTaskExecutor`。
