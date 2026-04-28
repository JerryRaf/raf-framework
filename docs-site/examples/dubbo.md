# Dubbo RPC 示例

演示 RAF Framework 中 Dubbo Provider/Consumer 的标准用法，包含 traceId 透明传播和 BusinessException 跨服务传播。

## 模块结构

```
raf-example-dubbo/
├── dubbo-api/       # 接口契约（Facade 层）
│   └── UserFacade.java
├── dubbo-provider/  # 用户服务（Provider）
│   ├── UserFacadeImpl.java   # @DubboService
│   └── application.yml
└── dubbo-consumer/  # 订单服务（Consumer）
    ├── OrderController.java  # @DubboReference
    └── application.yml
```

## 快速运行

**前置条件：** Nacos 2.x（`localhost:8848`）

```bash
cd examples/raf-example-dubbo

# 启动 Provider
cd dubbo-provider
mvn spring-boot:run &

# 启动 Consumer
cd ../dubbo-consumer
mvn spring-boot:run
```

## 核心功能

| 功能 | 实现方式 |
|------|----------|
| Provider 注册 | `@DubboService` 注解 |
| Consumer 调用 | `@DubboReference` 注解 |
| 服务发现 | Nacos 注册中心 |
| traceId 传播 | RAF 框架自动注入 Dubbo Attachment |
| 异常传播 | `BusinessException` 跨服务透明传播 |

## 关键代码

**Provider 端：**

```java
@DubboService
public class UserFacadeImpl implements UserFacade {

    @Override
    public UserDTO getUser(Long userId) {
        // traceId 已由框架自动从 Dubbo Attachment 中恢复
        return userService.getById(userId);
    }
}
```

**Consumer 端：**

```java
@RestController
public class OrderController {

    @DubboReference
    private UserFacade userFacade;

    @GetMapping("/orders/{id}")
    public RafResult<OrderVO> getOrder(@PathVariable Long id) {
        UserDTO user = userFacade.getUser(order.getUserId());
        // traceId 自动透传到 Provider，日志可端到端追踪
        return RafResult.success(buildVO(order, user));
    }
}
```

## 关键配置

```yaml
# dubbo-provider/application.yml
dubbo:
  application:
    name: dubbo-provider
  registry:
    address: nacos://localhost:8848
  protocol:
    name: dubbo
    port: 20880
```

## 测试验证

```bash
# 无需真实 Dubbo 连接，MockBean 集成测试
mvn test
```

详见 [示例源码](https://github.com/JerryRaf/raf-framework/tree/master/examples/raf-example-dubbo)
