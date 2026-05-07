# raf-example-nacos-starter

> 演示 `raf-framework-nacos-config-starter` + `raf-framework-nacos-discovery-starter` 的核心能力：动态配置刷新（@RefreshScope）、服务注册与发现、Feign 服务调用。

## 功能概述

### Nacos 配置中心（nacos-config-starter）

- **动态配置**：配置变更实时推送，无需重启服务
- **配置共享**：多服务共享 `base.yml` 等公共配置
- **配置加密**：配合 Jasypt 对敏感配置加密存储
- **多环境隔离**：通过 Namespace 隔离开发/测试/生产环境

### Nacos 服务发现（nacos-discovery-starter）

- **服务注册**：服务启动时自动注册到 Nacos
- **服务发现**：通过服务名调用，自动负载均衡
- **健康检查**：Nacos 定期检测服务实例健康状态
- **优雅下线**：服务停止时自动注销

## 快速接入

### 1. 引入依赖

```xml
<!-- Nacos 配置中心 -->
<dependency>
    <groupId>io.github.jerryraf</groupId>
    <artifactId>raf-framework-nacos-config-starter</artifactId>
</dependency>

<!-- Nacos 服务发现 -->
<dependency>
    <groupId>io.github.jerryraf</groupId>
    <artifactId>raf-framework-nacos-discovery-starter</artifactId>
</dependency>
```

### 2. 配置（`bootstrap.yml`）

> 注意：Nacos 配置必须在 `bootstrap.yml` 中配置，不能使用 `application.yml`，因为需要在 Spring 上下文初始化前加载。

```yaml
spring:
  application:
    name: raf-example-nacos-starter
  profiles:
    active: dev
  cloud:
    nacos:
      # 配置中心
      config:
        server-addr: ${NACOS_ADDR:127.0.0.1:8848}
        namespace: ${NACOS_NAMESPACE:}
        group: DEFAULT_GROUP
        file-extension: yaml
        # 共享配置（多服务共用）
        shared-configs:
          - data-id: base.yml
            group: DEFAULT_GROUP
            refresh: true
        # 服务专属配置
        extension-configs:
          - data-id: ${spring.application.name}.yml
            group: DEFAULT_GROUP
            refresh: true
      # 服务发现
      discovery:
        server-addr: ${NACOS_ADDR:127.0.0.1:8848}
        namespace: ${NACOS_NAMESPACE:}
        group: DEFAULT_GROUP
        metadata:
          version: 1.0.0
          env: ${spring.profiles.active}

server:
  port: 8080
  shutdown: graceful

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

### 3. 启动类

```java
@SpringBootApplication
@EnableDiscoveryClient  // 启用服务发现
public class NacosExampleApplication {
    public static void main(String[] args) {
        SpringApplication.run(NacosExampleApplication.class, args);
    }
}
```

## 配置项详解

### Nacos 配置中心

| 配置键 | 类型 | 说明 |
|---|---|---|
| `spring.cloud.nacos.config.server-addr` | string | Nacos 服务地址 |
| `spring.cloud.nacos.config.namespace` | string | 命名空间 ID（环境隔离） |
| `spring.cloud.nacos.config.group` | string | 配置分组，默认 `DEFAULT_GROUP` |
| `spring.cloud.nacos.config.file-extension` | string | 配置文件格式，默认 `yaml` |
| `spring.cloud.nacos.config.shared-configs` | list | 共享配置列表 |
| `spring.cloud.nacos.config.extension-configs` | list | 扩展配置列表 |

### Nacos 服务发现

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `spring.cloud.nacos.discovery.server-addr` | string | — | Nacos 服务地址 |
| `spring.cloud.nacos.discovery.namespace` | string | — | 命名空间 ID |
| `spring.cloud.nacos.discovery.group` | string | `DEFAULT_GROUP` | 服务分组 |
| `spring.cloud.nacos.discovery.weight` | float | `1.0` | 实例权重（负载均衡用） |
| `spring.cloud.nacos.discovery.metadata` | map | — | 实例元数据 |
| `spring.cloud.nacos.discovery.heart-beat-interval` | int | `5000` | 心跳间隔（毫秒） |
| `spring.cloud.nacos.discovery.ip` | string | — | 注册 IP（多网卡时指定） |

## 核心用法

### 动态配置刷新

```java
@Component
@RefreshScope  // 配置变更时自动刷新 Bean
@Slf4j
public class DynamicConfig {

    @Value("${app.title:RAF Framework}")
    private String title;

    @Value("${app.version:1.0.0}")
    private String version;

    @Value("${app.feature.enabled:false}")
    private boolean featureEnabled;

    public String getTitle() { return title; }
    public String getVersion() { return version; }
    public boolean isFeatureEnabled() { return featureEnabled; }
}
```

```java
@RestController
@RequestMapping("/config")
@ResponseResult
@RequiredArgsConstructor
public class ConfigController {

