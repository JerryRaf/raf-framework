## 项目概述

raf-framework 是企业级微服务开发框架，封装中间件接入、异常处理、分布式追踪、安全加密等横切关注点，让业务开发者只需关注业务本身。

**核心设计原则：**
- Starter架构模式-组件化模式
- 高内聚,低耦合
- Maven 双接入模式（底层核心理念）：同时支持 parent 继承（`raf-framework-parent`）与 BOM 组合（`raf-framework-dependencies`）
- 热插拔组件，遵循"引入即使用"原则
- 大部分功能默认关闭，需要时显式启用（`@ConditionalOnProperty`）
- 框架层不包含硬编码值（地址、密钥等）
- 版本治理：`revision` 由根聚合 POM 统一管理，发布通过 flatten-maven-plugin 展开为固定版本
- 统一编码规范，符合安全扫描要求（OWASP、Fortify）

## 技术栈

| 分类         | 组件                           | 版本              |
|------------|------------------------------|-----------------|
| 基础框架       | Spring Boot                  | 3.4.7           |
| 微服务        | Spring Cloud                 | 2024.0.1        |
| 微服务        | Spring Cloud Alibaba         | 2022.0.0.2      |
| RPC        | Dubbo                        | 3.3.4           |
| 注册/配置中心    | Nacos Client                 | 2.5.1           |
| 数据库 ORM    | MyBatis / MyBatis-Plus       | 3.5.19 / 3.5.11 |
| 连接池        | Druid                        | 1.2.24          |
| 缓存         | Redis (Lettuce) + Redisson   | 3.34.1          |
| 消息队列       | RocketMQ                     | 5.3.2           |
| 消息队列       | Kafka                        | 3.9.0           |
| 消息队列       | RabbitMQ                     | Spring AMQP     |
| 文档搜索       | Elasticsearch                | 7.17.9          |
| 文档数据库      | MongoDB                      | 5.2.1           |
| 分库分表       | ShardingSphere               | 5.5.1           |
| HTTP 客户端   | OkHttp                       | 4.12.0          |
| 加密         | Jasypt Boot                  | 3.0.5           |
| 错误追踪       | Sentry                       | 7.20.0 (需升级) |
| 线程池管理      | Dynamic TP                   | 1.2.1-x         |
| 监控         | Micrometer Prometheus        | 1.14.5          |
| API 文档     | Springdoc + Knife4j          | 2.5.0 / 4.5.0   |
| 工具库        | Hutool / Guava               | 5.8.36 / 33.4.5 |
| 上下文传播      | TransmittableThreadLocal     | 2.14.5          |

## 关联配置资源

**Nacos 配置文件位置**: `D:\Framework\co-demo\01data\nacos-config\`

框架依赖外部 Nacos 配置中心，配置文件包括：
- `base.yml` - 共享基础配置（Nacos、日志、监控、中间件地址占位符）
- `co-cms-service.yml` - CMS 服务配置，包含 `raf.redis`、`raf.redisson`、`raf.rabbit`、`raf.executor` 等框架配置示例
- `co-tms-service.yml` - TMS 服务配置


## 环境要求

- JDK 17+
- Maven 3.8.8+

## 常用命令 (Commands)
当需要执行 Maven 构建时，请必须带上本地环境参数：
```bash
# 本地构建命令
mvn clean install -s "D:\Program Files\apache-maven-3.9.10\conf\settings-raf.xml" -Dmaven.repo.local="D:\data\repository\local"
```

## 模块架构

```
raf-framework/
├── raf-framework-dependencies/        # 统一依赖与插件版本管理（BOM）
├── raf-framework-parent/              # 框架父工程，flatten-maven-plugin 版本占位
├── raf-framework-core/                # 核心库，所有自动配置实现
├── raf-framework-starter/             # 按功能拆分的 Starter 集合
└── examples/                          # 示例项目（完整可运行）
    ├── raf-framework-starter-parent/          # 应用项目父工程（jar 不带版本号）
    ├── raf-framework-web-starter/       # Web 基础（含 Jasypt 加密）
    ├── raf-framework-gateway-starter/         # Spring Cloud Gateway 网关
    ├── raf-framework-dubbo-starter/           # Dubbo RPC
    ├── raf-framework-nacos-config-starter/    # Nacos 配置中心
    ├── raf-framework-nacos-discovery-starter/ # Nacos 服务发现
    ├── raf-framework-mybatis-starter/         # MyBatis + Druid + 多数据源
    ├── raf-framework-jdbc-starter/            # 纯 JDBC 场景
    ├── raf-framework-druid-starter/           # Druid 连接池独立使用
    ├── raf-framework-redis-starter/           # Redis + Redisson 分布式锁
    ├── raf-framework-mongodb-starter/         # MongoDB 多数据源
    ├── raf-framework-rabbit-starter/          # RabbitMQ
    ├── raf-framework-rocketmq-starter/        # RocketMQ（含事务消息）
    ├── raf-framework-kafka-starter/           # Kafka
    ├── raf-framework-okhttp-starter/          # OkHttp 第三方 API 调用
    ├── raf-framework-elasticsearch-starter/   # Elasticsearch
    ├── raf-framework-shardingsphere-starter/  # ShardingSphere 分库分表
    ├── raf-framework-monitor-starter/         # Prometheus 监控
    ├── raf-framework-sentry-starter/          # Sentry 错误追踪
    └── raf-framework-swagger-starter/         # Swagger / Knife4j 文档
