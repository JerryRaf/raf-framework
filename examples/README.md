# RAF Framework Examples

本目录包含 RAF Framework 的完整示例项目，帮助你快速上手各个功能模块。

## 示例列表

| 示例 | 说明 | 涉及模块 |
|------|------|---------|
| [raf-example-basic](./raf-example-basic) | 基础 Web 应用，统一响应、异常处理 | springmvc-starter |
| [raf-example-redis](./raf-example-redis) | Redis 缓存 + Redisson 分布式锁 | redis-starter |
| [raf-example-mybatis](./raf-example-mybatis) | **完整的 MyBatis-Plus 多数据源示例** | mybatis-starter, datasource-starter |
| [raf-example-mq](./raf-example-mq) | RocketMQ 幂等生产/消费 | rocketmq-starter |

**raf-example-mybatis 特别说明**：
- ✅ 完整的生产级示例代码
- ✅ 多数据源配置（Master-Slave）
- ✅ 读写分离（@DsSelector 注解）
- ✅ MyBatis-Plus 分页
- ✅ 事务管理
- ✅ 自定义 SQL 查询
- ✅ REST API 完整示例
- ✅ 支持 H2 内存数据库测试
- ✅ 详细文档（README.md + ARCHITECTURE.md）

## 快速运行

### 前置条件

- JDK 17+
- Maven 3.8.8+
- 对应中间件（Redis / MySQL / RocketMQ）

### 构建

```bash
cd examples
mvn clean package -DskipTests
```

### 运行单个示例

```bash
cd raf-example-basic
mvn spring-boot:run
```

## 示例说明

### raf-example-basic

演示 RAF Framework 最核心的 Web 能力：

- `RafResult<T>` 统一响应格式
- `BusinessException` / `InfrastructureException` 异常分层
- 自定义业务错误码 `IResponseEnum`
- 参数校验 `@Valid` 自动返回 `code=10700`
- 分布式追踪 `traceId` 自动注入响应

### raf-example-redis

演示 Redis 相关能力：

- `RedisService` 封装的常用操作（String / Hash / List / ZSet）
- 缓存穿透防护（缓存空值）
- 缓存雪崩防护（随机 TTL）
- Redisson 分布式锁（`tryLock` 防死锁）

### raf-example-mybatis

**完整的 MyBatis-Plus 多数据源示例**，演示企业级数据库访问最佳实践：

**核心功能**：
- MyBatis-Plus Lambda 查询
- `@DsSelector` 多数据源路由（主库写/从库读）
- `@Transactional` 声明式事务
- 分页查询 `Page<T>`
- 批量操作 `saveBatch`
- 自定义 SQL 查询
- 逻辑删除
- 乐观锁

**快速启动**（使用 H2 内存数据库）：
```bash
cd raf-example-mybatis
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**配置示例**：
```yaml
spring.datasource:
  primary-master:
    url: jdbc:mysql://localhost:3306/example_db
    initial-size: 10
    max-active: 50
  
  primary-slave:
    url: jdbc:mysql://localhost:3307/example_db
    max-active: 100

mybatis-plus:
  global-config:
    db-config:
      id-type: AUTO
      logic-delete-field: isDeleted
```

**重要说明**：
1. **数据库驱动不在框架层** - 应用层按需引入 `mysql-connector-j`
2. **mybatis-config.xml 可选** - 推荐使用 Spring Boot 配置
3. **PageHelper 已废弃** - 使用 MyBatis-Plus 内置分页

详细文档请查看：[raf-example-mybatis/README.md](./raf-example-mybatis/README.md)

### raf-example-mq

演示消息队列能力：

- RocketMQ 幂等生产者（业务唯一键去重）
- RocketMQ 幂等消费者（消费前检查）
- 消息体 JSON 序列化
- 消费失败重试机制
