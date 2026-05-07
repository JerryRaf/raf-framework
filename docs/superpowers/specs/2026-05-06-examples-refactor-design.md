# Examples 重构设计文档

**日期**: 2026-05-06  
**作者**: Jerry  
**状态**: 已批准

---

## 背景

当前 `examples/` 目录存在以下问题：

1. 命名不统一：`raf-example-basic`、`raf-example-redis` 等名称与 starter 名称无直接对应关系
2. pom 结构不一致：`raf-example-mybatis` 使用独立版本管理，其余继承 `raf-framework-parent`
3. 包名混乱：`com.raf.example.basic`、`com.github.raf.examples.mybatis`、`io.github.jerryraf.examples.*` 三种风格并存
4. 存在两个复杂多服务示例（`raf-example-full-stack`、`raf-example-distributed-tx`）需要删除
5. 大量 starter 缺少对应 example

---

## 目标

- 每个有独立演示价值的 starter 对应一个 example
- 命名规范：`raf-example-{starter-name}`，与 starter 直接对应
- pom 结构统一：全部继承 `raf-framework-parent`
- 包名统一：`io.github.jerryraf.examples.{shortname}`
- 删除 `raf-example-full-stack` 和 `raf-example-distributed-tx`

---

## 最终目录结构

```
examples/
├── raf-example-web-starter/            # 原 raf-example-basic（重命名 + 包名修正）
├── raf-example-gateway-starter/        # 原 raf-example-gateway（重命名 + 包名修正）
├── raf-example-nacos-starter/          # 新建（合并 nacos-config + nacos-discovery）
├── raf-example-mybatis-starter/        # 原 raf-example-mybatis（重命名 + pom/包名统一）
├── raf-example-redis-starter/          # 原 raf-example-redis（重命名 + 包名修正）
├── raf-example-mongodb-starter/        # 新建
├── raf-example-rabbit-starter/         # 新建
├── raf-example-rocketmq-starter/       # 新建
├── raf-example-kafka-starter/          # 新建
├── raf-example-okhttp-starter/         # 新建
├── raf-example-elasticsearch-starter/  # 新建
├── raf-example-dubbo-starter/          # 原 raf-example-dubbo（重命名 + 包名修正，多模块）
│   ├── dubbo-api/
│   ├── dubbo-provider/
│   └── dubbo-consumer/
└── raf-example-openapi-starter/        # 新建
```

**删除**：`raf-example-full-stack`、`raf-example-distributed-tx`

---

## pom 统一规范

所有 example 统一继承 `raf-framework-parent`，不自带 `dependencyManagement`：

```xml
<parent>
    <groupId>io.github.jerryraf</groupId>
    <artifactId>raf-framework-parent</artifactId>
    <version>${revision}</version>
    <relativePath>../../raf-framework-parent</relativePath>
</parent>
<groupId>io.github.jerryraf.examples</groupId>
<artifactId>raf-example-{name}-starter</artifactId>
<version>1.0.0</version>
<packaging>jar</packaging>
```

多模块 example（dubbo）的子模块继承各自父 pom，父 pom 同样继承 `raf-framework-parent`。

---

## 包名规范

统一为 `io.github.jerryraf.examples.{shortname}`：

| example | 主包 |
|---|---|
| web-starter | `io.github.jerryraf.examples.web` |
| gateway-starter | `io.github.jerryraf.examples.gateway` |
| nacos-starter | `io.github.jerryraf.examples.nacos` |
| mybatis-starter | `io.github.jerryraf.examples.mybatis` |
| redis-starter | `io.github.jerryraf.examples.redis` |
| mongodb-starter | `io.github.jerryraf.examples.mongodb` |
| rabbit-starter | `io.github.jerryraf.examples.rabbit` |
| rocketmq-starter | `io.github.jerryraf.examples.rocketmq` |
| kafka-starter | `io.github.jerryraf.examples.kafka` |
| okhttp-starter | `io.github.jerryraf.examples.okhttp` |
| elasticsearch-starter | `io.github.jerryraf.examples.elasticsearch` |
| dubbo-starter | `io.github.jerryraf.examples.dubbo.{api/provider/consumer}` |
| openapi-starter | `io.github.jerryraf.examples.openapi` |

---

## 各 Example 详细说明

### 1. raf-example-web-starter

**对应 starter**: `raf-framework-web-starter`  
**来源**: 原 `raf-example-basic`（重命名，包名从 `com.raf.example.basic` 改为 `io.github.jerryraf.examples.web`）

