# RAF Framework Sentry Starter

企业级 Sentry 错误追踪集成，提供完整的异常监控、性能追踪和 APM 平台集成能力。

## 特性

- ✅ **多种 TraceId 来源**：支持框架自定义和 APM 平台两种模式
- ✅ **智能异常过滤**：默认只上报 SystemException 和 InfrastructureException
- ✅ **APM 平台集成**：自动检测 SkyWalking、Zipkin、Jaeger、Elastic APM、Datadog
- ✅ **性能监控**：可选的性能追踪，支持灵活的采样率配置
- ✅ **TraceId 一致性**：确保日志、APM、Sentry 使用统一的 traceId
- ✅ **Spring Boot 3 原生支持**：基于 Sentry Spring Boot Starter 8.x

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>io.github.jerryraf</groupId>
    <artifactId>raf-framework-sentry-starter</artifactId>
</dependency>
```

### 2. 配置 Sentry

```yaml
raf:
  sentry:
    enabled: true
    dsn: https://your-key@sentry.io/project-id
    trace-id-source: framework  # 或 apm
```

### 3. 测试

```java
@RestController
public class TestController {
    
    @GetMapping("/test-error")
    public void testError() {
        // SystemException 会自动上报到 Sentry
        throw new SystemException("测试系统异常");
    }
}
```

## 配置说明

### 基础配置

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `raf.sentry.enabled` | Boolean | false | 是否启用 Sentry |
| `raf.sentry.dsn` | String | - | Sentry 项目 DSN（必填） |
| `raf.sentry.environment` | String | ${spring.profiles.active} | 环境标识 |
| `raf.sentry.release` | String | ${spring.application.name} | 发布版本 |
| `raf.sentry.debug` | Boolean | false | 是否打印调试日志 |

### TraceId 集成

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `raf.sentry.trace-id-source` | Enum | framework | traceId 来源：framework / apm |

**framework 模式**：使用框架的 `ContextHolder.getTraceId()`
- 适用于未接入 APM 平台的场景
- 框架自动生成和管理 traceId

**apm 模式**：使用 APM 平台的 traceId
- 自动检测以下 APM 平台：
  - SkyWalking（MDC key: `tid`）
  - Zipkin（MDC key: `X-B3-TraceId`）
  - Jaeger（MDC key: `trace_id`）
  - Elastic APM（MDC key: `trace.id`）
  - Datadog（MDC key: `dd.trace_id`）
- 确保 Sentry、日志、APM 平台使用统一的 traceId

### 异常过滤

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `raf.sentry.exception-filter.enabled` | Boolean | true | 是否启用异常过滤 |
| `raf.sentry.exception-filter.include-exceptions` | Set<String> | SystemException, InfrastructureException | 需要上报的异常类型 |
| `raf.sentry.exception-filter.exclude-exceptions` | Set<String> | [] | 需要排除的异常类型 |
| `raf.sentry.exception-filter.report-business-exception` | Boolean | false | 是否上报 BusinessException |

### 性能监控

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `raf.sentry.enable-performance` | Boolean | false | 是否启用性能追踪 |
| `raf.sentry.sample-rate` | Double | 1.0 | 错误采样率（0.0-1.0） |
| `raf.sentry.traces-sample-rate` | Double | 0.1 | 性能追踪采样率（0.0-1.0） |

## 完整配置示例

```yaml
raf:
  sentry:
    # 基础配置
    enabled: true
    dsn: https://your-key@sentry.io/project-id
    environment: production
    release: my-app-v1.0.0
    debug: false
    
    # TraceId 集成（推荐使用 apm）
    trace-id-source: apm
    
    # 异常过滤
    exception-filter:
      enabled: true
      report-business-exception: false
      include-exceptions:
        - com.raf.framework.autoconfigure.common.exception.SystemException
        - com.raf.framework.autoconfigure.common.exception.InfrastructureException
      exclude-exceptions:
        - com.example.RateLimitException  # 排除限流异常
    
    # 性能监控
    enable-performance: true
    sample-rate: 1.0          # 100% 错误采样
    traces-sample-rate: 0.1   # 10% 性能追踪采样
    
    # 附加标签
    tags:
      team: backend
      region: cn-north
      version: 3.0.0
```

## 使用场景

### 场景 1：未接入 APM 平台

```yaml
raf:
  sentry:
    enabled: true
    dsn: https://your-key@sentry.io/project-id
    trace-id-source: framework  # 使用框架自定义 traceId
