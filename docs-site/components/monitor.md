# Prometheus 监控（monitor-starter）

## 功能概述

- **Micrometer 集成**：自动暴露 JVM、HTTP、线程池等指标
- **Prometheus 格式**：`/actuator/prometheus` 端点输出指标
- **自定义指标**：支持业务指标上报
- **线程池指标**：框架异步线程池自动注册 Prometheus 指标

## 配置项

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `raf.monitor.enabled` | boolean | `false` | 是否启用监控 |
| `management.endpoints.web.exposure.include` | string | `health,info` | 暴露的 Actuator 端点 |
| `management.endpoint.prometheus.enabled` | boolean | `true` | 是否启用 Prometheus 端点 |
| `management.metrics.tags.application` | string | `${spring.application.name}` | 指标应用标签 |

## 快速接入

```xml
<dependency>
    <groupId>io.github.jerryraf</groupId>
    <artifactId>raf-framework-monitor-starter</artifactId>
</dependency>
```

```yaml
raf:
  monitor:
    enabled: true

management:
  endpoints:
    web:
      exposure:
        include: health, info, prometheus, metrics
  endpoint:
    prometheus:
      enabled: true
    health:
      show-details: always
  metrics:
    tags:
      application: ${spring.application.name}
      env: ${spring.profiles.active}
```

## 核心用法

### 自定义业务指标

```java
@Component
public class OrderMetrics {

    private final Counter orderCreatedCounter;
    private final Timer orderProcessTimer;

    public OrderMetrics(MeterRegistry registry) {
        this.orderCreatedCounter = Counter.builder("order.created.total")
            .description("创建订单总数")
            .tag("type", "normal")
            .register(registry);

        this.orderProcessTimer = Timer.builder("order.process.duration")
            .description("订单处理耗时")
            .register(registry);
    }

    public void recordOrderCreated() {
        orderCreatedCounter.increment();
    }

    public void recordOrderProcess(Runnable task) {
        orderProcessTimer.record(task);
    }
}
```

### Prometheus 采集配置

```yaml
# prometheus.yml（Prometheus 服务端配置）
scrape_configs:
  - job_name: 'your-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['your-service-host:8080']
    scrape_interval: 15s
```

## 常见问题

**Q: `/actuator/prometheus` 返回 404？**

A: 确认 `management.endpoints.web.exposure.include` 包含 `prometheus`，且 `raf.monitor.enabled: true`。

**Q: 线程池指标在 Prometheus 中找不到？**

A: 框架异步线程池（`raf.async`）启用后自动注册指标，指标名称格式为 `executor_{poolName}_*`。

**Q: 如何在 Grafana 中展示这些指标？**

A: 导入 Spring Boot 官方 Grafana Dashboard（ID: 4701），或根据指标名称自定义 Panel。
