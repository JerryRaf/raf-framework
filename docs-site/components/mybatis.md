# MyBatis 多数据源（mybatis-starter）

## 功能概述

- **多数据源路由**：`@DsSelector` 注解方法级切换数据源，底层动态代理透明路由
- **MyBatis-Plus 集成**：分页插件、逻辑删除、自动填充
- **Druid 连接池**：慢 SQL 告警（300ms/500ms）、连接池监控
- **读写分离**：主库写、从库读，注解驱动

## 配置项

### 数据源路由（raf.datasource）

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `raf.datasource.enabled` | boolean | `false` | 是否启用动态数据源路由 |
| `raf.datasource.primary` | string | `master` | 默认数据源 Bean 名称 |
| `raf.datasource.datasources` | list | `[]` | 纳入路由的数据源 Bean 名称列表 |

### 分页

推荐使用 MyBatis-Plus 内置分页插件和 `Page<T>` / `IPage<T>`，不再使用 PageHelper。

## 快速接入

**1. 引入依赖**（应用层还需引入数据库驱动）

```xml
<dependency>
    <groupId>io.github.jerryraf</groupId>
    <artifactId>raf-framework-mybatis-starter</artifactId>
</dependency>

<!-- 数据库驱动（框架层不包含，应用层按需引入） -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
```

**2. 配置**（`application.yml`）

```yaml
raf:
  datasource:
    enabled: true
    primary: primary-master
    datasources:
      - primary-master
      - primary-slave

mybatis-plus:
  global-config:
    db-config:
      id-type: AUTO
      logic-delete-field: isDeleted
      logic-delete-value: 1
      logic-not-delete-value: 0
```

## 核心用法

### @DsSelector 数据源路由

```java
// 写操作 - 路由到主库
@DsSelector("primary-master")
@Transactional(rollbackFor = Exception.class)
public User createUser(User user) {
    userMapper.insert(user);
    return user;
}

// 读操作 - 路由到从库
@DsSelector("primary-slave")
public User getUserById(Long id) {
    return userMapper.selectById(id);
}
```

### MyBatis-Plus 分页

```java
@DsSelector("primary-slave")
public IPage<User> pageUsers(int pageNum, int pageSize, String keyword) {
    Page<User> page = new Page<>(pageNum, pageSize);
    LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
        .like(StringUtils.isNotBlank(keyword), User::getName, keyword)
        .orderByDesc(User::getCreateTime);
    return userMapper.selectPage(page, wrapper);
}
```

### 自定义 SQL（XML Mapper）

```java
// Mapper 接口
@DsSelector("primary-slave")
List<UserVO> selectUserWithRole(@Param("deptId") Long deptId);
```

```xml
<!-- UserMapper.xml -->
<select id="selectUserWithRole" resultType="UserVO">
    SELECT u.*, r.role_name
    FROM user u
    LEFT JOIN user_role ur ON u.id = ur.user_id
    LEFT JOIN role r ON ur.role_id = r.id
    WHERE u.dept_id = #{deptId}
    AND u.is_deleted = 0
</select>
```

## 常见问题

**Q: 数据库驱动为什么不在框架里？**

A: 框架层不包含数据库驱动，避免版本冲突。应用层按需引入 `mysql-connector-j`、`postgresql` 等。

**Q: `@DsSelector` 和 `@Transactional` 同时使用时数据源切换不生效？**

A: `@DsSelector` 必须在事务开启之前生效。确保 `@DsSelector` 注解在调用链的最外层，或者在事务方法外部的 Service 层使用。

**Q: 慢 SQL 日志在哪里看？**

A: Druid 慢 SQL 日志通过 SLF4J 输出，logger 名称为 `druid.sql.Statement`，级别为 WARN（超过 `slowSqlMillis`）或 ERROR（超过 `slowSqlMillis * 2`）。
