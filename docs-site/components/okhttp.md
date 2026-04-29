# OkHttp 第三方 API 调用（okhttp-starter）

## 功能概述

- **统一 HTTP 客户端**：集中管理第三方 API 调用，避免散落在各处
- **超时配置**：连接超时、读取超时、写入超时独立配置
- **重试机制**：失败自动重试
- **日志拦截器**：记录请求/响应详情，便于排查问题
- **traceId 传播**：自动在请求头中携带 traceId

## 配置项

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `raf.okhttp.enabled` | boolean | `false` | 是否启用 OkHttp |
| `raf.okhttp.connectTimeout` | int | `10` | 连接超时（秒） |
| `raf.okhttp.readTimeout` | int | `30` | 读取超时（秒） |
| `raf.okhttp.writeTimeout` | int | `30` | 写入超时（秒） |
| `raf.okhttp.maxIdleConnections` | int | `10` | 连接池最大空闲连接数 |
| `raf.okhttp.keepAliveDuration` | int | `300` | 连接保活时间（秒） |
| `raf.okhttp.retryOnConnectionFailure` | boolean | `true` | 连接失败是否重试 |

## 快速接入

```xml
<dependency>
    <groupId>io.github.jerryraf</groupId>
    <artifactId>raf-framework-okhttp-starter</artifactId>
</dependency>
```

```yaml
raf:
  okhttp:
    enabled: true
    connectTimeout: 10
    readTimeout: 30
    writeTimeout: 30
    maxIdleConnections: 20
    keepAliveDuration: 300
```

## 核心用法

### GET 请求

```java
@Autowired
private OkHttpClient okHttpClient;

public String callThirdPartyApi(String url) {
    Request request = new Request.Builder()
        .url(url)
        .addHeader("Authorization", "Bearer " + token)
        .build();

    try (Response response = okHttpClient.newCall(request).execute()) {
        if (!response.isSuccessful()) {
            throw new InfrastructureException("第三方 API 调用失败: " + response.code());
        }
        return response.body().string();
    } catch (IOException e) {
        throw new InfrastructureException("第三方 API 网络异常", e);
    }
}
```

### POST JSON 请求

```java
public <T> T postJson(String url, Object requestBody, Class<T> responseType) {
    String json = JsonUtils.toJson(requestBody);
    RequestBody body = RequestBody.create(json, MediaType.parse("application/json; charset=utf-8"));

    Request request = new Request.Builder()
        .url(url)
        .post(body)
        .build();

    try (Response response = okHttpClient.newCall(request).execute()) {
        if (!response.isSuccessful()) {
            throw new InfrastructureException("API 调用失败，状态码: " + response.code());
        }
        String responseBody = response.body().string();
        return JsonUtils.fromJson(responseBody, responseType);
    } catch (IOException e) {
        throw new InfrastructureException("API 调用网络异常", e);
    }
}
```

### 异步请求

```java
Request request = new Request.Builder().url(url).build();

okHttpClient.newCall(request).enqueue(new Callback() {
    @Override
    public void onFailure(Call call, IOException e) {
        log.error("异步请求失败: {}", url, e);
    }

    @Override
    public void onResponse(Call call, Response response) throws IOException {
        try (response) {
            String body = response.body().string();
            // 处理响应
        }
    }
});
```

## 常见问题

**Q: 调用第三方 API 超时，但超时时间已经设置很长了？**

A: 检查是否有代理或防火墙拦截。也可以通过 OkHttp 日志拦截器确认请求是否真正发出。

**Q: 如何为不同的第三方 API 配置不同的超时？**

A: 可以通过 `okHttpClient.newBuilder().readTimeout(60, TimeUnit.SECONDS).build()` 创建派生客户端，覆盖特定配置。

**Q: 连接池耗尽导致请求排队？**

A: 增大 `maxIdleConnections`，或检查是否有 Response 没有正确关闭（必须在 try-with-resources 中使用）。