**演示内容**:
- `RafResult<T>` 统一响应格式（code/msg/data/traceId）
- 四层异常体系：`BusinessException`、`InfrastructureException`、`SystemException`、`ProtocolException`
- `@Valid` 参数校验 → 自动返回 code=700
- traceId 自动注入每个响应
- 请求日志（`raf.log.level: REQ_BODY`）
- CORS 配置
- 配置注释：`raf.monitor`（Prometheus）和 `raf.sentry` 的配置示例

**核心文件**:
```
src/main/java/io/github/jerryraf/examples/web/
├── WebExampleApplication.java
├── controller/UserController.java
├── service/UserService.java
├── dto/UserCreateReq.java
├── dto/UserRes.java
└── common/UserErrorCode.java
src/main/resources/application.yml
```

---

### 2. raf-example-gateway-starter

**对应 starter**: `raf-framework-gateway-starter`  
**来源**: 原 `raf-example-gateway`（重命名，包名从 `io.github.jerryraf.examples.gateway` 保持不变）

**演示内容**:
- ECIES 非对称加密（请求体加密/解密）
- ECDSA 数字签名验证
- 防重放攻击（Redis nonce + 时间窗口）
- Spring Cloud Gateway 路由配置

**核心文件**:
```
src/main/java/io/github/jerryraf/examples/gateway/
├── GatewayExampleApplication.java
└── config/GatewayRouteConfig.java
src/main/resources/application.yml
src/test/java/.../GatewaySecurityIT.java
```

---

### 3. raf-example-nacos-starter

**对应 starter**: `raf-framework-nacos-config-starter` + `raf-framework-nacos-discovery-starter`  
**来源**: 新建

**演示内容**:
- Nacos 服务注册与发现
- Nacos 配置中心动态刷新（`@RefreshScope` + `@Value`）
- 配置加密（Jasypt）
- 多环境配置（dev/prod）

**核心文件**:
```
src/main/java/io/github/jerryraf/examples/nacos/
├── NacosExampleApplication.java
├── controller/ConfigController.java
└── config/DynamicConfig.java
src/main/resources/bootstrap.yml
```

---

### 4. raf-example-mybatis-starter

**对应 starter**: `raf-framework-mybatis-starter` + `raf-framework-datasource-starter`  
**来源**: 原 `raf-example-mybatis`（重命名，pom 结构统一，包名从 `com.github.raf.examples.mybatis` 改为 `io.github.jerryraf.examples.mybatis`）

**演示内容**:

**场景一：基础增删改查**
- MyBatis-Plus `BaseMapper`：`insert / updateById / deleteById / selectById`
- 条件查询：`LambdaQueryWrapper` 构建复杂查询条件
- 批量操作：`insertBatch / updateBatchById`
- 逻辑删除：`@TableLogic`，`deleteById` 实际执行 UPDATE is_deleted=1

**场景二：分页查询**
- MyBatis-Plus 内置分页插件（`MybatisPlusInterceptor`）
- `Page<T>` + `selectPage`：返回 `IPage<T>`（含 total/pages/records）
- 自定义 SQL 分页：Mapper XML 中使用 `Page` 参数

**场景三：事务管理**
- 单数据源事务：`@Transactional(rollbackFor = Exception.class)`
- 事务传播行为演示：`REQUIRED`（默认）vs `REQUIRES_NEW`（独立事务）
- 事务回滚验证：业务异常触发回滚，数据一致性保证
- 读写分离下的事务：写操作强制路由主库（`@DsSelector(DS_MASTER)` + `@Transactional`）

**多数据源配置**:
- Druid 连接池（master/slave 分别配置池参数）
- `@DsSelector` 注解读写分离路由
- dev profile 使用 H2 内存数据库，无需外部依赖

**核心文件**:
```
src/main/java/io/github/jerryraf/examples/mybatis/
├── MybatisExampleApplication.java
├── config/
│   ├── DataSourceConfig.java          # 多数据源 + DynamicRoutingDataSource
│   └── MyBatisConfig.java             # MybatisPlusInterceptor 分页插件
├── controller/UserController.java     # CRUD + 分页接口
├── service/UserService.java           # 业务逻辑 + 事务演示
├── dao/UserMapper.java                # BaseMapper + 自定义 SQL
├── entity/User.java                   # @TableLogic 逻辑删除
└── dto/
    ├── UserCreateReq.java
    ├── UserUpdateReq.java
    └── UserPageReq.java
src/main/resources/
├── bootstrap.yml
└── mapper/UserMapper.xml              # 自定义分页 SQL
```

---

### 5. raf-example-redis-starter

