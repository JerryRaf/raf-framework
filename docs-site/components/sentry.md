# Sentry 错误追踪（sentry-starter）

## 功能概述

- **自动错误上报**：未捕获异常自动上报到 Sentry
- **异常过滤**：可配置只上报特定类型异常（默认不上报 BusinessException）
- **traceId 集成**：支持框架 traceId 或 APM 平台 traceId
- **性能监控**：可选的事务追踪和采样率配置
- **环境标签**：自动附加环境、版本、自定义标签

## 配置项

### 基础配置

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `raf.sentry.enabled` | boolean | `false` | 是否启用 Sentry |
| `raf.sentry.dsn` | string | — | Sentry 项目 DSN（必填） |
| `raf.sentry.environment` | string | `${spring.profiles.active}` | 环境标识 |
| `raf.sentry.release` | string | `${spring.application.name}` | 发布版本 |
| `raf.sentry.debug` | boolean | `false` | 是否打印 Sentry 调试日志 |

### TraceId 集成

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `raf.sentry.trace-id-source` | enum | `framework` | traceId 来源：`framework`（框架）/ `apm`（APM 平台） |

`apm` 模式自动检测以下 APM 平台（通过 MDC）：

| APM 平台 | MDC Key |
|---|---|
| SkyWalking | `tid` |
| Zipkin | `X-B3-TraceId` |
| Jaeger | `trace_id` |
| Elastic APM | `trace.id` |
| Datadog | `dd.trace_id` |

### 异常过滤

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `raf.sentry.exception-filter.enabled` | boolean | `true` | 是否启用异常过滤 |
| `raf.sentry.exception-filter.include-exceptions` | list | `[SystemException, InfrastructureException]` | 需要上报的异常类型 |
| `raf.sentry.exception-filter.exclude-exceptions` | list | — | 需要排除的异常类型（优先级高于 include） |
| `raf.sentry.exception-filter.report-business-exception` | boolean | `false` | 是否上报 BusinessException |

### 性能监控

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `raf.sentry.enable-performance` | boolean | `false` | 是否启用性能追踪 |
| `raf.sentry.sample-rate` | double | `1.0` | 错误采样率（0.0-1.0） |
| `raf.sentry.traces-sample-rate` | double | `0.1` | 性能追踪采样率（0.0-1.0） |

### 附加配置

| 配置键 | 类型 | 说明 |
|---|---|---|
| `raf.sentry.tags` | map | 附加标签，如 `{team: backend, region: cn-north}` |
| `raf.sentry.send-default-pii` | boolean | 是否发送个人身份信息（默认 false） |

## 快速接入

```xml
<dependency>
    <groupId>io.github.jerryraf</groupId>
    <artifactId>raf-framework-sentry-starter</artifactId>
</dependency>
```

```yaml
raf:
  sentry:
    enabled: true
    dsn: https://your-key@sentry.io/project-id
    trace-id-source: apm   # 接入 SkyWalking 等 APM 平台时使用
    exception-filter:
      enabled: true
      report-business-exception: false
    enable-performance: true
    sample-rate: 1.0
    traces-sample-rate: 0.1
    tags:
      team: backend
      region: cn-north
```

## 核心用法

### 手动上报异常

```java
// 上报异常并附加上下文
try {
    externalService.call();
} catch (Exception e) {
    Sentry.withScope(scope -> {
        scope.setTag("service", "payment");
        scope.setExtra("orderId", orderId.toString());
        Sentry.captureException(e);
    });
    throw new InfrastructureException("支付服务调用失败", e);
}
```

### 上报自定义消息

```java
// 上报业务告警（非异常）
Sentry.withScope(scope -> {
    scope.setLevel(SentryLevel.WARNING);
    scope.setTag("type", "business-alert");
    Sentry.captureMessage("订单超时未支付，数量: " + count);
});
```

## 常见问题

**Q: Sentry 收不到错误，但服务确实抛出了异常？**

A: 检查 `exception-filter.include-exceptions` 是否包含了该异常类型。默认只上报 `SystemException` 和 `InfrastructureException`。

**Q: 生产环境和测试环境的错误混在一起？**

A: 确认 `raf.sentry.environment` 配置正确（如 `prod`/`test`），Sentry 控制台可按环境过滤。

**Q: traceId 在 Sentry 和 SkyWalking 中不一致？**

A: 将 `trace-id-source` 改为 `apm`，框架会自动从 SkyWalking 的 MDC（key: `tid`）读取 traceId，确保两者一致。
