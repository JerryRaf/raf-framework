# Quick Start Guide

## 1. 最快启动方式（H2 内存数据库）

```bash
cd D:\Framework\raf-framework\examples\raf-example-mybatis
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

访问：http://localhost:8080/api/users

## 2. 使用 MySQL 数据库

### 步骤 1：创建数据库

```bash
mysql -u root -p < src/main/resources/schema.sql
```

### 步骤 2：修改配置

编辑 `src/main/resources/bootstrap.yml`：

```yaml
spring.datasource:
  primary-master:
    url: jdbc:mysql://localhost:3306/example_db
    username: root
    password: your_password
```

### 步骤 3：启动应用

```bash
mvn spring-boot:run
```

## 3. API 测试

```bash
# 创建用户
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"username":"john","email":"john@example.com","age":25,"status":1}'

# 查询用户
curl http://localhost:8080/api/users/1

# 分页查询
curl "http://localhost:8080/api/users/page?pageNum=1&pageSize=10"

# 搜索用户
curl "http://localhost:8080/api/users/search?username=john"
```

## 4. 核心代码示例

### 读写分离

```java
// 写操作 - 主库
@DsSelector(DataSourceConfig.DS_MASTER)
@Transactional(rollbackFor = Exception.class)
public User createUser(User user) {
    userMapper.insert(user);
    return user;
}

// 读操作 - 从库
@DsSelector(DataSourceConfig.DS_SLAVE)
public User getUserById(Long id) {
    return userMapper.selectById(id);
}
```

### 分页查询

```java
@DsSelector(DataSourceConfig.DS_SLAVE)
public IPage<User> paginateUsers(int pageNum, int pageSize) {
    Page<User> page = new Page<>(pageNum, pageSize);
    return userMapper.selectPage(page, null);
}
```

## 5. 项目结构

```
raf-example-mybatis/
├── src/main/java/
│   └── com/github/raf/examples/mybatis/
│       ├── MybatisExampleApplication.java
│       ├── config/
│       │   ├── DataSourceConfig.java      # 数据源配置
│       │   └── MyBatisConfig.java         # Mapper 扫描
│       ├── entity/User.java               # 实体类
│       ├── dao/UserMapper.java            # Mapper 接口
│       ├── service/UserService.java       # 业务层
│       └── controller/UserController.java # REST API
└── src/main/resources/
    ├── bootstrap.yml                      # 配置文件
    ├── schema.sql                         # 数据库初始化
    └── mapper/UserMapper.xml              # Mapper XML
```

## 6. 常见问题

### Q: ClassNotFoundException: com.mysql.cj.jdbc.Driver

A: 添加 MySQL 驱动依赖：

```xml
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
</dependency>
```

### Q: PageHelper ClassNotFoundException

A: 移除 mybatis-config.xml 中的 PageHelper 配置，使用 MyBatis-Plus 内置分页。

### Q: 数据源路由不生效

A: 确保 `@DsSelector` 在 public 方法上，并启用 AOP：

```java
@EnableAspectJAutoProxy(proxyTargetClass = true)
```

## 7. 更多文档

- [README.md](README.md) - 完整功能说明
- [ARCHITECTURE.md](ARCHITECTURE.md) - 架构设计文档
