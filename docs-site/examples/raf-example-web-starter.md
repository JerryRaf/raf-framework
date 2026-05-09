# raf-example-web-starter

> 演示 `raf-framework-web-starter` 的核心能力：统一响应封装、全局异常处理、HTTP 请求日志、CORS 跨域、API 版本路由、异步线程池。

## 功能概述

`raf-framework-web-starter` 是所有 Web 服务的基础 Starter，提供：

- **统一响应封装**：`@ResponseResult` 注解自动将返回值包装为 `RafResult<T>`
- **全局异常处理**：四层异常体系（Business / Infrastructure / System / Protocol）
- **HTTP 请求日志**：可配置的访问日志级别（OFF → RSP_BODY）
- **CORS 跨域**：统一跨域配置
- **API 版本路由**：`@ApiVersion` 注解支持 URL 版本路由
- **异步线程池**：多线程池配置，自动传播 traceId

## 快速接入

### 1. 引入依赖

```xml
<dependency>
    <groupId>io.github.jerryraf</groupId>
    <artifactId>raf-framework-web-starter</artifactId>
</dependency>
```

### 2. 最小配置（`application.yml`）

```yaml
spring:
  application:
    name: raf-example-web-starter

server:
  port: 8080
  shutdown: graceful

raf:
  log:
    enabled: true
    level: RSP_HEADERS
  cors:
    enabled: true
    path: /**
    allowOrigins:
      - https://your-domain.com   # 生产环境指定具体域名，禁止使用 *
    allowMethods:
      - GET
      - POST
      - PUT
      - DELETE
      - OPTIONS
    allowHeaders:
      - "*"
```

## 配置项详解

### 请求日志（raf.log）

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `raf.log.enabled` | boolean | `false` | 是否启用 HTTP 请求日志 |
| `raf.log.level` | enum | `RSP_HEADERS` | 日志级别：OFF / BASIC / REQ_HEADERS / REQ_BODY / RSP_HEADERS / RSP_BODY |
| `raf.log.payloadMaxLength` | int | `4096` | 单个请求/响应 body 写入日志的最大字符数 |
| `raf.log.maxBodyCacheBytes` | int | `1048576` | 请求体最大缓存字节数，超过后不读取 body，避免大请求撑爆内存 |

日志级别说明：

| 级别 | 记录内容 |
|---|---|
| `OFF` | 不记录任何请求日志 |
| `BASIC` | 仅记录请求方法、URL、状态码、耗时 |
| `REQ_HEADERS` | BASIC + 请求头 |
| `REQ_BODY` | REQ_HEADERS + 请求体 |
| `RSP_HEADERS` | REQ_BODY + 响应头（默认） |
| `RSP_BODY` | RSP_HEADERS + 响应体（排障用，生产慎用） |

### CORS 跨域（raf.cors）

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `raf.cors.enabled` | boolean | `false` | 是否启用跨域 |
| `raf.cors.path` | string | — | 跨域路径，如 `/**` |
| `raf.cors.allowOrigins` | list | `[]` | 允许的来源，**生产环境必须指定具体域名** |
| `raf.cors.allowHeaders` | list | `["*"]` | 允许的请求头 |
| `raf.cors.allowMethods` | list | `["GET","POST","PUT","DELETE","OPTIONS"]` | 允许的 HTTP 方法 |
| `raf.cors.allowExposeHeaders` | list | — | 暴露给前端的响应头 |

### 单线程池（raf.executor）

框架内置单线程池，Bean 名称固定为 `rafAsyncExecutor`，支持 `@Async` 注解。

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `raf.executor.enabled` | boolean | `false` | 是否启用 |
| `raf.executor.corePoolSize` | int | `10` | 核心线程数 |
| `raf.executor.maxPoolSize` | int | `120` | 最大线程数 |
| `raf.executor.queueCapacity` | int | `100` | 队列容量 |
| `raf.executor.keepAliveSeconds` | int | `5` | 空闲线程存活时间（秒） |
| `raf.executor.threadNamePrefix` | string | `raf-async-` | 线程名前缀 |
| `raf.executor.metricEnabled` | boolean | `false` | 是否启用 Prometheus 指标采集 |
| `raf.executor.awaitTerminationSeconds` | int | `60` | 优雅停机等待时间（秒） |

### 多线程池（raf.async）

支持按业务域配置多个独立线程池，动态注册为 Spring Bean，Bean 名称即配置 key。

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `raf.async.enabled` | boolean | `false` | 是否启用多线程池 |
| `raf.async.pools.{name}.coreSize` | int | `10` | 核心线程数 |
| `raf.async.pools.{name}.maxSize` | int | `50` | 最大线程数 |
| `raf.async.pools.{name}.queueCapacity` | int | `100` | 队列容量 |
| `raf.async.pools.{name}.keepAliveSeconds` | int | `60` | 空闲线程存活时间（秒） |
| `raf.async.pools.{name}.threadNamePrefix` | string | `raf-async-` | 线程名前缀 |

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
        // 自动包装为 {"code":0,"msg":"success","data":{...}}
    }
}
```

响应格式：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "id": 1,
    "username": "alice"
  }
}
```

响应码说明：

| 响应码 | 含义 |
|---|---|
| `0` | 成功 |
| `10401` | 未授权 |
| `10403` | 权限不足 |
| `10404` | 资源未找到 |
| `10405` | HTTP 方法错误 |
| `10415` | 不支持的媒体类型 |
| `10426` | 请升级协议 |
| `10429` | 访问频率超限 |
| `10500` | 服务器内部错误 |
| `10601` | 请更新 App 版本 |
| `10602` | 服务升级中 |
| `10700` | 参数校验错误 |
| `10800` | 第三方接口错误 |
| `10000+` | 业务异常（应用自定义） |

