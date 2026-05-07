# raf-example-okhttp-starter

> 演示 `raf-framework-okhttp-starter` 的核心能力：多渠道 OkHttp 客户端配置、GET/POST/PUT/DELETE 请求、Bearer Token / Basic 认证、重试与超时。

## 功能概述

`raf-framework-okhttp-starter` 提供：

- **多渠道隔离**：每个第三方服务配置独立的 `OkHttpClient`（超时、认证、连接池互不影响）
- **HttpExecutor**：统一封装 GET / POST JSON / POST 表单 / PUT / DELETE 操作
- **认证支持**：内置 Bearer Token、Basic Auth 自动注入请求头
- **重试机制**：连接失败自动重试，可配置最大重试次数
- **请求日志**：可配置 OFF / BASIC / HEADERS / BODY 四个日志级别
- **traceId 传播**：自动将 traceId 注入到请求头，便于跨服务链路追踪

## 快速接入

### 1. 引入依赖

```xml
<dependency>
    <groupId>io.github.jerryraf</groupId>
    <artifactId>raf-framework-okhttp-starter</artifactId>
</dependency>
```

### 2. 配置（`application.yml`）

```yaml
raf:
  okhttp:
    enabled: true
    channels:
      # 渠道名称即注入时的 channel 参数
      github:
        connect-timeout: 3000
        read-timeout: 10000
        write-timeout: 5000
        retry-on-connection-failure: true
        max-retries: 2
        level: BASIC

      payment:
        connect-timeout: 2000
        read-timeout: 5000
        auth:
          type: BEARER
          token: ${PAYMENT_API_TOKEN}

      sms:
        connect-timeout: 2000
        read-timeout: 8000
        auth:
          type: BASIC
          username: ${SMS_USERNAME}
          password: ${SMS_PASSWORD}
```

## 配置项详解

### 渠道配置（raf.okhttp.channels.{name}）

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `connect-timeout` | int | `2000` | 连接超时（毫秒） |
| `read-timeout` | int | `5000` | 读超时（毫秒） |
| `write-timeout` | int | `5000` | 写超时（毫秒） |
| `retry-on-connection-failure` | boolean | `true` | 连接失败是否重试 |
| `max-retries` | int | `0` | 最大重试次数（0 表示不重试） |
| `level` | string | `BASIC` | 日志级别：`NONE` / `BASIC` / `HEADERS` / `BODY` |
| `follow-redirects` | boolean | `true` | 是否跟随 HTTP 重定向 |
| `follow-ssl-redirects` | boolean | `true` | 是否跟随 HTTPS 重定向 |

### 连接池（connection）

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `max-idle-connections` | int | `10` | 最大空闲连接数 |
| `keep-alive-duration` | long | `30000` | 连接保活时长（毫秒） |

### 认证（auth）

| 配置键 | 类型 | 说明 |
|---|---|---|
| `type` | string | 认证类型：`BEARER` / `BASIC` |
| `token` | string | Bearer Token 值 |
| `username` | string | Basic Auth 用户名 |
| `password` | string | Basic Auth 密码 |

### SSL（ssl）

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `enabled` | boolean | `false` | 是否启用双向 SSL |
| `insecure` | boolean | `false` | 是否跳过证书验证（仅测试环境） |
| `key-store-path` | string | — | 客户端证书路径 |
| `trust-store-path` | string | — | 信任证书路径 |

## 核心用法

### 注入 HttpExecutor

```java
@Autowired
private HttpExecutor httpExecutor;
```

### GET 请求

```java
// 简单 GET
try (Response response = httpExecutor.get("github", "https://api.github.com/users/octocat")) {
    if (!response.isSuccessful()) {
        throw new InfrastructureException("GitHub API 调用失败: " + response.code());
    }
    String body = response.body().string();
}

// 带查询参数和自定义请求头
Map<String, String> headers = Map.of("X-Request-Id", traceId);
Map<String, String> params  = Map.of("page", "1", "per_page", "10");
try (Response response = httpExecutor.get("github", url, headers, params)) {
    // ...
}
```

