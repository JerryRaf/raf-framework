# raf-example-monitor-starter

> 演示 `raf-framework-monitor-starter` 的核心能力：Prometheus 指标暴露、Druid 连接池指标、线程池监控、自定义业务指标。

## 功能概述

`raf-framework-monitor-starter` 提供：

- **Prometheus 端点**：`/actuator/prometheus` 暴露所有指标，供 Prometheus 抓取
- **Druid 连接池指标**：自动注册 75+ 连接池指标（活跃连接、等待队列、SQL 执行统计等）
- **线程池指标**：定时采集框架异步线程池的核心/活跃/队列指标
- **自定义业务指标**：通过 `MeterRegistry` 注册 Counter / Gauge / Timer
- **`@Timed` 注解**：方法级耗时自动采集

## 快速接入

### 1. 引入依赖

```xml
<dependency>
    <groupId>io.github.jerryraf</groupId>
    <artifactId>raf-framework-monitor-starter</artifactId>
</dependency>
```

### 2. 配置（`application.yml`）

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    prometheus:
      enabled: true
  metrics:
    tags:
      application: ${spring.application.name}   # 所有指标自动附加 application 标签

raf:
  executor:
    enabled: true
    core-pool-size: 10
    max-pool-size: 50
    queue-capacity: 200
    thread-name-prefix: raf-async-
    metric-enabled: true   # 启用线程池 Prometheus 指标采集
```

## 暴露的指标端点

| 端点 | 说明 |
|---|---|
| `GET /actuator/prometheus` | Prometheus 格式指标，供 Prometheus Server 抓取 |
| `GET /actuator/health` | 健康检查（含数据库、Redis 等组件状态） |
| `GET /actuator/metrics` | 所有指标名称列表 |
| `GET /actuator/metrics/{name}` | 查看单个指标详情 |
| `GET /actuator/info` | 应用信息 |

## 核心指标说明

### JVM 指标（自动采集）

| 指标名 | 说明 |
|---|---|
| `jvm.memory.used` | JVM 内存使用量（按区域） |
| `jvm.gc.pause` | GC 暂停时间 |
| `jvm.threads.live` | 活跃线程数 |
| `jvm.classes.loaded` | 已加载类数量 |

### HTTP 请求指标（自动采集）

| 指标名 | 说明 |
|---|---|
| `http.server.requests` | HTTP 请求总数、耗时分布（含 uri、method、status 标签） |

### Druid 连接池指标（框架自动注册）

| 指标名 | 说明 |
|---|---|
| `druid_active_count` | 当前活跃连接数 |
| `druid_pooling_count` | 当前空闲连接数 |
| `druid_wait_thread_count` | 等待获取连接的线程数 |
| `druid_execute_count` | SQL 执行总次数 |
| `druid_error_count` | SQL 执行错误次数 |
| `druid_commit_count` | 事务提交次数 |
| `druid_rollback_count` | 事务回滚次数 |

所有 Druid 指标均带有 `pool` 标签（值为数据源名称），支持多数据源区分。

### 线程池指标（框架自动采集）

> 需配置 `raf.executor.metricEnabled=true` 启用。

| 指标名 | 说明 |
|---|---|
| `thread.pool.core.size` | 核心线程数 |
| `thread.pool.max.size` | 最大线程数 |
| `thread.pool.active.size` | 当前活跃线程数 |
| `thread.pool.thread.count` | 当前线程池总线程数 |
| `thread.pool.queue.size` | 队列中等待的任务数 |
| `thread.pool.largest.size` | 历史最大线程数 |
| `thread.pool.completed.count` | 已完成任务总数 |

所有线程池指标均带有 `thread.pool.name` 标签（值为线程池名称）。

## 核心用法

### 自定义 Counter（计数器）

```java
@Autowired
private MeterRegistry meterRegistry;

// 注册计数器
Counter orderCounter = Counter.builder("business.order.created")
    .description("订单创建总数")
    .tag("service", "order")
    .register(meterRegistry);

// 业务代码中递增
orderCounter.increment();
```

### 自定义 Gauge（实时值）

```java
// 监控缓存大小
Map<String, Object> cache = new ConcurrentHashMap<>();
Gauge.builder("business.cache.size", cache, Map::size)
    .description("本地缓存条目数")
    .register(meterRegistry);
```

### 自定义 Timer（耗时）

```java
Timer timer = Timer.builder("business.payment.duration")
    .description("支付处理耗时")
    .tag("channel", "alipay")
    .register(meterRegistry);

timer.record(() -> {
    paymentService.process(req);
});
```

### @Timed 注解（方法级自动计时）

```java
@Timed(value = "business.order.create.time", description = "订单创建耗时")
public Order createOrder(OrderCreateReq req) {
    // 方法执行时间自动记录到 Prometheus
    return orderMapper.insert(req);
}
```

### Prometheus 抓取配置

在 `prometheus.yml` 中添加抓取任务：

```yaml
scrape_configs:
  - job_name: 'raf-example-monitor'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
    scrape_interval: 15s
```

### Grafana 推荐看板

| 看板 | Dashboard ID | 说明 |
|---|---|---|
| JVM Micrometer | 4701 | JVM 内存、GC、线程全览 |
| Spring Boot Statistics | 6756 | HTTP 请求、响应时间 |
| HikariCP / Druid | 自定义 | 连接池监控 |

## 示例项目结构

```
raf-example-monitor-starter/
├── src/main/java/io/github/jerryraf/examples/monitor/
│   ├── MonitorExampleApplication.java
│   └── controller/
│       └── MonitorDemoController.java  # 业务指标演示 + 线程池状态查询
└── src/main/resources/
    ├── application.yml
    └── schema.sql
```

## API 接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/monitor/orders` | 模拟创建订单，触发业务计数器 |
| GET | `/api/monitor/thread-pools` | 查看线程池当前状态 |
| GET | `/api/monitor/endpoints` | 查看所有监控端点说明 |
| GET | `/actuator/prometheus` | Prometheus 指标抓取端点 |
| GET | `/actuator/health` | 健康检查 |

## 最佳实践

1. **标签设计**：为所有自定义指标添加 `application` 标签（已通过 `management.metrics.tags` 全局配置），便于多实例区分
2. **指标命名**：遵循 `{domain}.{entity}.{action}` 格式，如 `business.order.created`
3. **Gauge 注意**：Gauge 持有对象引用，避免注册短生命周期对象导致内存泄漏
4. **生产安全**：`/actuator/prometheus` 端点应配置 IP 白名单或 Basic Auth，避免暴露给外部
5. **告警规则**：在 Prometheus 中配置告警，如 `executor.queued > 80%` 触发线程池积压告警

## 常见问题

**Q: `/actuator/prometheus` 返回 404？**

A: 检查 `management.endpoints.web.exposure.include` 是否包含 `prometheus`，以及 `management.endpoint.prometheus.enabled: true`。

**Q: Druid 指标没有出现在 Prometheus 端点？**

A: 确认引入了 `raf-framework-mybatis-starter` 或 `raf-framework-datasource-starter`（包含 Druid），且数据源已初始化。

**Q: 自定义 Counter 在重启后归零？**

A: Prometheus 的 Counter 是进程内计数，重启后从 0 开始。Prometheus 通过 `increase()` 函数计算增量，不受重启影响。

**Q: 线程池指标不更新？**

A: 确认已配置 `raf.executor.metric-enabled=true`。框架通过 `@Scheduled` 每 5 秒采集一次线程池指标，指标名称格式为 `thread.pool.*`，带有 `thread.pool.name` 标签。
