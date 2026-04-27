# RAF Framework MyBatis Starter 架构设计文档

## 1. MySQL 驱动依赖设计原则

### 为什么不将 MySQL 驱动添加到 mybatis-starter？

**核心原则：数据库无关性（Database Agnostic）**

```
企业级框架分层设计：

┌─────────────────────────────────────────┐
│  raf-framework-mybatis-starter          │
│  (抽象层 - 数据库无关)                    │
│  - MyBatis-Plus 核心                     │
│  - 分页插件                              │
│  - 通用配置                              │
│  ❌ 不包含任何数据库驱动                  │
└─────────────────────────────────────────┘
              ↓ 依赖
┌─────────────────────────────────────────┐
│  应用层 (co-xxx-service)                 │
│  根据实际需求选择：                       │
│  ✅ mysql-connector-j (MySQL)           │
│  ✅ postgresql (PostgreSQL)             │
│  ✅ ojdbc (Oracle)                      │
│  ✅ mssql-jdbc (SQL Server)             │
│  ✅ h2 (H2 Database)                    │
└─────────────────────────────────────────┘
```

### 设计优势

1. **灵活性**：支持任意数据库，不强制绑定
2. **轻量化**：框架不携带不必要的依赖
3. **可扩展**：新数据库支持无需修改框架
4. **最佳实践**：符合 Spring Boot Starter 设计规范

### 正确的依赖管理方式

**根 POM 统一管理版本**：

```xml
<properties>
    <mysql-connector.version>9.1.0</mysql-connector.version>
</properties>

<dependencyManagement>
    <dependencies>
        <!-- MySQL Driver (optional, choose based on your database) -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <version>${mysql-connector.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

**应用层按需引入**：

```xml
<dependencies>
    <!-- RAF Framework Starters -->
    <dependency>
        <groupId>io.github.jerryraf</groupId>
        <artifactId>raf-framework-mybatis-starter</artifactId>
    </dependency>

    <!-- Choose your database driver -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
    </dependency>
</dependencies>
```

---

## 2. mybatis-config.xml 配置策略

### 配置文件是否必需？

**答案：可选，推荐使用 Spring Boot 配置**

### 配置方式对比

#### 方式 1：使用 bootstrap.yml（推荐）

```yaml
mybatis:
  mapper-locations: classpath*:mapper/**/*.xml
  type-aliases-package: com.company.entity

  configuration:
    map-underscore-to-camel-case: true
    lazy-loading-enabled: true
    aggressive-lazy-loading: false
    use-generated-keys: true
    jdbc-type-for-null: NULL

mybatis-plus:
  global-config:
    db-config:
      id-type: AUTO
      logic-delete-field: isDeleted
      logic-delete-value: 1
      logic-not-delete-value: 0
```

**优势**：
- ✅ 统一配置管理
- ✅ 支持 Spring Boot 配置刷新
- ✅ 易于环境切换
- ✅ 无需额外 XML 文件

#### 方式 2：使用 mybatis-config.xml（高级场景）

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE configuration PUBLIC
        "-//mybatis.org//DTD Config 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration>
    <settings>
        <setting name="mapUnderscoreToCamelCase" value="true"/>
        <setting name="lazyLoadingEnabled" value="true"/>
        <setting name="useGeneratedKeys" value="true"/>
        <setting name="jdbcTypeForNull" value="NULL"/>
    </settings>

    <!-- 高级配置：自定义 TypeHandler -->
    <typeHandlers>
        <typeHandler handler="com.company.handler.CustomTypeHandler"/>
    </typeHandlers>
</configuration>
```

**适用场景**：
- 需要自定义 TypeHandler
- 需要自定义 ObjectFactory
- 需要自定义 Plugin（非 MyBatis-Plus 插件）

### 优化后的配置加载逻辑

```java
@Value("${mybatis.config-file:}")  // 默认为空，可选
private String myBatisConfigPath;

public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
    SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
    bean.setDataSource(dataSource);

    // MyBatis config file is optional
    if (myBatisConfigPath != null && !myBatisConfigPath.isEmpty()) {
        ClassPathResource configResource = new ClassPathResource(myBatisConfigPath);
        if (configResource.exists()) {
            bean.setConfigLocation(configResource);
            log.info("Loaded MyBatis config from: {}", myBatisConfigPath);
        } else {
            log.warn("MyBatis config file not found, using default configuration");
        }
    } else {
        log.info("No MyBatis config file specified, using Spring Boot auto-configuration");
    }

    return bean.getObject();
}
```