```

### 场景 2：已接入 SkyWalking

```yaml
raf:
  sentry:
    enabled: true
    dsn: https://your-key@sentry.io/project-id
    trace-id-source: apm  # 使用 SkyWalking 的 traceId
```

此时 Sentry 会自动从 MDC 中读取 SkyWalking 的 `tid`，确保 traceId 一致性。

### 场景 3：只上报特定异常

```yaml
raf:
  sentry:
    enabled: true
    dsn: https://your-key@sentry.io/project-id
    exception-filter:
      enabled: true
      include-exceptions:
        - com.example.CriticalException  # 只上报关键异常
```

### 场景 4：启用性能监控

```yaml
raf:
  sentry:
    enabled: true
    dsn: https://your-key@sentry.io/project-id
    enable-performance: true
    traces-sample-rate: 0.2  # 20% 采样率
```

## 最佳实践

### 1. 生产环境配置

```yaml
raf:
  sentry:
    enabled: true
    dsn: ${SENTRY_DSN}  # 从环境变量读取
    trace-id-source: apm  # 使用 APM 平台的 traceId
    exception-filter:
      report-business-exception: false  # 不上报业务异常
    enable-performance: true
    sample-rate: 1.0
    traces-sample-rate: 0.1  # 低采样率，避免性能开销
```

### 2. 开发环境配置

```yaml
raf:
  sentry:
    enabled: false  # 开发环境关闭 Sentry
    # 或者使用独立的开发环境 DSN
    # dsn: https://dev-key@sentry.io/dev-project-id
    # debug: true
```

### 3. 异常过滤策略

- **默认策略**：只上报 SystemException 和 InfrastructureException
- **BusinessException**：业务异常通常是预期内的错误，不需要告警
- **排除特定异常**：使用 `exclude-exceptions` 排除不需要告警的异常（如限流、熔断）

### 4. 性能监控采样率

- **错误采样**：建议 100%（`sample-rate: 1.0`）
- **性能追踪**：建议 10%-20%（`traces-sample-rate: 0.1-0.2`），避免性能开销

### 5. TraceId 一致性

生产环境推荐使用 `trace-id-source: apm`，确保：
- 日志中的 traceId
- APM 平台的 traceId
- Sentry 的 traceId

三者保持一致，便于问题排查和关联分析。

## 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                    Application Layer                        │
│  (抛出异常: SystemException, InfrastructureException)        │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│              GlobalExceptionHandler                         │
│  (捕获异常，触发 Sentry 上报)                                 │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│           RafSentryEventProcessor                           │
│  1. 注入 traceId (from TraceIdProvider)                     │
│  2. 异常过滤 (ExceptionFilter)                               │
│  3. 增强上下文 (tags, contexts)                              │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│              TraceIdProvider                                │
│  ┌──────────────────┐  ┌──────────────────┐                │
│  │ Framework        │  │ APM              │                │
│  │ (ContextHolder)  │  │ (MDC)            │                │
│  └──────────────────┘  └──────────────────┘                │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                  Sentry Platform                            │
│  (错误追踪、性能监控、告警)                                    │
└─────────────────────────────────────────────────────────────┘
```

## 依赖版本

- Sentry: 8.7.0
- Spring Boot: 3.4.7
- Java: 17+

## 常见问题

### Q1: Sentry 没有收到错误事件？

**检查清单：**
1. 确认 `raf.sentry.enabled=true`
2. 确认 DSN 配置正确
3. 检查异常类型是否在 `include-exceptions` 中
4. 检查是否被 `exclude-exceptions` 排除
5. 查看日志中是否有 "Sentry event filtered" 信息

### Q2: TraceId 不一致？

**解决方案：**
- 确认使用 `trace-id-source: apm`
- 确认 APM 平台已正确配置
- 检查 MDC 中是否有 traceId（通过日志验证）

### Q3: 性能监控数据太多？

**解决方案：**
- 降低 `traces-sample-rate`（建议 0.1-0.2）
- 或者关闭性能监控 `enable-performance: false`

### Q4: BusinessException 需要上报？

**解决方案：**
```yaml
raf:
  sentry:
    exception-filter:
      report-business-exception: true
```

## 更新日志

### v3.0.0 (2026-04-20)

- ✨ 全面重构，升级到 Sentry 8.7.0
- ✨ 支持多种 TraceId 来源（framework / apm）
- ✨ 智能异常过滤策略
- ✨ 完整的 APM 平台集成
- ✨ 性能监控支持
- ✨ Spring Boot 3 原生支持

## 许可证

MIT License
