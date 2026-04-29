# raf-example-distributed-tx

Seata AT 模式分布式事务示例，演示跨服务事务一致性保证。

## 功能演示

- Seata AT 模式分布式事务
- 三服务协调（订单、支付、库存）
- 事务回滚场景演示
- 分布式追踪（traceId 跨服务传播）

## 架构说明

```
用户下单请求
    │
    ▼
order-service（事务发起方）
    ├── 创建订单记录
    ├── 调用 payment-service 扣款
    └── 调用 product-service 扣库存

任意步骤失败 → Seata 协调全局回滚
```

## 模块结构

```
raf-example-distributed-tx/
├── order-service/     # 订单服务（端口 8082，事务发起方）
├── payment-service/   # 支付服务（端口 8083）
└── product-service/   # 库存服务（端口 8084）
```

## 环境要求

- JDK 17+
- Maven 3.8.8+
- MySQL 8.x
- Nacos 2.x
- Seata Server 2.x

## 快速启动

**1. 启动基础设施**

```bash
# Nacos
docker run -d --name nacos -e MODE=standalone -p 8848:8848 nacos/nacos-server:v2.3.0

# MySQL（需要创建三个数据库：order_db、payment_db、product_db）
docker run -d --name mysql -e MYSQL_ROOT_PASSWORD=root123 -p 3306:3306 mysql:8.0

# Seata Server
docker run -d --name seata-server -p 8091:8091 -p 7091:7091 seataio/seata-server:2.0.0
```

**2. 初始化数据库**

```bash
# 执行各服务的 SQL 初始化脚本
mysql -u root -proot123 < order-service/src/main/resources/sql/init.sql
mysql -u root -proot123 < payment-service/src/main/resources/sql/init.sql
mysql -u root -proot123 < product-service/src/main/resources/sql/init.sql
```

**3. 按顺序启动服务**

```bash
# 先启动 payment-service 和 product-service
cd examples/raf-example-distributed-tx/payment-service && mvn spring-boot:run &
cd examples/raf-example-distributed-tx/product-service && mvn spring-boot:run &

# 再启动 order-service
cd examples/raf-example-distributed-tx/order-service && mvn spring-boot:run
```

## API 接口列表

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/order` | 创建订单（触发分布式事务） |
| POST | `/api/order/fail` | 创建订单并模拟失败（验证回滚） |
| GET | `/api/order/{id}` | 查询订单状态 |

### 测试事务回滚

```bash
# 正常下单（成功）
curl -X POST http://localhost:8082/api/order \
  -H "Content-Type: application/json" \
  -d '{"userId": 1, "productId": 1, "quantity": 1, "amount": 99.00}'

# 模拟失败（验证三个服务全部回滚）
curl -X POST http://localhost:8082/api/order/fail \
  -H "Content-Type: application/json" \
  -d '{"userId": 1, "productId": 1, "quantity": 1, "amount": 99.00}'
```