### 异常使用规范

框架定义四层异常体系，按场景选择：

```java
// 业务异常（HTTP 200，WARN 日志，无堆栈，code >= 10000）
throw new BusinessException(AppResponseEnum.USER_NOT_FOUND);

// 基础设施异常（第三方服务/数据库错误，HTTP 500，ERROR 日志）
throw new InfrastructureException("Redis 连接失败", e);

// 系统异常（未预期错误，HTTP 500，ERROR 日志 + 完整堆栈）
throw new SystemException("未知错误", e);

// 协议异常（认证失败，HTTP 401）
throw new ProtocolException(RafResponseEnum.UNAUTHORIZED, "Token 已过期");
```

自定义业务错误码：

```java
@Getter
@AllArgsConstructor
public enum AppResponseEnum implements IResponseEnum {

    USER_NOT_FOUND(10001, "用户不存在"),
    USER_ALREADY_EXISTS(10002, "用户已存在"),
    INSUFFICIENT_BALANCE(10003, "余额不足");

    private final int code;
    private final String msg;
}
```

### API 版本路由

```java
@ApiVersion("v1")
@RestController
@RequestMapping("/api/user")
public class UserV1Controller {
    // 访问路径：/v1/api/user
}

@ApiVersion("v2")
@RestController
@RequestMapping("/api/user")
public class UserV2Controller {
    // 访问路径：/v2/api/user
}
```

### 异步线程池

**单线程池（`raf.executor`）**，Bean 名称 `rafAsyncExecutor`：

```yaml
raf:
  executor:
    enabled: true
    core-pool-size: 20
    max-pool-size: 100
    queue-capacity: 500
    thread-name-prefix: raf-async-
    metric-enabled: true
```

注入并使用（traceId 通过 `TransmittableThreadLocal` 自动传播到子线程）：

```java
@Autowired
@Qualifier("rafAsyncExecutor")
private ThreadPoolTaskExecutor asyncExecutor;

public void asyncProcessOrder(Long orderId) {
    asyncExecutor.execute(() -> processOrder(orderId));
}
```

或直接使用 `@Async` 注解（默认使用 `rafAsyncExecutor`）：

```java
@Async
public void asyncProcessOrder(Long orderId) {
    processOrder(orderId);
}
```

**多线程池（`raf.async`）**，按业务域隔离，Bean 名称即配置 key：

```yaml
raf:
  async:
    enabled: true
    pools:
      order-pool:
        core-size: 10
        max-size: 50
        queue-capacity: 200
        thread-name-prefix: order-
      report-pool:
        core-size: 5
        max-size: 20
        queue-capacity: 100
        thread-name-prefix: report-
```

```java
@Autowired
@Qualifier("order-pool")
private ThreadPoolTaskExecutor orderExecutor;
```

## 示例项目结构

```
raf-example-web-starter/
├── src/main/java/io/github/jerryraf/examples/web/
│   ├── BasicExampleApplication.java
│   ├── common/
│   │   └── UserErrorCode.java          # 自定义业务错误码
│   ├── controller/
│   │   └── UserController.java         # REST API 示例
│   ├── dto/
│   │   ├── UserCreateReq.java          # 请求 DTO（含 @Valid 校验）
│   │   └── UserRes.java                # 响应 DTO
│   └── service/
│       └── UserService.java            # 业务逻辑
└── src/main/resources/
    └── application.yml
```

## 最佳实践

1. **日志级别**：生产环境使用 `RSP_HEADERS`，排障时临时切换到 `REQ_BODY`/`RSP_BODY`，排障完毕立即恢复
2. **异常分层**：严格按四层异常体系使用，不要用 `RuntimeException` 代替业务异常
3. **响应封装**：在 Controller 类级别加 `@ResponseResult`，避免遗漏
4. **线程池隔离**：不同业务域使用不同线程池，避免慢任务阻塞快任务
5. **CORS 安全**：生产环境将 `allowOrigins` 配置为具体域名，不要使用 `*`

## 常见问题

**Q: `@ResponseResult` 不生效，返回值没有被包装？**

A: 检查是否在 Controller 类或方法上加了注解。如果 Controller 继承了其他基类，注解需要加在具体方法上。

**Q: 全局异常处理器捕获不到自定义异常？**

A: 确保自定义异常继承了框架的四层异常之一（`BusinessException` / `InfrastructureException` / `SystemException` / `ProtocolException`）。

**Q: 日志级别设置了 `REQ_BODY` 但看不到请求体？**

A: 框架会缓存请求体用于日志，但当请求体超过 `raf.log.maxBodyCacheBytes` 时会跳过 body 读取并记录省略标记。

**Q: 访问日志会不会泄露敏感信息？**

A: 请求参数中的 password、token、authorization、secret、cookie 等字段会被写为 `***`；手机号、身份证、邮箱、银行卡、地址会做模式脱敏；Cookie 只记录名称并隐藏值。

**Q: 异步线程池的 Bean 名称是什么？**

A: 框架注册的异步线程池 Bean 名称为 `rafAsyncExecutor`，通过 `@Qualifier("rafAsyncExecutor")` 注入，或直接使用 `@Async` 注解（默认使用该线程池）。
