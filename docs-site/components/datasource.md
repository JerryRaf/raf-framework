# 数据源连接池（datasource-starter）

## 功能概述

`raf-framework-datasource-starter` 提供 Druid 连接池的独立封装，适用于不需要 MyBatis-Plus 但需要 Druid 连接池管理的场景。

- **Druid 连接池**：企业级连接池，内置慢 SQL 监控
- **多数据源支持**：通过 `raf.dataSource.{name}` 配置多个数据源
- **动态路由**：`@DsSelector` 注解切换数据源
- **连接池监控**：Druid 内置 Web 监控页面（可选）

## 配置项

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `raf.dataSource.{name}.enabled` | boolean | `false` | 是否启用该数据源 |
| `raf.dataSource.{name}.url` | string | — | JDBC URL |
| `raf.dataSource.{name}.username` | string | — | 用户名 |
| `raf.dataSource.{name}.password` | string | — | 密码（支持 Jasypt 加密） |
| `raf.dataSource.{name}.driverClassName` | string | — | 驱动类名 |
| `raf.dataSource.{name}.initialSize` | int | `5` | 初始连接数 |
| `raf.dataSource.{name}.minIdle` | int | `5` | 最小空闲连接数 |
| `raf.dataSource.{name}.maxActive` | int | `20` | 最大连接数 |
| `raf.dataSource.{name}.maxWait` | int | `60000` | 获取连接最大等待时间（毫秒） |
| `raf.dataSource.{name}.validationQuery` | string | `SELECT 1` | 连接有效性检测 SQL |
| `raf.dataSource.{name}.testWhileIdle` | boolean | `true` | 空闲时检测连接有效性 |
| `raf.dataSource.{name}.slowSqlMillis` | int | `300` | 慢 SQL 告警阈值（毫秒） |
| `raf.dataSource.{name}.filters` | string | `stat,wall` | Druid 过滤器（stat=监控, wall=防 SQL 注入） |

## 快速接入

```xml
<dependency>
    <groupId>io.github.jerryraf</groupId>
    <artifactId>raf-framework-datasource-starter</artifactId>
</dependency>
```

```yaml
raf:
  dataSource:
    main:
      enabled: true
      url: jdbc:mysql://localhost:3306/your_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
      username: root
      password: ENC(your_encrypted_password)
      initialSize: 10
      maxActive: 50
      slowSqlMillis: 300
      filters: stat,wall
```

## 核心用法

### 注入 DataSource

```java
@Autowired
@Qualifier("mainDataSource")
private DataSource mainDataSource;

// 或者直接使用 JdbcTemplate
@Autowired
private JdbcTemplate jdbcTemplate;
```

### @DsSelector 切换数据源

```java
@DsSelector("main")
public List<Map<String, Object>> queryData(String sql) {
    return jdbcTemplate.queryForList(sql);
}
```

## 常见问题

**Q: 多数据源时 Bean 名称是什么？**

A: 命名规则为 `{name}DataSource`，例如配置了 `raf.dataSource.main`，Bean 名称为 `mainDataSource`。

**Q: Druid 监控页面如何开启？**

A: 在 `filters` 中加入 `stat`，并在 Spring Boot 中配置 `StatViewServlet`：

```java
@Bean
public ServletRegistrationBean<StatViewServlet> druidStatView() {
    ServletRegistrationBean<StatViewServlet> bean = new ServletRegistrationBean<>(new StatViewServlet(), "/druid/*");
    bean.addInitParameter("loginUsername", "admin");
    bean.addInitParameter("loginPassword", "admin123");
    return bean;
}
```