### 配置迁移指南

**从 PageHelper 迁移到 MyBatis-Plus**：

```xml
<!-- ❌ 旧配置（PageHelper） -->
<plugins>
    <plugin interceptor="com.github.pagehelper.PageInterceptor">
        <property name="helperDialect" value="mysql"/>
    </plugin>
</plugins>

<!-- ✅ 新配置（MyBatis-Plus 内置分页） -->
<!-- 无需配置，自动启用 -->
```

**使用方式**：

```java
// MyBatis-Plus 分页
Page<User> page = new Page<>(pageNum, pageSize);
IPage<User> result = userMapper.selectPage(page, queryWrapper);
```

---

## 3. 完整示例项目

### 项目结构

```
raf-framework-examples/mybatis-example/
├── pom.xml
├── README.md
├── src/main/java/
│   └── com/github/raf/examples/mybatis/
│       ├── MybatisExampleApplication.java
│       ├── config/
│       │   ├── DataSourceConfig.java          # 多数据源配置
│       │   └── MyBatisConfig.java             # Mapper 扫描
│       ├── entity/
│       │   └── User.java                      # 实体类
│       ├── dao/
│       │   └── UserMapper.java                # Mapper 接口
│       ├── service/
│       │   └── UserService.java               # 业务层（读写分离）
│       └── controller/
│           └── UserController.java            # REST API
└── src/main/resources/
    ├── bootstrap.yml                          # 配置文件
    └── mapper/
        └── UserMapper.xml                     # Mapper XML
```

### 核心功能演示

#### 1. 多数据源配置

```yaml
spring.datasource:
  primary-master:
    url: jdbc:mysql://localhost:3306/db
    username: root
    password: root123
    initial-size: 10
    max-active: 50

  primary-slave:
    url: jdbc:mysql://localhost:3307/db
    username: root
    password: root123
    max-active: 100
```

#### 2. 读写分离

```java
// 写操作 - 使用主库
@DsSelector(DataSourceConfig.DS_MASTER)
@Transactional(rollbackFor = Exception.class)
public User createUser(User user) {
    userMapper.insert(user);
    return user;
}

// 读操作 - 使用从库
@DsSelector(DataSourceConfig.DS_SLAVE)
public User getUserById(Long id) {
    return userMapper.selectById(id);
}
```

#### 3. 分页查询

```java
@DsSelector(DataSourceConfig.DS_SLAVE)
public IPage<User> paginateUsers(int pageNum, int pageSize) {
    Page<User> page = new Page<>(pageNum, pageSize);
    return userMapper.selectPage(page, null);
}
```

#### 4. 复杂查询

```java
@DsSelector(DataSourceConfig.DS_SLAVE)
public List<User> complexQuery(String username, Integer minAge, Integer maxAge) {
    LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
    wrapper.like(username != null, User::getUsername, username)
           .ge(minAge != null, User::getAge, minAge)
           .le(maxAge != null, User::getAge, maxAge)
           .orderByDesc(User::getCreateTime);
    return userMapper.selectList(wrapper);
}
```

### 快速启动

```bash
# 1. 克隆项目
cd raf-framework-examples/mybatis-example

# 2. 配置数据库（或使用 H2）
# 编辑 src/main/resources/bootstrap.yml

# 3. 启动应用
mvn spring-boot:run

# 4. 测试 API
curl http://localhost:8080/api/users
```

---

## 总结

### 架构设计原则

1. **框架层保持数据库无关性**
   - 不强制绑定特定数据库驱动
   - 支持多种数据库切换

2. **配置灵活可选**
   - 优先使用 Spring Boot 配置
   - 支持 XML 配置高级场景

3. **完整示例驱动**
   - 提供生产级示例代码
   - 覆盖常见使用场景
   - 包含最佳实践指导

### 最佳实践

✅ **DO**：
- 在根 POM 统一管理数据库驱动版本
- 使用 bootstrap.yml 配置 MyBatis
- 使用 @DsSelector 实现读写分离
- 使用 MyBatis-Plus 内置分页

❌ **DON'T**：
- 不要在框架层包含数据库驱动
- 不要使用 PageHelper（已被 MyBatis-Plus 替代）
- 不要在 mybatis-config.xml 中配置基础设置
- 不要忽略事务管理

### 参考资源

- 示例项目：`raf-framework-examples/mybatis-example`
- 配置文件：`bootstrap.yml`
- 文档：`README.md`
