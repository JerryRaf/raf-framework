# raf-example-dubbo-starter

> 演示 `raf-framework-dubbo-starter` 的核心能力：Dubbo Provider/Consumer 双模块、Nacos 服务注册与发现、traceId 在 RPC 调用链中自动传播、统一异常处理、JSR-303 参数校验。

## 功能概述

`raf-framework-dubbo-starter` 提供：

- **分布式追踪**：Provider/Consumer 双向自动传播 traceId
- **异常统一处理**：`CustExceptionFilter` 将 Dubbo 异常转换为框架标准异常
- **参数校验**：`ValidFilter` 在 Provider 端自动触发 JSR-303 校验
- **泛化调用支持**：`CustGenericFilter` 支持泛化调用场景

## 模块结构

```
raf-example-dubbo-starter/
├── dubbo-api/          # 接口定义（共享，Facade 模块）
├── dubbo-provider/     # 服务提供者（端口 20880）
└── dubbo-consumer/     # 服务消费者（端口 8081）
```

## 快速接入

### 1. 引入依赖

```xml
<dependency>
    <groupId>io.github.jerryraf</groupId>
    <artifactId>raf-framework-dubbo-starter</artifactId>
</dependency>
```

### 2. 启动 Nacos

```bash
docker run -d --name nacos \
  -e MODE=standalone \
  -p 8848:8848 -p 9848:9848 \
  nacos/nacos-server:v2.3.0
```

### 3. Provider 配置（`application.yml`）

```yaml
spring:
  application:
    name: raf-example-dubbo-provider

dubbo:
  application:
    name: ${spring.application.name}
  registry:
    address: nacos://${NACOS_ADDR:127.0.0.1:8848}
    parameters:
      namespace: ${NACOS_NAMESPACE:}
  protocol:
    name: dubbo
    port: 20880
  scan:
    base-packages: io.github.jerryraf.examples.dubbo.provider.service

server:
  port: 8080
```

> 框架 Filter（`providerTrace`、`custException`、`custValid`）通过 `@Activate(group = PROVIDER)` 自动激活，无需手动配置 `dubbo.provider.filter`。

### 4. Consumer 配置（`application.yml`）

```yaml
spring:
  application:
    name: raf-example-dubbo-consumer

dubbo:
  application:
    name: ${spring.application.name}
  registry:
    address: nacos://${NACOS_ADDR:127.0.0.1:8848}
    parameters:
      namespace: ${NACOS_NAMESPACE:}
  consumer:
    check: false  # 启动时不检查 Provider 是否在线

server:
  port: 8081
```

> `consumerTrace` Filter 通过 `@Activate(group = CONSUMER)` 自动激活，无需手动配置 `dubbo.consumer.filter`。

## 配置项详解

### 框架自动注册的 Filter

| SPI 名称 | 类名 | 作用 | 适用端 |
|---|---|---|---|
| `providerTrace` | `ProviderTraceFilter` | Provider 端接收 traceId，写入 ContextHolder | Provider（自动激活） |
| `consumerTrace` | `ConsumerTraceFilter` | Consumer 端发送 traceId 到请求头 | Consumer（自动激活） |
| `custException` | `CustExceptionFilter` | 统一异常处理，转换为 RafResult | Provider（自动激活） |
| `custValid` | `ValidFilter` | JSR-303 参数校验 | Provider（自动激活） |
| `custGeneric` | `CustGenericFilter` | 泛化调用支持 | Provider（自动激活） |

所有 Filter 均通过 `@Activate` 注解自动激活，无需在 `dubbo.provider.filter` 或 `dubbo.consumer.filter` 中手动配置。

### Dubbo 核心配置

| 配置键 | 说明 |
|---|---|
| `dubbo.registry.address` | 注册中心地址，格式 `nacos://host:port` |
| `dubbo.registry.parameters.namespace` | Nacos 命名空间 ID |
| `dubbo.protocol.name` | 协议名称，默认 `dubbo` |
| `dubbo.protocol.port` | Provider 监听端口，默认 `20880` |
| `dubbo.scan.base-packages` | Provider 服务实现类扫描包路径 |
| `dubbo.provider.filter` | Provider 端 Filter 链 |
| `dubbo.consumer.filter` | Consumer 端 Filter 链 |

## 核心用法

### API 模块（接口定义）

```java
// dubbo-api 模块：只包含接口和 DTO，不依赖 Spring Boot
public interface UserFacade {

    /**
     * 根据 ID 查询用户
     */
    RafResult<UserDTO> getUserById(Long userId);

    /**
     * 创建用户
     */
    RafResult<UserDTO> createUser(UserCreateReq req);
}
```

```java
@Data
public class UserDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String email;
    private Integer age;
}
```

> Facade 模块的 `pom.xml` 必须禁用 `spring-boot-maven-plugin`，否则打包后无法被其他模块依赖。

### Provider 端（服务提供者）

```java
@DubboService(version = "1.0.0", group = "user")
@RequiredArgsConstructor
@Slf4j
public class UserFacadeImpl implements UserFacade {

    private final UserService userService;

    @Override
    public RafResult<UserDTO> getUserById(Long userId) {
        log.info("Dubbo Provider 收到查询请求，userId={}, traceId={}",
            userId, ContextHolder.getTraceId());

        UserDTO user = userService.getUserById(userId);
        return RafResult.success(user);
    }

    @Override
    public RafResult<UserDTO> createUser(UserCreateReq req) {
        // ValidFilter 已在框架层自动触发 JSR-303 校验
        // 此处无需手动调用 validate
        UserDTO user = userService.createUser(req);
        return RafResult.success(user);
    }
}
```