**对应 starter**: `raf-framework-redis-starter`  
**来源**: 原 `raf-example-redis`（重命名，包名从 `com.raf.example.redis` 改为 `io.github.jerryraf.examples.redis`，内容重写）

**演示内容**:

**场景一：常用操作**
- String：`set/get/setEx/incr/decr`
- Hash：`hSet/hGet/hGetAll/hDel`
- List：`lPush/rPush/lPop/lRange`
- Set：`sAdd/sMembers/sIsMember/sRem`
- ZSet（有序集合）：`zAdd/zRange/zRangeByScore/zRem`
- 过期时间：`expire/ttl/persist`
- 批量操作：`mSet/mGet`

**场景二：分布式锁**
- Redisson `tryLock`：等待超时 + 自动续期（WatchDog），防死锁
- 锁粒度：按业务 key 细粒度加锁（如 `lock:stock:{productId}`）
- 正确释放：`finally` 块中 `isHeldByCurrentThread()` 判断后再 `unlock()`
- 演示场景：库存扣减（高并发下防超卖）

**核心文件**:
```
src/main/java/io/github/jerryraf/examples/redis/
├── RedisExampleApplication.java
├── controller/
│   ├── RedisOpsController.java        # 常用操作演示接口
│   └── StockController.java           # 分布式锁演示接口
├── service/
│   ├── RedisOpsService.java           # String/Hash/List/Set/ZSet 操作示例
│   └── StockService.java              # 分布式锁 + 库存扣减
└── dto/StockDeductReq.java
src/main/resources/application.yml
```

---

### 6. raf-example-mongodb-starter

**对应 starter**: `raf-framework-mongodb-starter`  
**来源**: 新建

**演示内容**:
- 多数据源配置（primary/secondary）
- `MongoTemplate` CRUD
- 文档查询（条件查询、分页）
- 索引创建

**核心文件**:
```
src/main/java/io/github/jerryraf/examples/mongodb/
├── MongodbExampleApplication.java
├── controller/ArticleController.java
├── service/ArticleService.java
└── entity/Article.java
src/main/resources/application.yml
```

---

### 7. raf-example-rabbit-starter

**对应 starter**: `raf-framework-rabbit-starter`  
**来源**: 新建

**演示内容**:

**场景一：普通消息（订单通知）**
- 直连交换机（Direct Exchange）发送与消费
- 消费者手动 ACK（`AcknowledgeMode.MANUAL`）
- 消费异常处理：业务异常 → nack + requeue=false 进死信；基础设施异常 → nack + requeue=true 重试
- 幂等消费：Redis 记录已处理 messageId，重复投递直接 ACK 跳过

**场景二：延迟队列（订单超时取消）**
- 死信交换机模式（DLX + TTL）：消息在业务队列 TTL 到期后路由到死信队列触发处理
- 支持不同延迟时长（10min/30min/60min 对应不同 TTL 队列）
- 延迟消息发送 API + 消费者处理

**核心文件**:
```
src/main/java/io/github/jerryraf/examples/rabbit/
├── RabbitExampleApplication.java
├── config/
│   ├── RabbitNormalConfig.java        # 普通队列：交换机、队列、绑定声明
│   └── RabbitDelayConfig.java         # 延迟队列：DLX + TTL 队列声明
├── producer/
│   ├── OrderNotifyProducer.java       # 普通消息生产者
│   └── OrderDelayProducer.java        # 延迟消息生产者
├── consumer/
│   ├── OrderNotifyConsumer.java       # 普通消费者（手动ACK + 幂等）
│   └── OrderTimeoutConsumer.java      # 延迟队列消费者（超时取消处理）
├── service/IdempotentService.java     # 幂等校验（Redis messageId 去重）
├── controller/OrderController.java    # 触发发送的 REST 接口
└── dto/OrderMessage.java
src/main/resources/application.yml
```

---

### 8. raf-example-rocketmq-starter

**对应 starter**: `raf-framework-rocketmq-starter`  
**来源**: 新建

**演示内容**:
- NORMAL 普通消息
- DELAY 延时消息（18 个延时级别）
- TRANSACTION 事务消息（半消息 + 本地事务 + 回查）
- 消费者幂等（消息 ID 去重）

**核心文件**:
```
src/main/java/io/github/jerryraf/examples/rocketmq/
├── RocketMqExampleApplication.java
├── producer/OrderProducer.java
├── consumer/OrderConsumer.java
├── transaction/OrderTransactionListener.java
└── dto/OrderMessage.java
src/main/resources/application.yml
```

---

### 9. raf-example-kafka-starter

