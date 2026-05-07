# RAF Framework Examples

本目录包含 RAF Framework 的完整示例项目，每个示例对应一个独立可运行的 Spring Boot 应用。

## 示例列表

| 示例 | 说明 |
|------|------|
| [raf-example-web-starter](./raf-example-web-starter) | Web 基础：统一响应、异常处理、访问日志 |
| [raf-example-mybatis-starter](./raf-example-mybatis-starter) | MyBatis-Plus 多数据源 + 读写分离 |
| [raf-example-redis-starter](./raf-example-redis-starter) | Redis 缓存 + Redisson 分布式锁 |
| [raf-example-dubbo-starter](./raf-example-dubbo-starter) | Dubbo RPC（多模块：api / provider / consumer）|
| [raf-example-rabbit-starter](./raf-example-rabbit-starter) | RabbitMQ 消息队列 |
| [raf-example-rocketmq-starter](./raf-example-rocketmq-starter) | RocketMQ（含事务消息）|
| [raf-example-kafka-starter](./raf-example-kafka-starter) | Kafka 高吞吐量消息 |
| [raf-example-elasticsearch-starter](./raf-example-elasticsearch-starter) | Elasticsearch 全文检索 |
| [raf-example-mongodb-starter](./raf-example-mongodb-starter) | MongoDB 多数据源 |
| [raf-example-shardingsphere-starter](./raf-example-shardingsphere-starter) | ShardingSphere 分库分表 |
| [raf-example-gateway-starter](./raf-example-gateway-starter) | Spring Cloud Gateway 网关 |
| [raf-example-nacos-starter](./raf-example-nacos-starter) | Nacos 配置中心 & 服务发现 |
| [raf-example-okhttp-starter](./raf-example-okhttp-starter) | OkHttp 第三方 API 调用 |
| [raf-example-openapi-starter](./raf-example-openapi-starter) | OpenAPI 文档（Springdoc + Knife4j）|
| [raf-example-monitor-starter](./raf-example-monitor-starter) | Prometheus 监控指标 |
| [raf-example-sentry-starter](./raf-example-sentry-starter) | Sentry 错误追踪 |
| [raf-example-kms-starter](./raf-example-kms-starter) | 多云 KMS 密钥管理与字段加密 |

## 前置条件

- JDK 17+
- Maven 3.8.8+
- 对应中间件（Redis / MySQL / RocketMQ 等，各示例 README 有说明）

## 构建

```bash
cd examples
mvn clean package -DskipTests
```

## 运行单个示例

```bash
cd raf-example-web-starter
mvn spring-boot:run
```

## 详细文档

各示例的详细说明请查阅 `docs-site/examples/` 目录下对应的文档。
