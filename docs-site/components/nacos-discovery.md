# Nacos 服务发现（nacos-discovery-starter）

## 功能概述

- **服务注册**：服务启动时自动注册到 Nacos
- **服务发现**：通过服务名调用，自动负载均衡
- **健康检查**：Nacos 定期检测服务实例健康状态
- **优雅下线**：服务停止时自动注销，避免流量打到已停止的实例

## 配置项

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `spring.cloud.nacos.discovery.server-addr` | string | — | Nacos 服务地址 |
| `spring.cloud.nacos.discovery.namespace` | string | — | 命名空间 ID |
| `spring.cloud.nacos.discovery.group` | string | `DEFAULT_GROUP` | 服务分组 |
| `spring.cloud.nacos.discovery.service` | string | `${spring.application.name}` | 服务名 |
| `spring.cloud.nacos.discovery.weight` | float | `1.0` | 实例权重（负载均衡用） |
| `spring.cloud.nacos.discovery.metadata` | map | — | 实例元数据（如版本号、环境标签） |
| `spring.cloud.nacos.discovery.heart-beat-interval` | int | `5000` | 心跳间隔（毫秒） |
| `spring.cloud.nacos.discovery.ip` | string | — | 注册 IP（多网卡时指定） |

## 快速接入

**1. 引入依赖**

```xml
<dependency>
    <groupId>io.github.jerryraf</groupId>
    <artifactId>raf-framework-nacos-discovery-starter</artifactId>
</dependency>
```

**2. 配置**（`bootstrap.yml`）

```yaml
spring:
  application:
    name: your-service-name
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
        namespace: your-namespace-id
        group: DEFAULT_GROUP
        metadata:
          version: 1.0.0
          env: dev
```

## 核心用法

### Feign 服务调用

```java
// 定义 Feign 客户端
@FeignClient(name = "user-service", path = "/api/user")
public interface UserServiceClient {

    @GetMapping("/{id}")
    RafResult<UserDTO> getUser(@PathVariable Long id);
}

// 注入使用
@Autowired
private UserServiceClient userServiceClient;

public UserDTO getUserById(Long id) {
    RafResult<UserDTO> result = userServiceClient.getUser(id);
    if (!result.isSuccess()) {
        throw new InfrastructureException("用户服务调用失败: " + result.getMsg());
    }
    return result.getData();
}
```

### 优雅停机配置

```yaml
server:
  shutdown: graceful  # 优雅停机（等待当前请求处理完成）

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s  # 最长等待 30 秒
```

### 多网卡环境指定注册 IP

```yaml
spring:
  cloud:
    nacos:
      discovery:
        ip: 192.168.1.100  # 指定注册到 Nacos 的 IP
```

## 常见问题

**Q: 服务注册成功但 Feign 调用报 `No instances available`？**

A: 检查 Feign 客户端的 `name` 是否与目标服务的 `spring.application.name` 完全一致（大小写敏感），以及 namespace 和 group 是否匹配。

**Q: 多网卡环境下注册了错误的 IP？**

A: 通过 `spring.cloud.nacos.discovery.ip` 显式指定注册 IP，或通过 `spring.cloud.inetutils.preferred-networks` 配置优先网段。

**Q: 服务下线后 Nacos 还显示实例在线？**

A: 确认服务配置了优雅停机（`server.shutdown: graceful`），且 Nacos 心跳超时时间（默认 15 秒）内会自动摘除。