### Consumer 端（服务消费者）

```java
@RestController
@RequestMapping("/api/user")
@ResponseResult
@RequiredArgsConstructor
@Slf4j
public class UserController {

    @DubboReference(version = "1.0.0", group = "user")
    private UserFacade userFacade;

    @GetMapping("/{id}")
    public UserDTO getUser(@PathVariable Long id) {
        log.info("Consumer 发起 Dubbo 调用，userId={}, traceId={}",
            id, ContextHolder.getTraceId());

        RafResult<UserDTO> result = userFacade.getUserById(id);
        if (!result.isSuccess()) {
            throw new BusinessException(result.getCode(), result.getMsg());
        }
        return result.getData();
    }

    @PostMapping
    public UserDTO createUser(@RequestBody @Valid UserCreateReq req) {
        RafResult<UserDTO> result = userFacade.createUser(req);
        if (!result.isSuccess()) {
            throw new BusinessException(result.getCode(), result.getMsg());
        }
        return result.getData();
    }
}
```

### traceId 自动传播

框架自动在 Dubbo 调用链中传播 traceId，无需业务代码干预：

```
HTTP 请求 → Gateway（生成 traceId）
  → Consumer（ConsumerTraceFilter 写入 Dubbo 请求头）
    → Provider（ProviderTraceFilter 读取并写入 ContextHolder）
      → 日志自动包含 traceId
```

验证 traceId 传播：

```bash
# 调用 Consumer 接口
curl http://localhost:8081/api/user/1

# 查看 Provider 日志，应包含相同的 traceId
# [traceId=abc123] Dubbo Provider 收到查询请求，userId=1
```

### 异常处理

`CustExceptionFilter` 自动处理 Provider 端异常：

```java
// Provider 端抛出 BusinessException
throw new BusinessException(UserErrorCode.USER_NOT_FOUND);

// Consumer 端收到的是 RafResult（HTTP 200），需判断 isSuccess()
RafResult<UserDTO> result = userFacade.getUserById(id);
if (!result.isSuccess()) {
    // result.getCode() = 10001, result.getMsg() = "用户不存在"
    throw new BusinessException(result.getCode(), result.getMsg());
}
```

## 示例项目结构

```
raf-example-dubbo-starter/
├── dubbo-api/
│   └── src/main/java/io/github/jerryraf/examples/dubbo/api/
│       ├── facade/UserFacade.java          # 接口定义
│       └── dto/UserDTO.java                # 数据传输对象
├── dubbo-provider/
│   └── src/main/java/io/github/jerryraf/examples/dubbo/provider/
│       ├── DubboProviderApplication.java
│       ├── service/impl/UserFacadeImpl.java # @DubboService 实现
│       └── service/UserService.java
└── dubbo-consumer/
    └── src/main/java/io/github/jerryraf/examples/dubbo/consumer/
        ├── DubboConsumerApplication.java
        └── controller/UserController.java  # @DubboReference 调用
```

## 最佳实践

1. **接口版本管理**：使用 `version` 和 `group` 区分不同版本的服务，支持灰度发布
2. **序列化**：DTO 必须实现 `Serializable`，建议显式声明 `serialVersionUID`
3. **超时配置**：为每个 Dubbo 服务配置合理的超时时间，避免级联超时
4. **重试策略**：幂等接口可配置重试，非幂等接口（写操作）设置 `retries=0`
5. **Facade 模块**：禁用 `spring-boot-maven-plugin`，保持接口模块轻量
6. **traceId 一致性**：生产环境推荐接入 APM 平台（如 SkyWalking），确保全链路 traceId 一致

```yaml
# 超时和重试配置示例
dubbo:
  provider:
    timeout: 3000      # 默认超时 3 秒
    retries: 0         # 写操作不重试
  consumer:
    timeout: 5000      # Consumer 端超时 5 秒
    check: false       # 启动时不检查 Provider
```

## 常见问题

**Q: Dubbo 服务注册到 Nacos 后，Consumer 找不到 Provider？**

A: 检查 `dubbo.registry.address` 中的 namespace 是否与 Provider 一致，以及 `version` 和 `group` 是否匹配。

**Q: Provider 抛出 BusinessException，Consumer 收到的是什么？**

A: `CustExceptionFilter` 会将 `BusinessException` 转换为 `RafResult` 返回，Consumer 收到的是正常响应（HTTP 200），需要判断 `result.isSuccess()`。

**Q: Facade 模块的 pom.xml 需要特殊配置吗？**

A: Facade 模块（只包含接口定义）必须禁用 `spring-boot-maven-plugin`，否则打包后无法被其他模块依赖：

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <configuration>
                <skip>true</skip>
            </configuration>
        </plugin>
    </plugins>
</build>
```

**Q: Consumer 启动时报 `No provider available`？**

A: 设置 `dubbo.consumer.check=false`，允许 Consumer 在 Provider 未启动时正常启动。

**Q: 如何验证 traceId 在 RPC 链路中传播？**

A: 在 Provider 和 Consumer 的日志中搜索相同的 traceId，或调用 `/api/trace` 接口查看链路信息。
