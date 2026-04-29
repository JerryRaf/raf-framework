# 数据源路由（datasource-starter）

## 功能概述

`raf-framework-datasource-starter` 提供动态数据源路由能力，适用于应用已经通过 Spring Boot、Druid 或 Hikari 创建多个 `DataSource` Bean，需要通过 `@DsSelector` 在业务方法上切换数据源的场景。

- **动态路由**：`@DsSelector` 注解按方法切换当前数据源
- **事务兼容**：数据源选择应在事务开启前完成
- **低侵入**：框架不硬编码数据库驱动、地址、账号或连接池类型
- **热插拔**：默认关闭，显式配置 `raf.datasource.enabled=true` 后启用

## 配置项

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `raf.datasource.enabled` | boolean | `false` | 是否启用动态数据源路由 |
| `raf.datasource.primary` | string | `master` | 默认数据源 Bean 名称 |
| `raf.datasource.datasources` | list | `[]` | 纳入路由的数据源 Bean 名称列表 |

## 快速接入

```xml
<dependency>
    <groupId>io.github.jerryraf</groupId>
    <artifactId>raf-framework-datasource-starter</artifactId>
</dependency>
```

```yaml
spring:
  datasource:
    master:
      url: jdbc:mysql://localhost:3306/app
      username: root
      password: ENC(encrypted_password)
    slave:
      url: jdbc:mysql://localhost:3307/app
      username: readonly
      password: ENC(encrypted_password)

raf:
  datasource:
    enabled: true
    primary: master
    datasources:
      - master
      - slave
```

## 核心用法

```java
@DsSelector("master")
@Transactional(rollbackFor = Exception.class)
public void createOrder(Order order) {
    orderMapper.insert(order);
}

@DsSelector("slave")
public Order getOrder(Long id) {
    return orderMapper.selectById(id);
}
```

## 最佳实践

- 数据库驱动由应用层按需引入，例如 `mysql-connector-j`、`postgresql`。
- `@DsSelector` 放在事务边界外层，避免事务已经绑定连接后再切换数据源。
- 生产环境密码使用 Jasypt 或 KMS 加密，不在框架层硬编码。
- 慢 SQL、连接池参数和监控交给具体连接池 starter 或应用层配置。