```

## 示例项目

**位置**: `D:\Framework\raf-framework\examples\`

框架提供完整的生产级示例项目，演示各组件的最佳实践：

### raf-example-mybatis

**完整的 MyBatis-Plus 多数据源示例**，包含：
- ✅ 多数据源配置（Master-Slave）
- ✅ 读写分离（@DsSelector 注解）
- ✅ MyBatis-Plus 分页
- ✅ 事务管理
- ✅ 自定义 SQL 查询
- ✅ REST API 完整示例
- ✅ 详细文档（README.md + ARCHITECTURE.md）

**快速启动**：
```bash
cd D:\Framework\raf-framework\examples\raf-example-mybatis
mvn spring-boot:run -Dspring-boot.run.profiles=dev  # 使用 H2 内存数据库
```

**核心特性演示**：
```java
// 写操作 - 自动路由到主库
@DsSelector(DataSourceConfig.DS_MASTER)
@Transactional(rollbackFor = Exception.class)
public User createUser(User user) {
    userMapper.insert(user);
    return user;
}

// 读操作 - 自动路由到从库
@DsSelector(DataSourceConfig.DS_SLAVE)
public User getUserById(Long id) {
    return userMapper.selectById(id);
}

// 分页查询
@DsSelector(DataSourceConfig.DS_SLAVE)
public IPage<User> paginateUsers(int pageNum, int pageSize) {
    Page<User> page = new Page<>(pageNum, pageSize);
    return userMapper.selectPage(page, null);
}
```

**配置示例** (`bootstrap.yml`):
```yaml
spring.datasource:
  primary-master:
    url: jdbc:mysql://localhost:3306/example_db
    username: root
    password: root123
    initial-size: 10
    max-active: 50

  primary-slave:
    url: jdbc:mysql://localhost:3307/example_db
    username: root
    password: root123
    max-active: 100

mybatis-plus:
  global-config:
    db-config:
      id-type: AUTO
      logic-delete-field: isDeleted