**对应 starter**: `raf-framework-kafka-starter`  
**来源**: 新建

**演示内容**:
- 高吞吐生产者配置（批量发送、压缩）
- 消费者组配置
- 手动提交 offset
- 消费者并发配置

**核心文件**:
```
src/main/java/io/github/jerryraf/examples/kafka/
├── KafkaExampleApplication.java
├── producer/EventProducer.java
├── consumer/EventConsumer.java
└── dto/UserEvent.java
src/main/resources/application.yml
```

---

### 10. raf-example-okhttp-starter

**对应 starter**: `raf-framework-okhttp-starter`  
**来源**: 新建

**演示内容**:
- OkHttp 客户端配置（超时、连接池）
- 第三方 API 调用封装
- 统一异常处理（`InfrastructureException`）
- 重试配置

**核心文件**:
```
src/main/java/io/github/jerryraf/examples/okhttp/
├── OkHttpExampleApplication.java
├── controller/WeatherController.java
├── service/WeatherService.java
└── dto/WeatherRes.java
src/main/resources/application.yml
```

---

### 11. raf-example-elasticsearch-starter

**对应 starter**: `raf-framework-elasticsearch-starter`  
**来源**: 新建

**演示内容**:
- 索引创建与映射
- 文档 CRUD（`ElasticsearchRestTemplate`）
- 全文检索（`match` 查询）
- 分页查询

**核心文件**:
```
src/main/java/io/github/jerryraf/examples/elasticsearch/
├── ElasticsearchExampleApplication.java
├── controller/ProductSearchController.java
├── service/ProductSearchService.java
└── entity/ProductDocument.java
src/main/resources/application.yml
```

---

### 12. raf-example-dubbo-starter

**对应 starter**: `raf-framework-dubbo-starter`  
**来源**: 原 `raf-example-dubbo`（重命名，包名保持 `io.github.jerryraf.examples.dubbo.*`）  
**结构**: 多模块（需要 provider + consumer 协作演示）

**演示内容**:
- Dubbo Provider 服务暴露（`@DubboService`）
- Dubbo Consumer 服务引用（`@DubboReference`）
- Nacos 注册中心
- traceId 在 RPC 调用链中的传播

**模块结构**:
```
raf-example-dubbo-starter/
├── pom.xml（父 pom，packaging=pom）
├── dubbo-api/          # 接口定义，packaging=jar，禁用 spring-boot-maven-plugin
├── dubbo-provider/     # 服务提供者
└── dubbo-consumer/     # 服务消费者（含 REST 接口触发 RPC 调用）
```

---

### 13. raf-example-openapi-starter

**对应 starter**: `raf-framework-openapi-starter`  
**来源**: 新建

**演示内容**:
- Knife4j 文档配置
- 接口分组（按模块分组）
- Bearer Token 鉴权配置
- 自定义文档信息（title/version/contact）

**核心文件**:
```
src/main/java/io/github/jerryraf/examples/openapi/
├── OpenApiExampleApplication.java
├── config/OpenApiConfig.java
├── controller/UserController.java
└── controller/ProductController.java
src/main/resources/application.yml
```

---

## 现有 example 变更对照表

| 原名称 | 新名称 | 变更类型 |
|---|---|---|
| `raf-example-basic` | `raf-example-web-starter` | 重命名 + 包名修正 |
| `raf-example-gateway` | `raf-example-gateway-starter` | 重命名（包名已正确）|
| `raf-example-mybatis` | `raf-example-mybatis-starter` | 重命名 + pom 统一 + 包名修正 |
| `raf-example-redis` | `raf-example-redis-starter` | 重命名 + 包名修正 |
| `raf-example-dubbo` | `raf-example-dubbo-starter` | 重命名（包名已正确）|
| `raf-example-full-stack` | —— | **删除** |
| `raf-example-distributed-tx` | —— | **删除** |

---

## 实施顺序

1. 删除 `raf-example-full-stack` 和 `raf-example-distributed-tx`
2. 重命名并修正现有 5 个 example（web/gateway/mybatis/redis/dubbo）
3. 新建 8 个 example（nacos/mongodb/rabbit/rocketmq/kafka/okhttp/elasticsearch/openapi）

---

## 约束

- 所有 example 不引入框架层未管理的依赖版本（数据库驱动除外）
- 新建 example 的 application.yml 中中间件地址全部使用环境变量占位符（如 `${REDIS_HOST:localhost}`）
- 不在 example 中硬编码密钥、密码等敏感信息
- 每个 example 包含必要的注释说明演示要点
