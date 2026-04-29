# raf-example-full-stack

完整电商微服务系统示例，演示 raf-framework 在真实业务场景中的综合应用。

## 功能演示

- 5 个微服务协同工作
- API 网关统一入口（ECIES 加密 + ECDSA 签名）
- 服务间 Feign 调用 + Nacos 服务发现
- Redis 缓存 + 分布式锁
- MySQL 多数据源
- 统一日志追踪（traceId 全链路传播）
- Prometheus 监控指标

## 系统架构

```
客户端
  │
  ▼
fs-gateway（8080）
  ├── 安全验证（签名/加密/防重放）
  ├── 路由到各微服务
  │
  ├──→ fs-user-service（8081）    用户注册/登录/信息管理
  ├──→ fs-product-service（8082） 商品管理/库存查询
  ├──→ fs-order-service（8083）   订单创建/查询（调用 user/product/payment）
  └──→ fs-payment-service（8084） 支付处理
```

## 模块结构

```
raf-example-full-stack/
├── fs-gateway/          # API 网关（Spring Cloud Gateway）
├── fs-user-service/     # 用户服务
├── fs-product-service/  # 商品服务
├── fs-order-service/    # 订单服务（核心，调用其他服务）
├── fs-payment-service/  # 支付服务
└── docs/
    └── postman/         # Postman 接口集合
```

## 环境要求

- JDK 17+
- Maven 3.8.8+
- MySQL 8.x
- Redis 6.x+
- Nacos 2.x

## 快速启动

**1. 启动基础设施**

```bash
docker run -d --name nacos -e MODE=standalone -p 8848:8848 nacos/nacos-server:v2.3.0
docker run -d --name redis -p 6379:6379 redis:7-alpine
docker run -d --name mysql -e MYSQL_ROOT_PASSWORD=root123 -p 3306:3306 mysql:8.0
```

**2. 初始化数据库**

```bash
mysql -u root -proot123 < docs/sql/init-all.sql
```

**3. 按顺序启动服务**

```bash
# 1. 基础服务先启动
cd fs-user-service && mvn spring-boot:run &
cd fs-product-service && mvn spring-boot:run &
cd fs-payment-service && mvn spring-boot:run &

# 2. 依赖其他服务的订单服务
cd fs-order-service && mvn spring-boot:run &

# 3. 最后启动网关
cd fs-gateway && mvn spring-boot:run
```

## API 接口列表

所有请求通过网关（`http://localhost:8080`）访问：

| 方法 | 路径 | 服务 | 说明 |
|---|---|---|---|
| POST | `/api/user/register` | user-service | 用户注册 |
| POST | `/api/user/login` | user-service | 用户登录 |
| GET | `/api/product/list` | product-service | 商品列表 |
| GET | `/api/product/{id}` | product-service | 商品详情 |
| POST | `/api/order` | order-service | 创建订单 |
| GET | `/api/order/{id}` | order-service | 查询订单 |
| POST | `/api/payment/{orderId}` | payment-service | 支付订单 |

## Postman 测试

导入 `docs/postman/raf-full-stack.postman_collection.json` 到 Postman，包含所有接口的测试用例。