```

**重要说明**：
1. **数据库驱动不在框架层** - 应用层按需引入 `mysql-connector-j`、`postgresql` 等
2. **mybatis-config.xml 可选** - 推荐使用 Spring Boot 配置，仅高级场景需要 XML
3. **PageHelper 已废弃** - 使用 MyBatis-Plus 内置分页插件

### 其他示例

- `raf-example-basic` - 基础 Web 应用示例
- `raf-example-redis` - Redis + Redisson 分布式锁示例

## 核心自动配置

自动配置注册在 `raf-framework-core/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`（Spring Boot 3 标准）。

**主要配置类：**
- `SpringContext`, `EnvConfig`, `JacksonConfig` - 基础设施
- `WebMvcConfig`, `GlobalExceptionConfig`, `ResponseResultConfig` - Web 层
- `DataSourceAutoConfig`, `MybatisConfig`, `PageConfig` - 数据库层
- `RedisConfig`, `RedissonConfig`, `MongodbConfig` - NoSQL
- `RabbitMqConfig`, `RocketMqConfig`, `KafkaConfig` - 消息队列
- `ThreadPoolConfig`, `ThreadPoolMetrics` - 异步线程池
- `FeignRetryConfig`, `OkHttpAutoConfig` - HTTP 客户端
- `ElasticsearchAutoConfiguration`, `ShardingSphereAutoConfiguration` - 扩展组件

所有配置使用条件注解（`@ConditionalOnProperty`、`@ConditionalOnClass`）支持热插拔。

## 异常处理体系

框架定义四层异常层次（`com.raf.framework.autoconfigure.common.exception`）：

1. **BusinessException（业务异常）**: code >= 10000，HTTP 200，WARN 日志，性能优化（无堆栈）
2. **InfrastructureException（基础设施异常）**: 第三方服务/数据库错误，HTTP 500，ERROR 日志
3. **SystemException（系统异常）**: 未预期错误，HTTP 500，ERROR 日志 + 完整堆栈
4. **ProtocolException（协议异常）**: 认证/授权失败，HTTP 401

**全局异常处理器** (`GlobalExceptionConfig`) 处理验证错误、HTTP 状态码错误、SQL 异常，返回标准化 `RafResult` 响应。

## 标准响应格式与关键配置前缀

所有 API 响应使用 `RafResult<T>`，格式为 `{"code": 200, "msg": "成功！", "data": {}}`。响应码：200 成功 | 401 未授权 | 403 权限不足 | 404 未找到 | 500 服务错误 | 700 参数错误 | 800 第三方接口错误 | 10000+ 业务异常（应用自定义）

所有组件默认关闭，通过 `raf.{component}.enabled=true` 启用：

- `raf.dataSource.{name}` - 多数据源（`@DsSelector` 路由），`raf.pagehelper` 分页
- `raf.redis` / `raf.redisson` - Redis + 分布式锁，`raf.customCache` 自定义缓存 TTL
- `raf.mongodb` - MongoDB，支持 `raf.mongodb.dataSources.{name}` 多数据源
- `raf.rabbit` - RabbitMQ，Provider/Consumer 分离，延时队列（死信交换机模式）
- `raf.rocketmq` - RocketMQ，支持 NORMAL/FIFO/DELAY/TRANSACTION 四种消息类型
- `raf.kafka` - Kafka，高吞吐量生产者/消费者
- `raf.okhttp` - OkHttp，集中第三方 API 调用，可配置超时、重试
- `raf.executor` - 异步线程池（`raf-async-thread-pool`），含追踪传播和 Prometheus 指标
- `raf.log` - HTTP 请求/响应日志（BASIC/REQ_BODY/RSP_BODY 级别）
- `raf.cors` / `raf.feign-retry` / `raf.snow-flake` / `raf.sentry` / `raf.elasticsearch` / `raf.shardingsphere`

**Sentry 配置说明：**

Sentry 是企业级错误追踪和性能监控平台，框架提供完整集成支持。

**基础配置：**
- `raf.sentry.enabled` - 启用 Sentry 错误追踪（默认 false）
- `raf.sentry.dsn` - Sentry 项目 DSN（必填，格式：https://key@sentry.io/project-id）
- `raf.sentry.environment` - 环境标识（默认从 spring.profiles.active 获取）
- `raf.sentry.release` - 发布版本（默认从 spring.application.name 获取）
- `raf.sentry.debug` - 是否在控制台打印 Sentry 日志（默认 false，调试用）

**TraceId 集成（重要）：**
- `raf.sentry.trace-id-source` - traceId 来源，支持两种模式：
  - `framework`（默认）：使用框架的 `ContextHolder.getTraceId()`，适用于未接入 APM 平台的场景
  - `apm`：使用 APM 平台的 traceId，自动检测并集成以下 APM 平台：
    - SkyWalking（MDC key: `tid`）
    - Zipkin（MDC key: `X-B3-TraceId`）
    - Jaeger（MDC key: `trace_id`）
    - Elastic APM（MDC key: `trace.id`）
    - Datadog（MDC key: `dd.trace_id`）
    - 通用 APM（MDC key: `traceId`）

**异常过滤策略：**
- `raf.sentry.exception-filter.enabled` - 启用异常过滤（默认 true）
- `raf.sentry.exception-filter.include-exceptions` - 需要上报的异常类型（全限定类名），默认：
  - `com.raf.framework.autoconfigure.common.exception.SystemException`
  - `com.raf.framework.autoconfigure.common.exception.InfrastructureException`
- `raf.sentry.exception-filter.exclude-exceptions` - 需要排除的异常类型（优先级高于 include）
- `raf.sentry.exception-filter.report-business-exception` - 是否上报 BusinessException（默认 false，业务异常通常不需要告警）

**性能监控：**
- `raf.sentry.enable-performance` - 启用性能追踪（默认 false）
- `raf.sentry.sample-rate` - 错误采样率（0.0-1.0，默认 1.0 即 100%）
- `raf.sentry.traces-sample-rate` - 性能追踪采样率（0.0-1.0，默认 0.1 即 10%）

**附加配置：**
- `raf.sentry.tags` - 附加标签（Map 类型），例如：`{team: backend, region: cn-north}`
- `raf.sentry.send-default-pii` - 是否发送个人身份信息（默认 false）

**配置示例：**
```yaml
raf:
  sentry:
    enabled: true
    dsn: https://your-key@sentry.io/project-id
    trace-id-source: apm  # 使用 APM 平台的 traceId
    exception-filter:
      enabled: true
      report-business-exception: false
    enable-performance: true
    sample-rate: 1.0
    traces-sample-rate: 0.1
    tags:
      team: backend
      region: cn-north
