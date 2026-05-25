<div align="center">

# RAF Framework

**AI-Ready Enterprise Microservices Framework for Spring Boot 3.x**

企业级微服务开发框架 · 内置 Spring AI 集成 · 让业务开发者只关注业务本身

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2025.x-brightgreen.svg)](https://spring.io/projects/spring-cloud)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.jerryraf/raf-framework-dependencies)](https://central.sonatype.com/artifact/io.github.jerryraf/raf-framework-dependencies)
[![Build](https://github.com/JerryRaf/raf-framework/actions/workflows/ci.yml/badge.svg)](https://github.com/JerryRaf/raf-framework/actions/workflows/ci.yml)
[![Coverage](https://codecov.io/gh/JerryRaf/raf-framework/branch/master/graph/badge.svg)](https://codecov.io/gh/JerryRaf/raf-framework)
[![CodeQL](https://github.com/JerryRaf/raf-framework/actions/workflows/codeql.yml/badge.svg)](https://github.com/JerryRaf/raf-framework/actions/workflows/codeql.yml)
[![Security](https://github.com/JerryRaf/raf-framework/actions/workflows/dependency-check.yml/badge.svg)](https://github.com/JerryRaf/raf-framework/actions/workflows/dependency-check.yml)
[![Contributors](https://img.shields.io/github/contributors/JerryRaf/raf-framework)](https://github.com/JerryRaf/raf-framework/graphs/contributors)
[![文档](https://img.shields.io/badge/文档-GitHub%20Pages-blue?logo=github)](https://jerryraf.github.io/raf-framework/)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-1.0-green.svg)](https://spring.io/projects/spring-ai)
[![AI Ready](https://img.shields.io/badge/AI-Ready-blueviolet.svg)](#ai-集成spring-ai)

[快速开始](#快速开始) · [文档](#核心能力) · [更新日志](CHANGELOG.md) · [提交 Issue](https://github.com/jerry-raf/raf-framework/issues) · [贡献指南](#贡献指南)

</div>

---

> **raf-framework 不是另一个脚手架，而是一个"中间件接入层"。**
> 它解决的问题是：每个微服务项目都要重复写 Redis 配置、MQ 消费者、多数据源路由、全局异常处理……
> raf-framework 把这些横切关注点封装成热插拔 Starter，**引入依赖即生效，不引入零侵入**。

## 简介

RAF Framework 是基于 Spring Boot 3.x 生态构建的企业级微服务开发框架，面向中大型团队的标准化研发诉求。

框架不是业务代码，而是业务代码运行的**底座**——它封装了中间件接入、异常处理、分布式追踪、安全加密等横切关注点：

- **热插拔**：所有组件默认关闭，`raf.{component}.enabled=true` 显式启用，不引入 Starter 则零副作用
- **零硬编码**：地址、密钥、账号等环境配置全部由外部配置中心（Nacos）注入
- **统一标准**：响应格式、异常分层、日志规范、追踪传播、版本管理均有统一约定
- **双接入模式（核心理念）**：同时支持 parent 继承和 BOM 组合（Maven 单继承限制 + 企业灵活性需求）
- **生产就绪**：内置慢 SQL 告警、线程池 Prometheus 指标、优雅停机、分布式追踪、Sentry 错误上报
- **安全合规**：通过 OWASP / Fortify 安全扫描，网关层内置 ECIES 加密、ECDSA 签名、防重放机制

---

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8.8+
- Spring Boot 3.x 项目

### 依赖接入模式（核心理念）

RAF Framework 对外提供两种等价接入方式，使用者可按团队习惯选择：

#### 方式 A：Parent 继承（推荐给统一工程规范团队）

```xml
<parent>
    <groupId>io.github.jerryraf</groupId>
    <artifactId>raf-framework-parent</artifactId>
    <version>${revision}</version>
</parent>
```

#### 方式 B：BOM 组合（推荐给已有父工程团队）

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.github.jerryraf</groupId>
            <artifactId>raf-framework-dependencies</artifactId>
            <version>${revision}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 按需引入 Starter

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

## 架构概览

```
┌─────────────────────────────────────────────────────────────────────┐
│                           版本治理层                                  │
│                                                                     │
│   raf-framework-dependencies (BOM)   raf-framework-parent (POM)    │
│   统一管理所有三方依赖版本              继承 BOM，提供构建插件配置          │
│          ↓ import                           ↓ parent                │
└──────────────────────────┬──────────────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────────────┐
│                         接入层（Starter）                             │
│                                                                     │
│  Web    Gateway   Dubbo   Nacos    MyBatis   Redis   MongoDB        │
│  RabbitMQ  RocketMQ  Kafka  OkHttp  ES  ShardingSphere  Monitor    │
│  Sentry   OpenAPI   AI (Spring AI · Multi-Provider · Session)      │
│                                                                     │
│  所有 Starter 默认关闭，raf.{component}.enabled=true 显式启用          │
└──────────────────────────┬──────────────────────────────────────────┘
                           │ 依赖
┌──────────────────────────▼──────────────────────────────────────────┐
│                         核心层（Core）                                │
│                                                                     │
│  自动配置        异常处理体系          分布式追踪          Web 规范       │
│  AutoConfig    BusinessException    ContextHolder    RafResult     │
│  条件装配        InfraException       TraceId 传播     全局异常处理     │
│  @Conditional  SystemException      TTL 上下文        统一响应格式     │
└──────────────────────────┬──────────────────────────────────────────┘
                           │ 对接
┌──────────────────────────▼──────────────────────────────────────────┐
│                         中间件层（Infrastructure）                    │
│                                                                     │
│  缓存          数据库              消息队列              服务治理        │
│  Redis         MySQL / Druid      RocketMQ             Nacos        │
│  Redisson      MyBatis-Plus       Kafka                Dubbo        │
│                MongoDB            RabbitMQ             Feign        │
│                ShardingSphere                                       │
│                                                                     │
│  搜索           监控               安全                              │
│  Elasticsearch  Prometheus        Jasypt 加密                       │
│                 Sentry            ECIES / ECDSA（网关层）            │
└─────────────────────────────────────────────────────────────────────┘
```

**分层职责说明：**

| 层次 | 模块 | 职责 |
|------|------|------|
| 版本治理层 | `raf-framework-dependencies` / `raf-framework-parent` | 统一管理依赖版本，提供 BOM 和 Parent 双接入模式 |
| 接入层 | `raf-framework-starter/*` | 20+ 热插拔 Starter，引入即生效，不引入零副作用 |
| 核心层 | `raf-framework-core` | 自动配置、异常分层、分布式追踪、统一响应规范 |
| 中间件层 | 各类中间件 | Redis、MySQL、MQ、ES 等，由 Starter 统一接入 |

## 与同类方案对比

| 对比维度 | raf-framework | 纯 Spring Boot 脚手架 | JHipster |
|---|---|---|---|
| 接入方式 | BOM / Parent 双模式 | 复制粘贴 | 代码生成 |
| 中间件覆盖 | 18+ Starter | 按需手写 | 有限 |
| 侵入性 | 零侵入 | 高 | 高 |
| 升级成本 | 改一行版本号 | 逐文件修改 | 重新生成 |

---

## AI 集成（Spring AI）

raf-framework 内置 `raf-framework-ai-starter`，基于 [Spring AI 1.0](https://spring.io/projects/spring-ai) 封装，开箱即用地支持多 LLM 提供商接入、多轮会话管理、场景路由。

```xml
<dependency>
    <groupId>io.github.jerryraf</groupId>
    <artifactId>raf-framework-ai-starter</artifactId>
</dependency>
```

```yaml
raf:
  ai:
    enabled: true
    default-provider: openai
    routes:
      code: claude        # code 场景路由到 Claude
      summary: deepseek   # summary 场景路由到 DeepSeek
    session:
      max-history: 20
      ttl: 3600
    providers:
      openai:
        enabled: true
        api-key: ${OPENAI_API_KEY}
        model: gpt-4o
      claude:
        enabled: true
        api-key: ${ANTHROPIC_API_KEY}
        model: claude-opus-4-5
      deepseek:
        enabled: true
        api-key: ${DEEPSEEK_API_KEY}
        base-url: https://api.deepseek.com
        model: deepseek-chat
```

**核心能力：**

| 能力 | 说明 |
|------|------|
| 多 Provider | OpenAI · Anthropic Claude · DeepSeek，统一 API 调用 |
| 场景路由 | 按业务场景自动选择最合适的模型 |
| 会话管理 | 多轮对话历史，内存（Caffeine）或 Redis 双存储 |
| 流式输出 | `streamChat()` 支持 SSE 实时推送 |
| 分布式追踪 | AI 请求自动注入 traceId，与全链路追踪打通 |
| 统一响应 | AI 接口自动包装为 `RafResult<T>`，与业务接口规范一致 |

```java
@Autowired
private AiService aiService;

// 同步调用（默认 Provider）
String reply = aiService.chat("用一句话解释什么是分布式锁");

// 指定场景路由
String code = aiService.chat(AiRequest.builder()
    .scene("code")
    .prompt("用 Java 实现一个线程安全的单例模式")
    .build());

// 流式输出
Flux<String> stream = aiService.streamChat("请逐步分析这段 SQL 的性能问题：...");
```

---

## 模块架构

### POM 结构设计

RAF Framework 采用 **三层 POM 架构**，遵循 Spring Boot 设计模式：

```
raf-framework (root)
  ├── <revision>3.0.0</revision>          # 唯一版本定义
  ├── flatten-maven-plugin                # 替换 ${revision} → 3.0.0
  │
  ├─→ raf-framework-dependencies (BOM)
  │     ├── parent: raf-framework
  │     ├── <dependencyManagement>        # 统一依赖版本管理
  │     └── 使用 ${project.version}       # 避免 flatten 去除 dependencyManagement
  │
  └─→ raf-framework-parent (Parent POM)
        ├── parent: raf-framework-dependencies
        ├── <build> 配置                  # 插件、编译配置
        └── flatten-maven-plugin (ossrh)  # 去除 <parent> 块，自包含
```

**为什么三层？**

| 使用场景 | 接入方式 | 优势 |
|---------|---------|------|
| **公司有统一父 POM** | 只 import `raf-framework-dependencies` (BOM) | 绕过 Maven 单继承限制 |
| **新项目无约束** | 直接继承 `raf-framework-parent` | 获得完整构建配置 + 依赖管理 |

**版本管理原则**：

| POM | `<revision>` 定义 | 版本来源 |
|-----|------------------|----------|
| **raf-framework** (root) | ✅ `<revision>3.0.0</revision>` | 唯一定义处 |
| **raf-framework-dependencies** | ❌ 不定义 | 从 root 继承，使用 `${project.version}` |
| **raf-framework-parent** | ❌ 不定义 | 从 dependencies 继承 |

**flatten-maven-plugin 配置**：

| POM | 插件 | 模式 | 作用 |
|-----|------|------|------|
| **root** | ✅ | `resolveCiFriendliesOnly` | 替换 `${revision}` 为 `3.0.0` |
| **dependencies** | ❌ | - | 保留 `<dependencyManagement>` |
| **parent** | ✅ | `ossrh` | 去除 `<parent>` 块 |

> **参考**：Spring Boot 使用相同模式（`spring-boot-dependencies` + `spring-boot-starter-parent`）

### 功能模块

```
raf-framework/
├── raf-framework-dependencies/            # BOM：统一管理依赖和插件版本
├── raf-framework-parent/                  # 框架父工程（flatten-maven-plugin）
├── raf-framework-core/                    # 核心库：所有自动配置实现
└── raf-framework-starter/                 # 按功能拆分的 Starter
    ├── raf-framework-starter-parent/      # 应用项目父工程
    ├── raf-framework-web-starter/         # Web 基础 + Jasypt 加密
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
    └── raf-framework-openapi-starter/     # Springdoc + Knife4j 文档
```

### 构建与发布

```bash
# 构建所有模块
mvn clean install -DskipTests

# 发布到 Maven Central
mvn clean deploy -Prelease

# 本地仓库结构（install 后）
~/.m2/repository/io/github/jerryraf/
  ├── raf-framework/3.0.0/
  │   └── raf-framework-3.0.0.pom          # ${revision} → 3.0.0
  ├── raf-framework-dependencies/3.0.0/
  │   └── raf-framework-dependencies-3.0.0.pom  # 保留 <dependencyManagement>
  └── raf-framework-parent/3.0.0/
      └── raf-framework-parent-3.0.0.pom   # 无 <parent> 块
```

---

## 核心能力

### App API 设计规范

#### API 响应与错误处理

详见 [docs-site/components/api-response.md](docs-site/components/api-response.md)

**核心原则**：HTTP 状态码表达传输层语义；响应体 `code` 字段表达业务层语义。

**标准响应结构**：

```json
{
  "code": "0",
  "message": "success",
  "data": { "userId": 1024 },
  "traceId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d"
}
```

**业务码规范**：5 位纯数字 `XXYYY`
- `XX`（前 2 位）：服务 ID（统一分配）
- `YYY`（后 3 位）：错误流水号（建议与 HTTP 状态码对齐）

#### API 安全设计

详见 [docs-site/components/api-security.md](docs-site/components/api-security.md)

**安全通信协议**：

1. **请求加密**：所有 App 请求使用 AES-256-GCM 加密，外层使用 ECDSA 签名
2. **响应加密**：HTTP 200 响应体均为 AES 加密；协议握手失败返回明文 401
3. **防重放**：时间戳窗口 ±300 秒 + Nonce 唯一性校验（Redis TTL 10 分钟）
4. **密钥管理**：客户端 EC P-256 密钥对 + ECDH 会话密钥派生

**请求头规范**：

| 请求头 | 说明 |
|--------|------|
| `X-App-Key` | 客户端 EC 公钥（注册时下发） |
| `X-App-Ts` | 13 位 Unix 时间戳（毫秒） |
| `X-App-Nonce` | 32 位 UUID，一次性使用 |
| `X-App-Sign` | ECDSA 签名，覆盖 `clientKey+ts+nonce+content` |

### 统一响应格式

所有 REST 接口自动包装为 `RafResult<T>`，无需每个接口手动处理：

```json
{ "code": 200, "msg": "成功！", "data": {} }
```

| 响应码 | 含义 |
|--------|------|
| `0` | 成功 |
| `10401` | 未授权 |
| `10403` | 权限不足 |
| `10404` | 资源未找到 |
| `10500` | 系统错误 |
| `10700` | 参数校验失败 |
| `10800` | 第三方接口错误 |
| 应用自定义枚举 | 业务异常（应用层实现 `IResponseEnum`） |

### 异常分层体系

四层异常覆盖全链路错误场景，`GlobalExceptionConfig` 统一兜底处理：

| 异常类 | 适用场景 | HTTP 状态 | 日志级别 | 堆栈 |
|--------|----------|-----------|----------|------|
| `BusinessException` | 业务规则校验失败，使用应用层 `IResponseEnum` | 200 | WARN | 无（性能优化） |
| `InfrastructureException` | Redis 超时、Dubbo 调用失败、ES 不可用 | 500 | ERROR | 有 |
| `SystemException` | 未预期错误（NPE、类型转换等） | 500 | ERROR | 有 |
| `ProtocolException` | 签名失败、Token 无效、解密失败 | 401 | WARN | 无（性能优化） |

```java
// 使用示例
if (user == null) {
    throw new BusinessException(AppResponseEnum.USER_NOT_FOUND);
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
| 多数据源 | `raf.datasource.enabled=true` | 基于已存在 DataSource Bean 的 `@DsSelector` 动态路由 |
| 分页 | `mybatis-plus` 分页插件 | 使用 MyBatis-Plus 内置分页能力 |
| Redis | `raf.redis.enabled=true` | Lettuce 客户端，集群 / 单机 |
| Redisson | `raf.redisson.enabled=true` | 分布式锁，支持集群 + SSL |
| 自定义缓存 | `raf.redis.custom-cache` | 自定义 TTL 的缓存策略 |
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

完整依赖版本见 [docs-site/components/dependency-versions.md](docs-site/components/dependency-versions.md)。

---

## 开发规范

### 新增组件

1. 创建 `*Properties`（`@ConfigurationProperties`）定义配置项
2. 创建 `*Config`（`@Configuration` + `@ConditionalOnProperty`）实现自动配置
3. 注册到 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
4. 创建对应 Starter 模块，依赖 `raf-framework-core`

### 异常使用

```
业务规则不满足     → BusinessException（应用层 IResponseEnum）
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

## 遇到问题？

| 场景 | 去哪里 |
|---|---|
| 使用问题、配置疑惑 | [GitHub Discussions](https://github.com/JerryRaf/raf-framework/discussions) |
| 发现 Bug | [GitHub Issues（Bug Report）](https://github.com/JerryRaf/raf-framework/issues/new?template=bug_report.md) |
| 功能建议 | [GitHub Issues（Feature Request）](https://github.com/JerryRaf/raf-framework/issues/new?template=feature_request.md) |

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
