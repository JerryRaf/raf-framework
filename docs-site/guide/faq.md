# 常见问题（FAQ）

## 接入问题

### Q: 引入 Starter 后启动报错 `No qualifying bean of type`？

A: 检查以下几点：
1. 确认对应组件已通过 `raf.{component}.enabled=true` 启用
2. 确认配置文件中的必填项（如 Redis host、RocketMQ nameServer）已填写
3. 查看启动日志中的 `ConditionalOnProperty` 相关输出，确认条件是否满足

### Q: BOM 模式和 Parent 模式有什么区别，如何选择？

A: 两种模式功能等价，区别在于：
- **Parent 模式**（`raf-framework-parent`）：继承框架的构建配置（编译器版本、插件等），适合新项目
- **BOM 模式**（`raf-framework-dependencies`）：只导入依赖版本管理，不影响构建配置，适合已有父 POM 的项目

Maven 单继承限制下，如果项目已经继承了公司统一父 POM，只能使用 BOM 模式。

### Q: 如何确认某个 Starter 是否生效？

A: 启动时开启 `debug: true`，Spring Boot 会打印所有自动配置的条件评估报告（Conditions Evaluation Report），可以看到哪些配置类生效、哪些未生效及原因。

---

## 配置问题

### Q: 配置了 `raf.redis.enabled=true` 但 RedisService 注入失败？

A: 确认 `spring.data.redis.host` 等 Spring 原生 Redis 配置也已填写。框架的 `raf.redis` 是增强配置，底层仍依赖 Spring Data Redis 的连接配置。

### Q: 多数据源配置后，`@Transactional` 事务不生效？

A: 多数据源场景下，`@Transactional` 默认使用主数据源的事务管理器。如果需要指定数据源的事务，需要显式指定 `transactionManager`：

```java
@Transactional(transactionManager = "primaryMasterTransactionManager")
public void createOrder(Order order) { ... }
```

### Q: Jasypt 加密的配置如何生成密文？

A: 使用 Jasypt CLI 工具生成：

```bash
java -cp jasypt-1.9.3.jar org.jasypt.intf.cli.JasyptPBEStringEncryptionCLI \
  input="your_password" \
  password="your_jasypt_key" \
  algorithm="PBEWITHHMACSHA512ANDAES_256"
```

生成的密文填入配置文件：`password: ENC(密文)`。Jasypt key 通过环境变量注入，禁止硬编码。

---

## 版本问题

### Q: 升级框架版本后，某个 Starter 的配置项不生效了？

A: 查看 [版本升级指南](./upgrade.md) 了解 Breaking Changes。也可以通过 `debug: true` 查看条件评估报告，确认配置前缀是否有变化。

### Q: 框架版本和 Spring Boot 版本不兼容怎么办？

A: raf-framework 3.x 要求 Spring Boot 3.x（Jakarta EE 9+）。如果项目还在使用 Spring Boot 2.x，请使用 raf-framework 2.x 版本。

---

## 运行时问题

### Q: 日志中看不到 traceId？

A: 检查日志配置中是否包含 MDC 变量 `%X{traceId}`：

```xml
<!-- logback-spring.xml -->
<pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level [%X{traceId}] %logger{36} - %msg%n</pattern>
```

### Q: 异步线程池中 traceId 丢失？

A: 确认使用的是框架管理的线程池（`raf.async.enabled=true`），框架通过 `RafContextTaskDecorator` 自动传播 traceId。如果使用自定义线程池，需要手动包装：

```java
executor.setTaskDecorator(new RafContextTaskDecorator());
```

### Q: 生产环境 OOM，如何排查？

A: 常见原因：
1. 线程池队列无界（`queueCapacity` 设置过大），大量任务堆积
2. Redis 连接池泄漏（Response 未关闭）
3. 分页查询未限制页大小，一次查询数据量过大

建议开启 JVM 堆转储：`-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/data/logs/`