### POST JSON

```java
String json = jsonService.toJson(orderDTO);
try (Response response = httpExecutor.postJson("payment", "https://payment.internal/api/payments", json)) {
    if (!response.isSuccessful()) {
        throw new InfrastructureException("支付服务调用失败: " + response.code());
    }
    PaymentResult result = jsonService.fromJson(response.body().string(), PaymentResult.class);
}
```

### POST 表单

```java
Map<String, String> form = Map.of(
    "phone",   "13800138000",
    "content", "您的验证码是 123456",
    "sign",    "RAF"
);
try (Response response = httpExecutor.postForm("sms", "https://sms.provider.com/send", form)) {
    // ...
}
```

### PUT / DELETE

```java
// PUT 更新
try (Response response = httpExecutor.putJson("payment", url + "/" + id, json)) { ... }

// DELETE
try (Response response = httpExecutor.delete("payment", url + "/" + id, null)) { ... }
```

### 封装为 Service（推荐模式）

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentApiService {

    private final HttpExecutor httpExecutor;
    private final JsonService jsonService;

    private static final String CHANNEL = "payment";
    private static final String BASE_URL = "https://payment.internal/api/v1";

    public PaymentResult createPayment(PaymentReq req) {
        String json = jsonService.toJson(req);
        try (Response response = httpExecutor.postJson(CHANNEL, BASE_URL + "/payments", json)) {
            if (!response.isSuccessful()) {
                log.error("支付服务异常，status={}, body={}", response.code(),
                    response.body() != null ? response.body().string() : "");
                throw new InfrastructureException("支付服务调用失败");
            }
            return jsonService.fromJson(response.body().string(), PaymentResult.class);
        } catch (IOException e) {
            throw new InfrastructureException("支付服务网络异常", e);
        }
    }
}
```

## 示例项目结构

```
raf-example-okhttp-starter/
├── src/main/java/io/github/jerryraf/examples/okhttp/
│   ├── OkHttpExampleApplication.java
│   ├── controller/
│   │   └── ThirdPartyController.java   # 演示接口
│   └── service/
│       └── ThirdPartyApiService.java   # 多渠道调用封装
└── src/main/resources/
    └── application.yml                 # 三个渠道配置示例
```

## API 接口

| 方法 | 路径 | 渠道 | 说明 |
|---|---|---|---|
| GET | `/api/third-party/github/users/{username}` | github | 无认证 GET |
| POST | `/api/third-party/payment` | payment | Bearer Token POST JSON |
| POST | `/api/third-party/sms` | sms | Basic Auth POST 表单 |

## 最佳实践

1. **渠道隔离**：每个第三方服务配置独立渠道，超时和连接池互不影响，避免慢服务拖垮其他调用
2. **敏感配置加密**：`auth.token`、`auth.password` 使用 Jasypt 加密，格式 `ENC(密文)`
3. **Response 必须关闭**：使用 try-with-resources 确保 `Response` 关闭，防止连接泄漏
4. **异常转换**：捕获 `IOException` 后转换为 `InfrastructureException`，让全局异常处理器统一处理
5. **日志级别**：生产环境使用 `BASIC`，排障时临时切换到 `HEADERS`，避免 `BODY` 级别泄露敏感数据
6. **超时设置**：根据 SLA 设置合理超时，支付类接口建议 `read-timeout: 5000`，报表类可适当放宽

## 常见问题

**Q: 多个渠道的 `HttpExecutor` Bean 是同一个吗？**

A: 是的，`HttpExecutor` 是单例 Bean，内部维护一个 `Map<String, OkHttpClient>`，通过 channel 名称路由到对应客户端。

**Q: 如何动态切换 Token（Token 有过期时间）？**

A: 实现自定义 `Interceptor` 注入到对应渠道的 `OkHttpClient`，在拦截器中动态获取最新 Token。

**Q: 调用第三方 HTTPS 接口报证书错误？**

A: 开发环境可设置 `ssl.insecure: true` 跳过验证；生产环境应配置正确的 `trust-store-path` 信任对方证书。
