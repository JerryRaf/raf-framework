# Dubbo RPC（dubbo-starter）

## 功能概述

- **分布式追踪**：Provider/Consumer 双向自动传播 traceId
- **异常统一处理**：CustExceptionFilter 将 Dubbo 异常转换为框架标准异常
- **泛化调用支持**：CustGenericFilter 支持泛化调用场景
- **参数校验**：ValidFilter 在 Provider 端自动触发 JSR-303 校验

## 配置项

Dubbo 本身的配置通过 `dubbo.*` 前缀配置，框架 Starter 自动注册以下 Filter：

| Filter | 作用 | 适用端 |
|---|---|---|
| `ProviderTraceFilter` | Provider 端接收 traceId，写入 ContextHolder | Provider |
| `ConsumerTraceFilter` | Consumer 端发送 traceId 到请求头 | Consumer |
| `CustExceptionFilter` | 统一异常处理，转换为 RafResult | Provider |
| `ValidFilter` | JSR-303 参数校验 | Provider |
| `CustGenericFilter` | 泛化调用支持 | Provider |

## 快速接入

**1. 引入依赖**

```xml
<dependency>
    <groupId>io.github.jerryraf</groupId>
    <artifactId>raf-framework-dubbo-starter</artifactId>
</dependency>
```

**2. 配置**（`application.yml`）

```yaml
dubbo:
  application:
    name: ${spring.application.name}
  registry:
    address: nacos://127.0.0.1:8848
    parameters:
      namespace: your-namespace
  protocol:
    name: dubbo
    port: 20880
  scan:
    base-packages: com.yourcompany.yourapp.service
  provider:
    filter: providerTraceFilter,custExceptionFilter,validFilter
  consumer:
    filter: consumerTraceFilter
```

## 核心用法

### Provider 端（服务提供者）

```java
// 接口定义（通常在 facade 模块）
public interface OrderFacade {
    RafResult<OrderDTO> getOrder(Long orderId);
}

// 实现类
@DubboService(version = "1.0.0", group = "order")
public class OrderFacadeImpl implements OrderFacade {

    @Override
    public RafResult<OrderDTO> getOrder(Long orderId) {
        OrderDTO order = orderService.getById(orderId);
        return RafResult.success(order);
    }
}
```

### Consumer 端（服务消费者）

```java
@RestController
@RequestMapping("/api/order")
public class OrderController {

    @DubboReference(version = "1.0.0", group = "order")
    private OrderFacade orderFacade;

    @GetMapping("/{id}")
    @ResponseResult
    public OrderDTO getOrder(@PathVariable Long id) {
        RafResult<OrderDTO> result = orderFacade.getOrder(id);
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

## 常见问题

**Q: Dubbo 服务注册到 Nacos 后，Consumer 找不到 Provider？**

A: 检查 `dubbo.registry.address` 中的 namespace 是否与 Provider 一致，以及 `version` 和 `group` 是否匹配。

**Q: Provider 抛出 BusinessException，Consumer 收到的是什么？**

A: `CustExceptionFilter` 会将 `BusinessException` 转换为 `RafResult` 返回，Consumer 收到的是正常响应（HTTP 200），需要判断 `result.isSuccess()`。

**Q: Facade 模块的 pom.xml 需要特殊配置吗？**

A: Facade 模块（只包含接口定义）必须禁用 `spring-boot-maven-plugin`，否则打包后无法被其他模块依赖。