    private final DynamicConfig dynamicConfig;

    @GetMapping("/info")
    public Map<String, Object> getConfigInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("title", dynamicConfig.getTitle());
        info.put("version", dynamicConfig.getVersion());
        info.put("featureEnabled", dynamicConfig.isFeatureEnabled());
        return info;
    }
}
```

在 Nacos 控制台修改配置后，无需重启服务，`/config/info` 接口立即返回新值。

### 监听配置变更事件

```java
@Component
@Slf4j
public class ConfigChangeListener {

    @NacosConfigListener(dataId = "raf-example-nacos-starter.yml", groupId = "DEFAULT_GROUP")
    public void onConfigChange(String newConfig) {
        log.info("配置已更新，新内容长度: {}", newConfig.length());
        // 可在此处触发缓存刷新、连接池重建等操作
    }
}
```

### Feign 服务调用

```java
// 定义 Feign 客户端（name 必须与目标服务的 spring.application.name 完全一致）
@FeignClient(name = "user-service", path = "/api/user")
public interface UserServiceClient {

    @GetMapping("/{id}")
    RafResult<UserDTO> getUser(@PathVariable Long id);

    @PostMapping
    RafResult<UserDTO> createUser(@RequestBody UserCreateReq req);
}
```

```java
@Service
@RequiredArgsConstructor
public class UserAggregateService {

    private final UserServiceClient userServiceClient;

    public UserDTO getUserById(Long id) {
        RafResult<UserDTO> result = userServiceClient.getUser(id);
        if (!result.isSuccess()) {
            throw new InfrastructureException("用户服务调用失败: " + result.getMsg());
        }
        return result.getData();
    }
}
```

### Nacos 配置文件结构建议

```
Nacos 配置中心
├── base.yml（共享基础配置，所有服务共用）
│   ├── 日志配置
│   ├── 监控配置
│   └── 中间件地址占位符
├── raf-example-nacos-starter.yml（服务专属配置）
│   ├── raf.redis.*
│   ├── raf.rabbit.*
│   └── 业务配置
```

`base.yml` 示例内容：

```yaml
logging:
  level:
    root: INFO
    io.github.jerryraf: DEBUG

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

## 示例项目结构

```
raf-example-nacos-starter/
├── src/main/java/io/github/jerryraf/examples/nacos/
│   ├── NacosExampleApplication.java    # @EnableDiscoveryClient
│   ├── config/
│   │   └── DynamicConfig.java          # @RefreshScope 动态配置
│   └── controller/
│       └── ConfigController.java       # 查看当前配置值
└── src/main/resources/
    └── bootstrap.yml                   # Nacos 连接配置（必须是 bootstrap）
```

## 最佳实践

1. **环境隔离**：每个环境（dev/test/prod）使用独立的 Namespace，避免配置污染
2. **配置分层**：公共配置放 `base.yml`，服务专属配置放 `{service-name}.yml`
3. **敏感配置加密**：数据库密码、Redis 密码等使用 Jasypt 加密后存入 Nacos
4. **@RefreshScope 谨慎使用**：有状态的 Bean（如连接池）不要加 `@RefreshScope`，刷新会重建 Bean
5. **多网卡环境**：通过 `spring.cloud.nacos.discovery.ip` 显式指定注册 IP
6. **优雅停机**：配置 `server.shutdown: graceful`，确保 Nacos 注销后流量不再打入

## 常见问题

**Q: 服务启动时报 `No spring.config.import property has been defined`？**

A: Spring Cloud 2021+ 需要在 `bootstrap.yml` 中配置 Nacos，或在 `application.yml` 中添加 `spring.config.import: nacos:your-service.yml`。

**Q: 配置变更后 `@Value` 没有更新？**

A: 需要在 Bean 上加 `@RefreshScope` 注解。注意 `@RefreshScope` 会重新创建 Bean，有状态的 Bean 需要谨慎使用。

**Q: 多环境如何隔离？**

A: 使用 Nacos Namespace 隔离，每个环境（dev/test/prod）对应一个 Namespace ID，在 `bootstrap.yml` 中通过 `namespace` 配置。

**Q: 服务注册成功但 Feign 调用报 `No instances available`？**

A: 检查 Feign 客户端的 `name` 是否与目标服务的 `spring.application.name` 完全一致（大小写敏感），以及 namespace 和 group 是否匹配。

**Q: 多网卡环境下注册了错误的 IP？**

A: 通过 `spring.cloud.nacos.discovery.ip` 显式指定注册 IP，或通过 `spring.cloud.inetutils.preferred-networks` 配置优先网段。

**Q: 服务下线后 Nacos 还显示实例在线？**

A: 确认服务配置了优雅停机（`server.shutdown: graceful`），Nacos 心跳超时时间（默认 15 秒）内会自动摘除。
