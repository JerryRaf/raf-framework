# 分布式事务示例

演示 Seata AT 模式下，订单-库存-支付三服务的分布式事务和全局回滚。

## 模块结构

```
raf-example-distributed-tx/
├── tx-api/           # 接口契约
├── order-service/    # 事务发起方（@GlobalTransactional）
├── product-service/  # 库存服务（事务参与方）
└── payment-service/  # 支付服务（事务参与方）
```

## 核心场景

```
createOrder() — @GlobalTransactional
  ├─ 1. order-service:   INSERT orders (本地事务)
  ├─ 2. product-service: UPDATE stock - quantity (Dubbo RPC)
  └─ 3. payment-service: INSERT payments (Dubbo RPC)
       ↓ 任一步骤失败
       → Seata TC 协调全局回滚（undo_log 补偿）
```

## 快速运行

**前置条件：** Docker Compose

```bash
cd examples/raf-example-distributed-tx

# 一键启动 Seata + Nacos + 3 个 MySQL 实例
docker-compose up -d

# 等待服务就绪后启动各服务
mvn spring-boot:run -pl product-service &
mvn spring-boot:run -pl payment-service &
mvn spring-boot:run -pl order-service
```

## 测试场景

| 场景 | 触发方式 | 预期结果 |
|------|----------|----------|
| 正常下单 | `POST /orders` 库存充足 | 三张表均写入，全局提交 |
| 库存不足 | `POST /orders` 数量超库存 | 订单回滚，库存不变 |
| 支付服务异常 | 模拟 payment-service 抛异常 | 订单 + 库存均回滚 |

## 关键代码

```java
// order-service — 事务发起方
@GlobalTransactional(rollbackFor = Exception.class)
public OrderDTO createOrder(CreateOrderReq req) {
    // 1. 本地写订单
    orderMapper.insert(order);

    // 2. 扣减库存（Dubbo RPC，Seata AT 自动代理）
    productFacade.deductStock(req.getProductId(), req.getQuantity());

    // 3. 创建支付单（Dubbo RPC）
    paymentFacade.createPayment(order.getId(), req.getAmount());

    return buildDTO(order);
}
```

## Seata AT 模式原理

Seata AT 模式通过**数据源代理**自动生成 `undo_log`，无需业务代码感知：

1. **一阶段**：各服务执行本地 SQL，同时记录 `undo_log`（前镜像 + 后镜像）
2. **二阶段提交**：TC 通知各服务删除 `undo_log`
3. **二阶段回滚**：TC 通知各服务用 `undo_log` 反向补偿

## 关键配置

```yaml
seata:
  enabled: true
  application-id: order-service
  tx-service-group: raf-tx-group
  service:
    vgroup-mapping:
      raf-tx-group: default
  registry:
    type: nacos
    nacos:
      server-addr: localhost:8848
```

详见 [示例源码](https://github.com/JerryRaf/raf-framework/tree/master/examples/raf-example-distributed-tx)
