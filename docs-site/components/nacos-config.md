# Nacos 配置中心（nacos-config-starter）

## 功能概述

- **动态配置**：配置变更实时推送，无需重启服务
- **配置共享**：多服务共享 `base.yml` 等公共配置
- **配置加密**：配合 Jasypt 对敏感配置加密存储
- **多环境隔离**：通过 Namespace 隔离开发/测试/生产环境

## 配置项

Nacos 配置中心通过 `spring.config.import` 或 `bootstrap.yml` 配置：

| 配置键 | 类型 | 说明 |
|---|---|---|
| `spring.cloud.nacos.config.server-addr` | string | Nacos 服务地址 |
| `spring.cloud.nacos.config.namespace` | string | 命名空间 ID（环境隔离） |
| `spring.cloud.nacos.config.group` | string | 配置分组，默认 `DEFAULT_GROUP` |
| `spring.cloud.nacos.config.file-extension` | string | 配置文件格式，默认 `yaml` |
| `spring.cloud.nacos.config.shared-configs` | list | 共享配置列表 |
| `spring.cloud.nacos.config.extension-configs` | list | 扩展配置列表 |

## 快速接入

**1. 引入依赖**

```xml
<dependency>
    <groupId>io.github.jerryraf</groupId>
    <artifactId>raf-framework-nacos-config-starter</artifactId>
</dependency>
```

**2. 配置**（`bootstrap.yml`，注意必须是 bootstrap 而非 application）

```yaml
spring:
  application:
    name: your-service-name
  profiles:
    active: dev
  cloud:
    nacos:
      config:
        server-addr: 127.0.0.1:8848
        namespace: your-dev-namespace-id
        file-extension: yaml
        shared-configs:
          - data-id: base.yml
            group: DEFAULT_GROUP
            refresh: true
        extension-configs:
          - data-id: ${spring.application.name}.yml
            group: DEFAULT_GROUP
            refresh: true
```

## 核心用法

### 动态刷新配置

```java
@Component
@RefreshScope  // 配置变更时自动刷新 Bean
public class AppConfig {

    @Value("${app.feature.enabled:false}")
    private boolean featureEnabled;

    @Value("${app.rate-limit:100}")
    private int rateLimit;
}
```

### 监听配置变更

```java
@Component
public class ConfigChangeListener {

    @NacosConfigListener(dataId = "your-service.yml", groupId = "DEFAULT_GROUP")
    public void onConfigChange(String newConfig) {
        log.info("配置已更新: {}", newConfig);
        // 处理配置变更逻辑
    }
}
```

### 配置文件结构建议

```
Nacos 配置中心
├── base.yml（共享基础配置）
│   ├── 日志配置
│   ├── 监控配置
│   └── 中间件地址占位符
├── your-service.yml（服务专属配置）
│   ├── raf.redis.*
│   ├── raf.rabbit.*
│   └── 业务配置
```

## 常见问题

**Q: 服务启动时报 `No spring.config.import property has been defined`？**

A: Spring Cloud 2021+ 需要在 `bootstrap.yml` 中配置 Nacos，或在 `application.yml` 中添加 `spring.config.import: nacos:your-service.yml`。

**Q: 配置变更后 `@Value` 没有更新？**

A: 需要在 Bean 上加 `@RefreshScope` 注解。注意 `@RefreshScope` 会重新创建 Bean，有状态的 Bean 需要谨慎使用。

**Q: 多环境如何隔离？**

A: 使用 Nacos Namespace 隔离，每个环境（dev/test/prod）对应一个 Namespace ID，在 `bootstrap.yml` 中通过 `namespace` 配置。