```

**最佳实践：**
1. 生产环境建议使用 `trace-id-source: apm`，确保 Sentry、日志、APM 平台的 traceId 一致
2. 性能监控建议使用较低的采样率（0.1-0.2），避免性能开销
3. BusinessException 默认不上报，因为业务异常通常是预期内的错误
4. 使用 `exception-filter.exclude-exceptions` 排除不需要告警的异常（如限流异常）

## 开发规范

**添加新组件时：** 创建 `*Properties` (`@ConfigurationProperties`) + `*Config` (`@Configuration` + 条件注解) → 注册到 `AutoConfiguration.imports`。

**禁止行为：** 不用 `main` 方法测试（改用单元测试）| 不在框架层硬编码地址密钥 | 敏感配置必须 Jasypt 加密（PBEWITHHMACSHA512ANDAES_256）

**异常使用：** `BusinessException`（业务，code >= 10000）/ `InfrastructureException`（基础设施）/ `SystemException`（系统）/ `ProtocolException`（认证）

**日志：** DEBUG 仅开发 | INFO 关键业务 | WARN 需减少 | ERROR 生产需消灭。默认路径 `/data/logs`，SQL 300ms/500ms 告警。

**分布式追踪：** 

框架提供完整的分布式追踪支持，通过 `ContextHolder` + `TransmittableThreadLocal` 在 Dubbo、Feign、异步线程、MQ 间自动传播 traceId。

**追踪集成方案：**
1. **框架自定义追踪**：使用 `ContextHolder.getTraceId()` 生成和管理 traceId，适用于未接入 APM 平台的场景
2. **APM 平台集成**：支持与主流 APM 平台无缝集成，包括：
   - SkyWalking（推荐，国内广泛使用）
   - Zipkin（轻量级，适合中小型项目）
   - Jaeger（CNCF 项目，云原生首选）
   - Elastic APM（与 ELK 栈深度集成）
   - Datadog（商业 APM，功能强大）

**TraceId 一致性保证：**
- 日志系统：通过 MDC 自动注入 traceId 到日志
- APM 平台：自动读取 APM 平台的 traceId（通过 MDC）
- Sentry 错误追踪：可配置使用框架或 APM 平台的 traceId（`raf.sentry.trace-id-source`）
- 消息队列：在消息头中传播 traceId
- RPC 调用：在 Dubbo/Feign 请求头中传播 traceId

**最佳实践：**
- 生产环境推荐接入 APM 平台（如 SkyWalking），并配置 Sentry 使用 `trace-id-source: apm`
- 确保所有组件（日志、APM、Sentry、MQ）使用统一的 traceId，便于问题排查
- 在异步线程池中使用 `RafContextTaskDecorator` 确保上下文传播

**命名约定 & 版本 & 部署：** GroupId `com.{company}.{dept}` | ArtifactId `{project}.{module}` | Package `com.{company}.{app}.{layer}`。框架 jar 带版本号，应用 jar 通过 `raf-framework-starter-parent` 排除版本后缀。推荐分层 jar + Docker，优雅停机 `server.shutdown: graceful`（30秒）。Facade 项目须禁用 `spring-boot-maven-plugin`。

**测试：** 位于 `src/test/java`，集成测试用 `@SpringBootTest`，模拟外部依赖（Redis、DB、MQ）。
