# Changelog

All notable changes to RAF Framework will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [3.0.0] - 2026-04-27

### Added
- `ContextHolder.clearContext()` — clears all thread-local context and MDC entries; call at request/task boundaries to prevent leaks
- `BatchThreadHandlerUtil.executeWithThreadPool(List, service, batchSize, Executor)` — new overload accepting a managed executor to avoid creating ad-hoc thread pools per call
- `DtMockConfig` — functional mock `ThreadPoolExecutor` beans for `dev-mock` profile (previously returned `null`)
- `HttpResponseInterceptor.afterCompletion()` — cleans up `ContextHolder` and MDC after each request

### Fixed
- `DruidMetricsConfig` — was entirely commented out; restored and enabled via `raf.monitor.druid.enabled=true`
- `CommonPoolMetricsConfig` — `bindMetrics()` was commented out; restored with proper Micrometer gauge registration
- `HttpResponseInterceptor` — traceId was written to MDC only, not to `ContextHolder`; now both are kept in sync
- `AuditLogUtil` — static field `json = SpringContext.getBean()` caused potential NPE at class-loading time; changed to lazy initialization
- `ApiVersionHandlerMapping` — used reflection to mutate annotation `valueCache` (fragile, JDK-version-dependent); replaced with `RequestMappingInfo.Builder` to construct resolved paths cleanly
- `BatchThreadHandlerUtil` — created a new `Executors.newFixedThreadPool` on every call; now accepts an `Executor` parameter; ad-hoc pool creation is isolated to the legacy overload
- Duplicate `ThreadPoolHolder` in `raf-framework-web-starter` removed; all usages now point to `com.raf.framework.core.spring.async.ThreadPoolHolder`
- `ThreadPoolMetrics` — fixed duplicate metric key `thread.pool.largest.size` (was registered twice, second should be `thread.pool.completed.count`)

### Changed
- `GlobalExceptionConfig` — all log messages translated to English; removed commented-out Sentry stub; catch-all handler simplified
- `ContextHolder` — removed `//todo` comment; added full Javadoc; `clearTraceId()` retained for backward compatibility
- `RocketMqProducer` — all Chinese Javadoc and log messages translated to English
- `AbstractRocketMqConsumerListener` — all Chinese log messages translated to English
- `RocketMqConfig` — all Chinese log messages translated to English
- `MultiLevelCacheService` — all Chinese comments and Javadoc translated to English; improved log messages
- `RabbitMqMessageSender` — Chinese log messages translated to English; removed stale Chinese Javadoc
- `AbstractRabbitSenderConfirm` — Chinese log messages translated to English
- `RabbitMessageCacheMgr` — Chinese log messages translated to English; added `Thread.currentThread().interrupt()` on `InterruptedException`
- `AbstractRabbitConsumerListener` — Chinese method Javadoc translated to English
- `ThreadPoolConfig` — async uncaught exception handler log message translated to English
- `ThreadPoolMetrics` — log level changed from `INFO` to `DEBUG` for per-tick metrics output
- README — architecture diagram corrected: `raf-framework-autoconfigure` → `raf-framework-core`

---

## [2.x] - Legacy

See git history for changes prior to 3.0.0.
