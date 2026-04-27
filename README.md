<div align="center">

# RAF Framework

**企业级 Spring Boot 3.x 微服务开发框架**

让业务开发者只关注业务本身

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.7-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2024.0.1-brightgreen.svg)](https://spring.io/projects/spring-cloud)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.jerryraf/raf-framework-dependencies)](https://central.sonatype.com/artifact/io.github.jerryraf/raf-framework-dependencies)
[![Build](https://github.com/JerryRaf/raf-framework/actions/workflows/ci.yml/badge.svg)](https://github.com/JerryRaf/raf-framework/actions/workflows/ci.yml)

[快速开始](#快速开始) · [文档](#核心能力) · [更新日志](CHANGELOG.md) · [提交 Issue](https://github.com/jerry-raf/raf-framework/issues) · [贡献指南](#贡献指南)

</div>

---

## 简介

RAF Framework 是基于 Spring Boot 3.x 生态构建的企业级微服务开发框架，面向中大型团队的标准化研发诉求。

框架不是业务代码，而是业务代码运行的**底座**——它封装了中间件接入、异常处理、分布式追踪、安全加密等横切关注点：

- **热插拔**：所有组件默认关闭，`raf.{component}.enabled=true` 显式启用，不引入 Starter 则零副作用
- **零硬编码**：地址、密钥、账号等环境配置全部由外部配置中心（Nacos）注入
- **统一标准**：响应格式、异常分层、日志规范、追踪传播、版本管理均有统一约定
- **生产就绪**：内置慢 SQL 告警、线程池 Prometheus 指标、优雅停机、分布式追踪、Sentry 错误上报
- **安全合规**：通过 OWASP / Fortify 安全扫描，网关层内置 ECIES 加密、ECDSA 签名、防重放机制

---

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8.8+
- Spring Boot 3.x 项目

### 第一步：引入 BOM

在项目的 `pom.xml` 中统一管理版本：

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

### 第二步：按需引入 Starter

```xml
<!-- Web 基础（必须） -->
<dependency>
    <groupId>io.github.jerryraf</groupId>
    <artifactId>raf-framework-web-starter</artifactId>
</dependency>

<!-- Redis + 分布式锁（按需） -->
<dependency>
    <groupId>io.github.jerryraf</groupId>
    <artifactId>raf-framework-redis-starter</artifactId>
</dependency>

<!-- MyBatis + Druid + 多数据源（按需） -->
<dependency>
    <groupId>io.github.jerryraf</groupId>
    <artifactId>raf-framework-mybatis-starter</artifactId>
</dependency>
```

### 第三步：启用组件

```yaml
# application.yml
raf:
  redis:
    enabled: true
    cluster:
      nodes: host1:6379,host2:6379
  executor:
    enabled: true
    corePoolSize: 10
  log:
    enabled: true
    level: REQ_BODY   # BASIC | REQ_BODY | RSP_BODY
```

启动后所有 REST 接口自动返回标准化响应：

```json
{ "code": 200, "msg": "成功！", "data": {} }
```

---

## 模块架构

```
raf-framework/
├── raf-framework-dependencies/            # BOM：统一管理依赖和插件版本
├── raf-framework-parent/                  # 框架父工程（flatten-maven-plugin）
├── raf-framework-core/                    # 核心库：所有自动配置实现
└── raf-framework-starter/                 # 按功能拆分的 Starter
    ├── raf-framework-starter-parent/      # 应用项目父工程
    ├── raf-framework-web-starter/   # Web 基础 + Jasypt 加密
    ├── raf-framework-gateway-starter/     # Spring Cloud Gateway 网关
    ├── raf-framework-dubbo-starter/       # Dubbo RPC
    ├── raf-framework-nacos-config-starter/    # Nacos 配置中心
    ├── raf-framework-nacos-discovery-starter/ # Nacos 服务发现
    ├── raf-framework-mybatis-starter/     # MyBatis + Druid + 多数据源
    ├── raf-framework-jdbc-starter/        # 纯 JDBC 场景
    ├── raf-framework-druid-starter/       # Druid 连接池
    ├── raf-framework-redis-starter/       # Redis + Redisson 分布式锁
    ├── raf-framework-mongodb-starter/     # MongoDB 多数据源
    ├── raf-framework-rabbit-starter/      # RabbitMQ
    ├── raf-framework-rocketmq-starter/    # RocketMQ（含事务消息）
    ├── raf-framework-kafka-starter/       # Kafka
    ├── raf-framework-okhttp-starter/      # OkHttp 第三方 API 调用
    ├── raf-framework-elasticsearch-starter/   # Elasticsearch
    ├── raf-framework-shardingsphere-starter/  # ShardingSphere 分库分表
    ├── raf-framework-monitor-starter/     # Prometheus 监控
    ├── raf-framework-sentry-starter/      # Sentry 错误追踪
    └── raf-framework-swagger-starter/     # Springdoc + Knife4j 文档
```

版本由 `raf-framework-dependencies` BOM 统一管理，使用 `${revision}` 占位符，通过 `flatten-maven-plugin` 在发布时展开。

---

## 核心能力

### 统一响应格式

所有 REST 接口自动包装为 `RafResult<T>`，无需每个接口手动处理：

```json
{ "code": 200, "msg": "成功！", "data": {} }
```

| 响应码 | 含义 |
|--------|------|
| `200`  | 成功 |
| `401`  | 未授权 |
| `403`  | 权限不足 |
| `404`  | 资源未找到 |
| `500`  | 系统错误 |
| `700`  | 参数校验失败 |
| `800`  | 第三方接口错误 |
| `10000+` | 业务自定义异常（应用层定义） |

### 异常分层体系

四层异常覆盖全链路错误场景，`GlobalExceptionConfig` 统一兜底处理：

| 异常类 | 适用场景 | HTTP 状态 | 日志级别 | 堆栈 |
|--------|----------|-----------|----------|------|
| `BusinessException` | 业务规则校验失败，code ≥ 10000 | 200 | WARN | 无（性能优化） |
| `InfrastructureException` | Redis 超时、Dubbo 调用失败、ES 不可用 | 500 | ERROR | 有 |
| `SystemException` | 未预期错误（NPE、类型转换等） | 500 | ERROR | 有 |
| `ProtocolException` | 签名失败、Token 无效、解密失败 | 401 | WARN | 无（性能优化） |

```java
// 使用示例
if (user == null) {
    throw new BusinessException(10001, "用户不存在");
}
if (!redisClient.ping()) {
    throw new InfrastructureException("Redis 连接失败");
}
```

### 分布式追踪

`ContextHolder` + `TransmittableThreadLocal` 自动在以下场景传播 `traceId`，无需业务代码干预：

- Dubbo RPC 调用（Provider / Consumer 双向）
- Spring Cloud Feign 调用
- 异步线程池（`raf-async-thread-pool`）
- RocketMQ / RabbitMQ / Kafka 消息消费

### 多数据源路由

```java
// 方法级数据源切换，底层动态代理透明路由
@DsSelector("cms-slave")
public User getUserById(Long id) {
    return userMapper.selectById(id);
}
```

### 异步线程池

托管线程池 `raf-async-thread-pool`，自动传播 `traceId`，集成 Prometheus 指标，支持 Dynamic TP 动态调参无需重启：

```yaml
raf:
  executor:
    enabled: true
    threadNamePrefix: biz-async-
    corePoolSize: 10
    maxPoolSize: 50
    queueCapacity: 1000
```

### 网关安全

`raf-framework-gateway-starter` 内置：

- **ECIES 非对称加密**：请求体加密传输
- **ECDSA 签名验证**：防篡改
- **防重放攻击**：时间窗口 + nonce 校验

---

## 组件一览

| 组件 | 启用配置 | 说明 |
|------|----------|------|
| 多数据源 | `raf.dataSource.{name}.enabled=true` | 主从分离，`@DsSelector` 路由 |
| 分页 | `raf.pagehelper.enabled=true` | PageHelper 集成 |
| Redis | `raf.redis.enabled=true` | Lettuce 客户端，集群 / 单机 |
| Redisson | `raf.redisson.enabled=true` | 分布式锁，支持集群 + SSL |
| 自定义缓存 | `raf.customCache.enabled=true` | 自定义 TTL 的缓存策略 |
| MongoDB | `raf.mongodb.enabled=true` | 支持 `raf.mongodb.dataSources.{name}` 多数据源 |
| RabbitMQ | `raf.rabbit.enabled=true` | Provider / Consumer 分离，延时队列（死信交换机） |
| RocketMQ | `raf.rocketmq.enabled=true` | NORMAL / FIFO / DELAY / TRANSACTION 四种消息类型 |
| Kafka | `raf.kafka.enabled=true` | 高吞吐量生产者 / 消费者 |
| OkHttp | `raf.okhttp.enabled=true` | 集中管理第三方 API 调用，可配置超时、重试 |
| 异步线程池 | `raf.executor.enabled=true` | 含追踪传播和 Prometheus 指标 |
| 请求日志 | `raf.log.enabled=true` | BASIC / REQ_BODY / RSP_BODY 三个级别 |
| 跨域 | `raf.cors.enabled=true` | 统一 CORS 配置 |
| Feign 重试 | `raf.feign-retry.enabled=true` | Feign 调用自动重试 |
| 雪花 ID | `raf.snow-flake.enabled=true` | 分布式唯一 ID 生成 |
| Sentry | `raf.sentry.enabled=true` | 错误上报到 Sentry |
| Elasticsearch | `raf.elasticsearch.enabled=true` | ES 客户端封装 |
| ShardingSphere | `raf.shardingsphere.enabled=true` | 分库分表 |

---

## 技术栈

| 分类 | 组件 | 版本 |
|------|------|------|
| 基础框架 | Spring Boot | 3.4.7 |
| 微服务 | Spring Cloud | 2024.0.1 |
| 微服务 | Spring Cloud Alibaba | 2022.0.0.2 |
| RPC | Dubbo | 3.3.4 |
| 注册 / 配置中心 | Nacos Client | 2.5.1 |
| 数据库 ORM | MyBatis / MyBatis-Plus | 3.5.19 / 3.5.11 |
| 连接池 | Druid | 1.2.24 |
| 缓存 | Redis (Lettuce) + Redisson | 3.34.1 |
| 消息队列 | RocketMQ | 5.3.2 |
| 消息队列 | Kafka | 3.9.0 |
| 消息队列 | RabbitMQ | Spring AMQP |
| 文档搜索 | Elasticsearch | 7.17.9 |
| 文档数据库 | MongoDB | 5.2.1 |
| 分库分表 | ShardingSphere | 5.5.1 |
| HTTP 客户端 | OkHttp | 4.12.0 |
| 加密 | Jasypt Boot | 3.0.5 |
| 错误追踪 | Sentry | 1.7.28 |
| 线程池管理 | Dynamic TP | 1.2.1-x |
| 监控 | Micrometer Prometheus | 1.14.5 |
| API 文档 | Springdoc + Knife4j | 2.5.0 / 4.5.0 |
| 工具库 | Hutool / Guava | 5.8.36 / 33.4.5 |
| 上下文传播 | TransmittableThreadLocal | 2.14.5 |

---

## 开发规范

### 新增组件

1. 创建 `*Properties`（`@ConfigurationProperties`）定义配置项
2. 创建 `*Config`（`@Configuration` + `@ConditionalOnProperty`）实现自动配置
3. 注册到 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
4. 创建对应 Starter 模块，依赖 `raf-framework-autoconfigure`

### 异常使用

```
业务规则不满足     → BusinessException（code ≥ 10000）
调用 Redis/DB/第三方失败 → InfrastructureException
未预期的系统错误    → SystemException
认证 / 签名 / Token 失败 → ProtocolException
```

### 日志规范

| 级别 | 使用场景 |
|------|----------|
| `DEBUG` | 仅开发环境，禁止上生产 |
| `INFO` | 关键业务节点 |
| `WARN` | 可预期的异常（BusinessException 对应级别） |
| `ERROR` | 生产环境目标为零 |

默认日志路径 `/data/logs`，Druid 慢 SQL 阈值 300ms 告警、500ms 记录完整 SQL。

### 敏感配置加密

使用 Jasypt 加密敏感配置，格式 `ENC(密文)`：

```yaml
spring:
  datasource:
    password: ENC(xxxxxxxxxxxxxxxx)

jasypt:
  encryptor:
    algorithm: PBEWITHHMACSHA512ANDAES_256
    password: ${JASYPT_PASSWORD}   # 通过环境变量或启动参数注入，禁止硬编码
```

---

## 贡献指南

欢迎通过以下方式参与贡献：

1. **Fork** 本仓库并创建你的分支（`git checkout -b feat/your-feature`）
2. 提交变更（`git commit -m 'feat: add some feature'`）
3. 推送到远程（`git push origin feat/your-feature`）
4. 发起 **Pull Request**

提交前请确保：

- 代码通过 `mvn clean install` 构建
- 新功能须提供对应单元测试
- 遵循框架已有的命名约定和代码风格
- 敏感信息（地址、密钥）不出现在代码中

如遇问题或有功能建议，请 [提交 Issue](https://github.com/jerry-raf/raf-framework/issues)。

---

## 更新日志

详见 [CHANGELOG.md](CHANGELOG.md)。

---

## License

本项目基于 [MIT License](LICENSE) 开源，使用前请阅读许可证条款。
