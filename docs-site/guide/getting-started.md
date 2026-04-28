# 快速开始

## 环境要求

- JDK 17+
- Maven 3.8.8+
- Spring Boot 3.x

## 引入方式

### 方式 A：Parent 继承（推荐）

```xml
<parent>
  <groupId>io.github.jerryraf</groupId>
  <artifactId>raf-framework-parent</artifactId>
  <version>3.0.0</version>
</parent>
```

继承后自动获得：Spring Boot 3.x 依赖管理、统一编译配置、JaCoCo 覆盖率插件。

### 方式 B：BOM 组合

已有自定义 parent 时，通过 BOM 导入依赖管理：

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>io.github.jerryraf</groupId>
      <artifactId>raf-framework-dependencies</artifactId>
      <version>3.0.0</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

## 按需引入 Starter

所有 Starter 均为**可选**，不引入则零副作用。

### Web（统一响应 + 异常处理）

```xml
<dependency>
  <groupId>io.github.jerryraf</groupId>
  <artifactId>raf-framework-web-starter</artifactId>
</dependency>
```

提供：`RafResult<T>` 统一响应、`BusinessException` / `InfrastructureException` 异常分层、`@Valid` 参数校验自动返回 `code=10700`、`traceId` 自动注入。

### Redis + 分布式锁

```xml
<dependency>
  <groupId>io.github.jerryraf</groupId>
  <artifactId>raf-framework-redis-starter</artifactId>
</dependency>
```

提供：`RedisService` 封装（String / Hash / List / ZSet）、缓存穿透/雪崩防护、Redisson 分布式锁。

### MyBatis-Plus + 多数据源

```xml
<dependency>
  <groupId>io.github.jerryraf</groupId>
  <artifactId>raf-framework-mybatis-starter</artifactId>
</dependency>
<dependency>
  <groupId>io.github.jerryraf</groupId>
  <artifactId>raf-framework-datasource-starter</artifactId>
</dependency>
```

提供：MyBatis-Plus 分页、`@DsSelector` 多数据源路由（主库写/从库读）、慢 SQL 告警。

### RocketMQ 幂等消息

```xml
<dependency>
  <groupId>io.github.jerryraf</groupId>
  <artifactId>raf-framework-rocketmq-starter</artifactId>
</dependency>
```

提供：幂等生产者（业务唯一键去重）、幂等消费者（消费前检查）、消费失败重试。

### API 网关安全

```xml
<dependency>
  <groupId>io.github.jerryraf</groupId>
  <artifactId>raf-framework-gateway-starter</artifactId>
</dependency>
```

提供：ECIES 非对称加密、ECDSA 签名验证、防重放攻击（Nonce + 时间窗口）。

### Dubbo RPC

```xml
<dependency>
  <groupId>io.github.jerryraf</groupId>
  <artifactId>raf-framework-dubbo-starter</artifactId>
</dependency>
```

提供：`@DubboService` / `@DubboReference` 标准配置、traceId 在 RPC 链路中透明传播、`BusinessException` 跨服务传播。

## 启用组件

在 `application.yml` 中显式启用所需组件：

```yaml
raf:
  redis:
    enabled: true
  executor:
    enabled: true
  log:
    enabled: true
```

不引入对应 Starter 或不配置 `enabled: true`，则该组件零副作用，不会加载任何 Bean。

## 第一个接口

```java
@RestController
@RequestMapping("/users")
public class UserController {

    @GetMapping("/{id}")
    public RafResult<UserVO> getUser(@PathVariable Long id) {
        // 业务逻辑...
        return RafResult.success(userVO);
    }
}
```

响应自动包装为：

```json
{
  "code": "0",
  "message": "success",
  "data": { "id": 1, "name": "zhangsan" },
  "traceId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d"
}
```

## 下一步

- [API 响应与错误处理规范](/components/api-response) — 了解业务码设计
- [App API 安全设计](/components/api-security) — 了解加密通信协议
- [Gateway 示例](/examples/gateway) — 运行完整网关示例
