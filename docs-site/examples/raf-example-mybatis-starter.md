# raf-example-mybatis-starter

> 演示 `raf-framework-mybatis-starter` 的核心能力：MyBatis-Plus 多数据源、读写分离（@DsSelector）、分页查询、事务传播行为。

## 功能概述

`raf-framework-mybatis-starter` 提供：

- **多数据源**：通过 `raf.datasource.{name}` 配置多个数据源，`@DsSelector` 注解切换
- **读写分离**：主库写、从库读，AOP 自动路由
- **MyBatis-Plus 分页**：内置 `Page<T>` 分页，无需 PageHelper
- **Druid 连接池**：SQL 监控、慢查询告警
- **事务管理**：支持 REQUIRED / REQUIRES_NEW 等传播行为

## 快速接入

### 1. 引入依赖

```xml
<dependency>
    <groupId>io.github.jerryraf</groupId>
    <artifactId>raf-framework-mybatis-starter</artifactId>
</dependency>
<!-- 按需选择数据库驱动 -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
</dependency>
```

> 框架层不包含数据库驱动（数据库无关性设计），应用层按需引入。

### 2. 配置（`bootstrap.yml`）

```yaml
spring:
  application:
    name: raf-example-mybatis-starter
  # 数据源连接配置使用 Spring Boot 标准前缀
  datasource:
    primary-master:
      url: jdbc:mysql://${DB_HOST:localhost}:3306/example_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
      username: ${DB_USERNAME:root}
      password: ${DB_PASSWORD:}
      driver-class-name: com.mysql.cj.jdbc.Driver
    primary-slave:
      url: jdbc:mysql://${DB_SLAVE_HOST:localhost}:3306/example_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
      username: ${DB_USERNAME:root}
      password: ${DB_PASSWORD:}
      driver-class-name: com.mysql.cj.jdbc.Driver

raf:
  datasource:
    enabled: true
    primary: primary-master          # 默认主数据源 Bean 名称
    datasources:                     # 注册到动态路由的数据源 Bean 名称列表
      - primary-master
      - primary-slave

mybatis-plus:
  mapper-locations: classpath:mapper/*.xml
  global-config:
    db-config:
      logic-delete-field: isDeleted
      logic-delete-value: 1
      logic-not-delete-value: 0
```

### 3. 数据库初始化（`schema.sql`）

```sql
CREATE TABLE IF NOT EXISTS t_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(64)  NOT NULL COMMENT '用户名',
    email       VARCHAR(128) NOT NULL COMMENT '邮箱',
    age         INT          NOT NULL DEFAULT 0 COMMENT '年龄',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1-正常 0-禁用',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
```

## 配置项详解

### 多数据源（raf.datasource）

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `raf.datasource.enabled` | boolean | `false` | 是否启用动态数据源路由 |
| `raf.datasource.primary` | string | `master` | 默认主数据源的 Bean 名称 |
| `raf.datasource.datasources` | list | — | 注册到动态路由的数据源 Bean 名称列表 |

> 数据源的连接参数（url、username、password 等）通过 `spring.datasource.{name}.*` 配置，遵循 Spring Boot 标准。框架负责将这些数据源 Bean 组装为动态路由数据源。

### Druid 连接池推荐配置

```yaml
spring:
  datasource:
    primary-master:
      druid:
        initial-size: 5
        min-idle: 5
        max-active: 20
        max-wait: 60000
        time-between-eviction-runs-millis: 60000
        min-evictable-idle-time-millis: 300000
        validation-query: SELECT 1
        test-while-idle: true
        test-on-borrow: false
        test-on-return: false
        # 慢 SQL 告警
        filter:
          stat:
            enabled: true
            slow-sql-millis: 300
            log-slow-sql: true
```

## 核心用法

### 读写分离（@DsSelector）

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    // 写操作 - 路由到主库
    @DsSelector("primary-master")
    @Transactional(rollbackFor = Exception.class)
    public User createUser(UserCreateReq req) {
        User user = new User();
        user.setUsername(req.getUsername());
        user.setEmail(req.getEmail());
        user.setAge(req.getAge());
        userMapper.insert(user);
        return user;
    }

    // 读操作 - 路由到从库
    @DsSelector("primary-slave")
    public User getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
        }
        return user;
    }
}
```

### 分页查询

```java
@DsSelector("primary-slave")
public IPage<User> pageUsers(UserPageReq req) {
    Page<User> page = new Page<>(req.getPageNum(), req.getPageSize());

    LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
        .like(StringUtils.isNotBlank(req.getUsername()), User::getUsername, req.getUsername())
        .eq(req.getStatus() != null, User::getStatus, req.getStatus())
        .orderByDesc(User::getCreateTime);

    return userMapper.selectPage(page, wrapper);
}
```

响应格式：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "records": [...],
    "total": 100,
    "size": 10,
    "current": 1,
    "pages": 10
  }
}
```

### 事务传播行为

