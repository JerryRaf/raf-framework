# raf-example-sentry-starter

> 演示 `raf-framework-sentry-starter` 的核心能力：异常分层上报策略、traceId 关联、异常过滤配置、APM 平台集成。

## 功能概述

`raf-framework-sentry-starter` 提供：

- **异常分层上报**：`SystemException` / `InfrastructureException` 自动上报，`BusinessException` 默认不上报
- **traceId 关联**：每条 Sentry 事件自动注入 traceId，与日志、APM 平台保持一致
- **异常过滤**：白名单（include）+ 黑名单（exclude）精细控制上报范围
- **APM 集成**：支持 SkyWalking / Zipkin / Jaeger / Elastic APM / Datadog 的 traceId 读取
- **性能监控**：可选启用 Sentry Performance，追踪慢请求和事务耗时

## 快速接入

### 1. 引入依赖

```xml
<dependency>
    <groupId>io.github.jerryraf</groupId>
    <artifactId>raf-framework-sentry-starter</artifactId>
</dependency>
```

### 2. 配置（`application.yml`）

```yaml
raf:
  sentry:
    enabled: true
    dsn: ${SENTRY_DSN}                  # 必填，从 Sentry 项目设置获取
    environment: ${spring.profiles.active:dev}
    release: ${spring.application.name}
    trace-id-source: framework          # framework | apm
    exception-filter:
      enabled: true
      report-business-exception: false  # 业务异常不上报（默认）
    enable-performance: false
    sample-rate: 1.0
    tags:
      team: backend
      region: cn-north
```

## 配置项详解

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `raf.sentry.enabled` | boolean | `false` | 是否启用 Sentry |
| `raf.sentry.dsn` | string | — | Sentry 项目 DSN（必填） |
| `raf.sentry.environment` | string | 从 profiles.active 获取 | 环境标识（dev/test/prod） |
| `raf.sentry.release` | string | 从 application.name 获取 | 发布版本 |
| `raf.sentry.trace-id-source` | enum | `framework` | traceId 来源：`framework` / `apm` |
| `raf.sentry.debug` | boolean | `false` | 是否打印 Sentry 内部日志 |
| `raf.sentry.send-default-pii` | boolean | `false` | 是否发送个人身份信息 |
| `raf.sentry.enable-performance` | boolean | `false` | 是否启用性能监控 |
| `raf.sentry.sample-rate` | double | `1.0` | 错误采样率（0.0-1.0） |
| `raf.sentry.traces-sample-rate` | double | `0.1` | 性能追踪采样率 |
| `raf.sentry.tags` | map | — | 附加标签（如 team、region） |

### 异常过滤（exception-filter）

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `enabled` | boolean | `true` | 是否启用异常过滤 |
| `include-exceptions` | set | SystemException, InfrastructureException | 需要上报的异常类型（全限定类名） |
| `exclude-exceptions` | set | — | 需要排除的异常类型（优先级高于 include） |
| `report-business-exception` | boolean | `false` | 是否上报 BusinessException |

## 异常上报策略

框架四层异常的默认上报行为：

| 异常类型 | HTTP 状态 | 默认上报 | 说明 |
|---|---|---|---|
| `BusinessException` | 200 | ❌ 不上报 | 业务预期内的错误，不需要告警 |
| `InfrastructureException` | 500 | ✅ 上报 | 第三方服务/数据库异常，需要关注 |
| `SystemException` | 500 | ✅ 上报 | 未预期错误，必须告警 |
| `ProtocolException` | 401 | ❌ 不上报 | 认证失败，通常是客户端问题 |

## traceId 集成方案

### 方案一：框架自定义追踪（默认）

```yaml
raf:
  sentry:
    trace-id-source: framework
```

使用 `ContextHolder.getTraceId()`，适用于未接入 APM 平台的场景。

### 方案二：APM 平台集成

```yaml
raf:
  sentry:
    trace-id-source: apm
```

自动检测并读取以下 APM 平台的 traceId：

