# ShardingSphere 分库分表（shardingsphere-starter）

## 功能概述

- **分库分表**：水平拆分，支持分片键路由
- **读写分离**：主库写、从库读，自动路由
- **数据加密**：字段级加密存储
- **影子库**：压测流量路由到影子库，不影响生产数据

## 配置项

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `raf.shardingsphere.enabled` | boolean | `false` | 是否启用 ShardingSphere |

ShardingSphere 的详细分片规则通过 `spring.shardingsphere.*` 配置，框架 Starter 负责自动装配。

## 快速接入

```xml
<dependency>
    <groupId>io.github.jerryraf</groupId>
    <artifactId>raf-framework-shardingsphere-starter</artifactId>
</dependency>
```

```yaml
raf:
  shardingsphere:
    enabled: true

spring:
  shardingsphere:
    datasource:
      names: ds0, ds1
      ds0:
        type: com.alibaba.druid.pool.DruidDataSource
        url: jdbc:mysql://localhost:3306/order_db_0
        username: root
        password: root123
      ds1:
        type: com.alibaba.druid.pool.DruidDataSource
        url: jdbc:mysql://localhost:3306/order_db_1
        username: root
        password: root123
    rules:
      sharding:
        tables:
          t_order:
            actual-data-nodes: ds$->{0..1}.t_order_$->{0..3}
            database-strategy:
              standard:
                sharding-column: user_id
                sharding-algorithm-name: database-inline
            table-strategy:
              standard:
                sharding-column: order_id
                sharding-algorithm-name: table-inline
        sharding-algorithms:
          database-inline:
            type: INLINE
            props:
              algorithm-expression: ds$->{user_id % 2}
          table-inline:
            type: INLINE
            props:
              algorithm-expression: t_order_$->{order_id % 4}
    props:
      sql-show: true  # 开发环境开启 SQL 日志
```

## 核心用法

### 分片键路由

```java
// 框架自动根据 user_id 路由到对应分库，根据 order_id 路由到对应分表
// 业务代码无需感知分片逻辑
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    @Select("SELECT * FROM t_order WHERE user_id = #{userId}")
    List<Order> selectByUserId(@Param("userId") Long userId);
}
```

### 读写分离配置

```yaml
spring:
  shardingsphere:
    rules:
      readwrite-splitting:
        data-sources:
          readwrite_ds:
            write-data-source-name: ds_primary
            read-data-source-names:
              - ds_replica_0
              - ds_replica_1
            load-balancer-name: round_robin
        load-balancers:
          round_robin:
            type: ROUND_ROBIN
```

## 常见问题

**Q: 跨分片查询性能很差？**

A: 跨分片查询会在所有分片上执行，结果在内存中合并。尽量在查询条件中包含分片键，避免全分片扫描。

**Q: 分布式事务如何处理？**

A: ShardingSphere 支持 XA 和 Seata AT 两种分布式事务模式。推荐使用 Seata AT 模式，配合 `raf-example-distributed-tx` 示例。

**Q: `sql-show: true` 生产环境要关闭吗？**

A: 是的，生产环境必须关闭，否则每条 SQL 都会打印日志，严重影响性能。