```java
// REQUIRED（默认）：加入当前事务，没有则新建
@DsSelector("primary-master")
@Transactional(rollbackFor = Exception.class)
public User createUserRequired(UserCreateReq req) {
    User user = buildUser(req);
    userMapper.insert(user);
    // 如果后续操作抛异常，整个事务回滚
    return user;
}

// REQUIRES_NEW：挂起当前事务，新建独立事务
@DsSelector("primary-master")
@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
public void saveOperationLog(String operation, Long userId) {
    // 此事务独立提交，不受外层事务影响
    // 适用于：审计日志、操作记录等不应因业务失败而丢失的数据
    operationLogMapper.insert(buildLog(operation, userId));
}
```

### XML Mapper 复杂查询

```xml
<!-- src/main/resources/mapper/UserMapper.xml -->
<mapper namespace="io.github.jerryraf.examples.mybatis.mapper.UserMapper">

    <select id="selectUserWithOrders" resultMap="UserWithOrdersMap">
        SELECT u.id, u.username, u.email,
               o.id AS order_id, o.amount, o.status AS order_status
        FROM t_user u
        LEFT JOIN t_order o ON o.user_id = u.id
        WHERE u.id = #{userId}
          AND u.status = 1
    </select>

    <resultMap id="UserWithOrdersMap" type="UserWithOrdersVO">
        <id property="id" column="id"/>
        <result property="username" column="username"/>
        <result property="email" column="email"/>
        <collection property="orders" ofType="OrderVO">
            <id property="orderId" column="order_id"/>
            <result property="amount" column="amount"/>
            <result property="status" column="order_status"/>
        </collection>
    </resultMap>
</mapper>
```

## 架构设计说明

### 数据库无关性

框架层不包含任何数据库驱动，应用层按需引入：

```
raf-framework-mybatis-starter（抽象层）
  ├── MyBatis-Plus 核心
  ├── 分页插件
  └── 通用配置（不含驱动）

应用层（按需选择）
  ├── mysql-connector-j（MySQL）
  ├── postgresql（PostgreSQL）
  ├── ojdbc（Oracle）
  └── h2（H2 内存数据库，测试用）
```

### 读写分离架构

```
写请求 → @DsSelector("primary-master") → 主库（写）
读请求 → @DsSelector("primary-slave")  → 从库（读）
                                         ↑
                                    主从同步（MySQL Binlog）
```

## 示例项目结构

```
raf-example-mybatis-starter/
├── src/main/java/io/github/jerryraf/examples/mybatis/
│   ├── MybatisExampleApplication.java
│   ├── config/
│   │   └── DataSourceConfig.java       # 数据源常量定义
│   ├── entity/
│   │   └── User.java                   # 实体类（@TableName）
│   ├── mapper/
│   │   └── UserMapper.java             # Mapper 接口（继承 BaseMapper）
│   ├── service/
│   │   └── UserService.java            # 业务层（读写分离、分页、事务）
│   ├── controller/
│   │   └── UserController.java         # REST API
│   └── dto/
│       ├── UserCreateReq.java
│       └── UserPageReq.java
└── src/main/resources/
    ├── bootstrap.yml
    ├── schema.sql                       # 建表 SQL
    ├── data.sql                         # 初始化数据
    └── mapper/
        └── UserMapper.xml               # 复杂查询 XML
```

## 最佳实践

1. **数据源命名**：主库用 `{name}-master`，从库用 `{name}-slave`，语义清晰
2. **@DsSelector 位置**：加在 Service 方法上，不要加在 Mapper 上，保持 Mapper 层无感知
3. **事务边界**：写操作必须加 `@Transactional`，读操作不加（避免占用事务连接）
4. **分页参数校验**：pageNum >= 1，pageSize 限制最大值（如 100），防止全表扫描
5. **慢 SQL 监控**：配置 Druid 慢 SQL 告警（300ms/500ms），及时发现性能问题
6. **索引设计**：查询条件字段建索引，避免全表扫描；联合索引遵循最左前缀原则

## 常见问题

**Q: `ClassNotFoundException: com.mysql.cj.jdbc.Driver`？**

A: 添加 MySQL 驱动依赖：

```xml
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
</dependency>
```

**Q: `@DsSelector` 不生效，数据源没有切换？**

A: 确保方法是 `public`，且通过 Spring 代理调用（不能在同一个类内部直接调用）。需要启用 AOP：

```java
@EnableAspectJAutoProxy(proxyTargetClass = true)
```

**Q: 分页查询 total 始终为 0？**

A: 检查 MyBatis-Plus 分页插件是否已注册。框架自动注册，但如果应用层自定义了 `MybatisPlusInterceptor`，需要手动添加 `PaginationInnerInterceptor`。

**Q: 主从延迟导致读到旧数据？**

A: 写操作后立即读取时，强制走主库：`@DsSelector("primary-master")`。或者在业务层引入短暂延迟，等待主从同步。

**Q: 多数据源事务如何处理？**

A: 跨数据源事务需要分布式事务支持（Seata）。单数据源内的事务由 Spring 管理，正常使用 `@Transactional` 即可。