| APM 平台 | MDC Key |
|---|---|
| SkyWalking | `tid` |
| Zipkin | `X-B3-TraceId` |
| Jaeger | `trace_id` |
| Elastic APM | `trace.id` |
| Datadog | `dd.trace_id` |
| 通用 | `traceId` |

**最佳实践**：生产环境接入 SkyWalking 后，配置 `trace-id-source: apm`，确保 Sentry 事件、日志、APM 链路使用同一个 traceId，一键定位问题。

## 核心用法

### 异常自动上报

框架的全局异常处理器（`GlobalExceptionConfig`）会自动将符合条件的异常上报到 Sentry，业务代码无需任何改动：

```java
// 这两种异常会自动上报到 Sentry
throw new InfrastructureException("Redis 连接超时");
throw new SystemException("未知错误", e);

// 这种不会上报（业务异常）
throw new BusinessException(AppResponseEnum.USER_NOT_FOUND);
```

### 手动上报（特殊场景）

```java
import io.sentry.Sentry;

// 手动捕获异常并上报
try {
    riskyOperation();
} catch (Exception e) {
    Sentry.captureException(e);
    log.error("操作失败", e);
}

// 上报自定义消息
Sentry.captureMessage("重要业务事件：订单量超过阈值");
```

### 附加上下文信息

```java
import io.sentry.Sentry;
import io.sentry.protocol.User;

// 设置用户信息（注意 send-default-pii 需为 true）
User user = new User();
user.setId(String.valueOf(userId));
Sentry.setUser(user);

// 附加额外数据
Sentry.configureScope(scope -> {
    scope.setTag("orderId", orderId);
    scope.setExtra("requestBody", requestJson);
});
```

### 排除特定异常

```yaml
raf:
  sentry:
    exception-filter:
      exclude-exceptions:
        # 限流异常不需要告警（预期内的流量控制）
        - com.alibaba.csp.sentinel.slots.block.BlockException
        # 参数校验异常不需要告警
        - org.springframework.web.bind.MethodArgumentNotValidException
```

## 示例项目结构

```
raf-example-sentry-starter/
├── src/main/java/io/github/jerryraf/examples/sentry/
│   ├── SentryExampleApplication.java
│   └── controller/
│       └── ErrorDemoController.java    # 触发不同异常类型的演示接口
└── src/main/resources/
    └── application.yml
```

## API 接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/demo/business-error` | 触发 BusinessException（不上报） |
| GET | `/api/demo/infra-error` | 触发 InfrastructureException（上报） |
| GET | `/api/demo/system-error` | 触发 SystemException（上报，含堆栈） |
| GET | `/api/demo/ok` | 正常请求，验证 traceId 注入 |

## 最佳实践

1. **生产环境**：`trace-id-source: apm`，确保 Sentry、日志、APM 三端 traceId 一致
2. **采样率**：错误采样 `sample-rate: 1.0`（全量），性能追踪 `traces-sample-rate: 0.1`（10%）
3. **环境隔离**：dev/test 环境的 DSN 与 prod 分开，避免测试噪音污染生产告警
4. **告警分级**：在 Sentry 项目中配置告警规则，`SystemException` 立即通知，`InfrastructureException` 聚合后通知
5. **敏感信息**：`send-default-pii: false`（默认），不发送 IP、Cookie 等个人信息

## 常见问题

**Q: 本地开发时不想上报到 Sentry 怎么办？**

A: 设置 `raf.sentry.enabled: false`，或将 DSN 设为空字符串，Sentry SDK 会自动禁用。

**Q: Sentry 事件中看不到 traceId？**

A: 检查 `trace-id-source` 配置是否与实际使用的追踪方案匹配。使用框架追踪时确认请求经过了 Web 过滤器（traceId 在请求进入时生成）。

**Q: 如何验证 Sentry 配置是否生效？**

A: 设置 `raf.sentry.debug: true`，启动时控制台会打印 Sentry 初始化日志；调用 `/api/demo/infra-error` 接口，在 Sentry 控制台确认事件是否到达。
