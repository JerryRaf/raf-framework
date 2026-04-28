# 全栈电商示例

完整的微服务电商系统，串联 Gateway、Dubbo、RocketMQ、Seata 分布式事务所有技术点。

## 架构

```
客户端
  ↓ HTTPS（ECIES 加密 + ECDSA 签名）
[fs-gateway :8080]          ← Spring Cloud Gateway + 安全过滤器
  ↓ Dubbo RPC（Nacos 服务发现）
[fs-order-service :8081]    ← @GlobalTransactional 事务发起方
  ├─→ [fs-user-service :8082]     Dubbo RPC（查用户信息）
  ├─→ [fs-product-service :8083]  Dubbo RPC（扣减库存）
  └─→ RocketMQ ──→ [fs-payment-service :8084]  异步支付通知
```

## 模块结构

```
raf-example-full-stack/
├── fs-gateway/          # API 网关（加密 + 签名 + 防重放 + 路由）
├── fs-user-service/     # 用户服务（Provider）
├── fs-product-service/  # 商品/库存服务（Provider）
├── fs-order-service/    # 订单服务（事务发起方 + Consumer）
├── fs-payment-service/  # 支付服务（RocketMQ Consumer）
└── docker-compose.yml   # 一键启动全部中间件
```

## 快速运行

**前置条件：** Docker Compose

```bash
cd examples/raf-example-full-stack

# 一键启动：Nacos + Seata + MySQL + Redis + RocketMQ
docker-compose up -d

# 按顺序启动各服务
mvn spring-boot:run -pl fs-user-service &
mvn spring-boot:run -pl fs-product-service &
mvn spring-boot:run -pl fs-payment-service &
mvn spring-boot:run -pl fs-order-service &
mvn spring-boot:run -pl fs-gateway
```

访问 `http://localhost:8080`

## 完整下单流程

使用 Postman Collection（`docs/postman/raf-full-stack.postman_collection.json`）：

| 步骤 | 接口 | 说明 |
|------|------|------|
| 1 | `POST /users/register` | 注册用户，获取 token |
| 2 | `GET /products/{id}` | 查询商品库存 |
| 3 | `POST /orders` | 创建订单（触发分布式事务） |
| 4 | `GET /orders/{id}` | 查询订单状态 |
| 5 | `GET /orders/{id}/payment` | 查询支付结果（异步 MQ） |

## 技术整合点

| 技术 | 应用场景 |
|------|----------|
| Spring Cloud Gateway | 统一入口，ECIES 解密 + ECDSA 验签 |
| Dubbo + Nacos | 服务间 RPC 调用，traceId 透传 |
| Seata AT | 订单-库存跨服务事务一致性 |
| RocketMQ | 支付结果异步通知，解耦支付服务 |
| Redis | Nonce 防重放、分布式锁 |
| MyBatis-Plus | 多数据源读写分离 |

## 可观测性

所有服务均输出结构化日志，包含 `traceId`，可通过 ELK / SkyWalking 端到端追踪一次请求的完整链路。

详见 [示例源码](https://github.com/JerryRaf/raf-framework/tree/master/examples/raf-example-full-stack)
