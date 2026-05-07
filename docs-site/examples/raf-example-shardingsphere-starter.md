# raf-example-shardingsphere-starter

> 演示 `raf-framework-shardingsphere-starter` 的核心能力：水平分库分表（INLINE 算法）、跨分片分页查询、ShardingSphere 与 MyBatis-Plus 集成。

## 功能概述

`raf-framework-shardingsphere-starter` 提供：

- **水平分库**：按分片键将数据路由到不同数据库实例
- **水平分表**：按分片键将数据路由到同一库的不同表
- **读写分离**：配合 ShardingSphere 读写分离规则
- **MyBatis-Plus 集成**：与 `raf-framework-mybatis-starter` 无缝集成
- **与框架多数据源互斥**：启用 ShardingSphere 后自动禁用框架的 `DataSourceAutoConfig`

## 快速接入

### 1. 引入依赖

```xml
<dependency>
    <groupId>io.github.jerryraf</groupId>
    <artifactId>raf-framework-shardingsphere-starter</artifactId>
</dependency>
<dependency>
    <groupId>io.github.jerryraf</groupId>
    <artifactId>raf-framework-mybatis-starter</artifactId>
</dependency>
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
</dependency>
```

### 2. 数据库准备

```sql
-- 创建两个分库
CREATE DATABASE shard_db0;
CREATE DATABASE shard_db1;

-- 在每个分库中创建两张分表
USE shard_db0;
CREATE TABLE t_order_0 (
    order_id    BIGINT       NOT NULL COMMENT '订单ID（分表键）',
    user_id     BIGINT       NOT NULL COMMENT '用户ID（分库键）',
    status      VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    total_amount DECIMAL(10,2) NOT NULL,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE t_order_1 LIKE t_order_0;

USE shard_db1;
CREATE TABLE t_order_0 LIKE shard_db0.t_order_0;
CREATE TABLE t_order_1 LIKE shard_db0.t_order_0;
```

### 3. 配置（`application.yml`）

```yaml
raf:
  shardingsphere:
    enabled: true
    sql-show: ${SHARDING_SQL_SHOW:false}    # 生产环境默认关闭，开发时通过环境变量开启
    data-sources:
      ds0:
        driver-class-name: com.mysql.cj.jdbc.Driver
        url: jdbc:mysql://localhost:3306/shard_db0?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
        username: ${DB_USERNAME}   # 使用专用低权限账号，禁止使用 root
        password: ${DB_PASSWORD}
        initial-size: 5
        min-idle: 5
        max-active: 20
      ds1:
        driver-class-name: com.mysql.cj.jdbc.Driver
        url: jdbc:mysql://localhost:3306/shard_db1?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
        username: ${DB_USERNAME}   # 使用专用低权限账号，禁止使用 root
        password: ${DB_PASSWORD}
        initial-size: 5
        min-idle: 5
        max-active: 20
    rules: |
      - !SHARDING
        tables:
          t_order:
            actualDataNodes: ds${0..1}.t_order_${0..1}
            databaseStrategy:
              standard:
                shardingColumn: user_id
                shardingAlgorithmName: db_inline
            tableStrategy:
              standard:
                shardingColumn: order_id
                shardingAlgorithmName: tbl_inline
            keyGenerateStrategy:
              column: order_id
              keyGeneratorName: snowflake
        shardingAlgorithms:
          db_inline:
            type: INLINE
            props:
              algorithm-expression: ds${user_id % 2}
          tbl_inline:
            type: INLINE
            props:
              algorithm-expression: t_order_${order_id % 2}
        keyGenerators:
          snowflake:
            type: SNOWFLAKE
```

## 配置项详解

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `raf.shardingsphere.enabled` | boolean | `false` | 是否启用 ShardingSphere |
| `raf.shardingsphere.sql-show` | boolean | `false` | 是否打印路由后的真实 SQL |
| `raf.shardingsphere.data-sources.{name}.*` | — | — | 数据源配置（Druid） |
| `raf.shardingsphere.rules` | string | — | 分片规则（YAML 格式，ShardingSphere 原生语法） |
| `raf.shardingsphere.props` | map | — | ShardingSphere 属性配置 |

## 分片策略说明

### INLINE 算法（简单场景）

```yaml
shardingAlgorithms:
  db_inline:
    type: INLINE
    props:
      algorithm-expression: ds${user_id % 2}   # 按 user_id 取模路由到 ds0/ds1
  tbl_inline:
    type: INLINE
    props:
      algorithm-expression: t_order_${order_id % 2}  # 按 order_id 取模路由到 t_order_0/t_order_1
```

### HASH_MOD 算法（字符串 key）

```yaml
shardingAlgorithms:
  db_hash:
    type: HASH_MOD
    props:
      sharding-count: 2
```

### 范围分片（按时间分表）

```yaml
shardingAlgorithms:
  tbl_range:
    type: INTERVAL
    props:
      datetime-pattern: "yyyy-MM-dd HH:mm:ss"
      datetime-lower: "2024-01-01 00:00:00"
      datetime-upper: "2026-12-31 23:59:59"
      sharding-suffix-pattern: "yyyyMM"
      datetime-interval-amount: 1
      datetime-interval-unit: MONTHS
```

## 核心用法

### 实体类

```java
@Data
@TableName("t_order")
public class Order {

    @TableId(type = IdType.ASSIGN_ID)  // ShardingSphere 雪花算法生成 ID
    private Long orderId;

    private Long userId;       // 分库键

    private String status;

    private BigDecimal totalAmount;

    private LocalDateTime createTime;
}
```

### 写操作（自动路由）

```java
@Transactional(rollbackFor = Exception.class)
public Order createOrder(Order order) {
    order.setCreateTime(LocalDateTime.now());
    orderMapper.insert(order);
    // ShardingSphere 自动路由：
    // user_id=1 → ds1.t_order_1（若 order_id 为奇数）
    // user_id=2 → ds0.t_order_0（若 order_id 为偶数）
    log.info("路由到 ds{}.t_order_{}", order.getUserId() % 2, order.getOrderId() % 2);
    return order;
}
```

### 读操作（精确路由 vs 广播）

```java
// 精确路由：包含分片键，ShardingSphere 只查对应分片
Order order = orderMapper.selectById(orderId);  // 需要 orderId 作为分表键

// 广播查询：不包含分片键，ShardingSphere 查所有分片并合并
List<Order> orders = orderMapper.selectByUserId(userId);  // 包含分库键 user_id，只查对应分库的所有分表
```

### 分页查询（跨分片归并）

```java
Page<Order> page = new Page<>(1, 10);
LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
    .eq(Order::getStatus, "PENDING")
    .orderByDesc(Order::getCreateTime);
Page<Order> result = orderMapper.selectPage(page, wrapper);
// ShardingSphere 自动查所有分片，归并排序后返回
```

## 示例项目结构

```
raf-example-shardingsphere-starter/
├── src/main/java/io/github/jerryraf/examples/shardingsphere/
│   ├── ShardingSphereExampleApplication.java
│   ├── entity/
│   │   └── Order.java                  # 订单实体（分库键 user_id，分表键 order_id）
│   ├── mapper/
│   │   └── OrderMapper.java            # MyBatis-Plus Mapper
│   ├── service/
│   │   └── OrderService.java           # 业务逻辑
│   └── controller/
│       └── OrderController.java        # REST API
└── src/main/resources/
    └── application.yml                 # ShardingSphere 分片规则配置
```

## API 接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/orders` | 创建订单（自动路由到对应分库分表） |
| GET | `/api/orders/{orderId}` | 按订单ID查询（精确路由） |
| GET | `/api/orders/user/{userId}` | 按用户ID查询订单列表 |
| GET | `/api/orders/page` | 分页查询（跨分片归并） |

## 最佳实践

1. **分片键选择**：选择查询频率最高的字段作为分片键，避免全分片扫描
2. **分片数量**：分片数建议为 2 的幂次（2、4、8），便于后续扩容
3. **避免跨分片 JOIN**：ShardingSphere 支持跨分片 JOIN，但性能差，尽量在应用层聚合
4. **广播表**：字典表、配置表等小表配置为广播表，每个分片都有完整数据
5. **绑定表**：关联查询频繁的表（如 t_order 和 t_order_item）配置为绑定表，避免笛卡尔积
6. **sql-show**：开发环境开启，生产环境关闭，避免日志过多

## 广播表配置

```yaml
rules: |
  - !SHARDING
    tables:
      t_order:
        # ... 分片配置
    bindingTables:
      - t_order, t_order_item    # 绑定表，关联查询不产生笛卡尔积
  - !BROADCAST
    tables:
      - t_dict                   # 广播表，每个分片都有完整数据
      - t_config
```

## 常见问题

**Q: 启用 ShardingSphere 后框架的多数据源配置还能用吗？**

A: 不能。`ShardingSphereCondition` 会在启用 ShardingSphere 时自动禁用框架的 `DataSourceAutoConfig`，两者互斥。

**Q: 分页查询性能差怎么办？**

A: 跨分片分页需要从所有分片取数据再归并，深分页（大 offset）性能差。建议：① 加分片键过滤缩小范围；② 使用游标分页（基于 ID 的范围查询）代替 offset 分页。

**Q: 如何扩容（从 2 分片扩到 4 分片）？**

A: ShardingSphere 本身不提供自动迁移。需要：① 停写；② 使用 ShardingSphere Scaling 或自定义脚本迁移数据；③ 更新分片规则；④ 恢复写入。建议提前规划分片数量。

**Q: 事务支持吗？**

A: 支持本地事务（单分片）和分布式事务（跨分片，需配置 XA 或 Seata）。单分片事务直接用 `@Transactional`，跨分片事务需额外配置。
