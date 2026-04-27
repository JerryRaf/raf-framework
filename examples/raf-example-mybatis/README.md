# RAF Framework MyBatis Example

Complete example demonstrating enterprise-grade MyBatis-Plus integration with multi-datasource, read-write splitting, and pagination.

## Features

- ✅ **Multi-DataSource Configuration** - Master-Slave architecture
- ✅ **Read-Write Splitting** - Automatic routing with `@DsSelector`
- ✅ **MyBatis-Plus Integration** - Enhanced CRUD operations
- ✅ **Pagination Support** - Built-in pagination plugin
- ✅ **Transaction Management** - Declarative transactions
- ✅ **Connection Pooling** - Druid with monitoring
- ✅ **Logic Delete** - Soft delete support
- ✅ **Optimistic Locking** - Version-based concurrency control
- ✅ **Auto-Fill Fields** - Automatic timestamp and user tracking

## Architecture

```
┌─────────────────────────────────────────┐
│  Controller Layer                       │
│  - REST API endpoints                   │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│  Service Layer                          │
│  - @DsSelector(DS_MASTER) for writes   │
│  - @DsSelector(DS_SLAVE) for reads     │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│  DynamicRoutingDataSource               │
│  (Thread-local based routing)           │
└─────────────────────────────────────────┘
         ↙                    ↘
┌──────────────┐      ┌──────────────┐
│ Master DS    │      │ Slave DS     │
│ (Write)      │      │ (Read)       │
└──────────────┘      └──────────────┘
```

## Quick Start

### 1. Database Setup

Create database and table:

```sql
CREATE DATABASE example_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE example_db;

CREATE TABLE t_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Primary Key',
    username VARCHAR(50) NOT NULL COMMENT 'Username',
    email VARCHAR(100) COMMENT 'Email',
    age INT COMMENT 'Age',
    status INT DEFAULT 1 COMMENT 'Status (1: Active, 0: Inactive)',
    is_deleted INT DEFAULT 0 COMMENT 'Logic Delete (1: Deleted, 0: Not Deleted)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Create Time',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update Time',
    create_by VARCHAR(50) COMMENT 'Creator',
    update_by VARCHAR(50) COMMENT 'Updater',
    version INT DEFAULT 0 COMMENT 'Version (Optimistic Lock)',
    INDEX idx_username (username),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User Table';
```

### 2. Configuration

Update `src/main/resources/bootstrap.yml`:

```yaml
spring.datasource:
  primary-master:
    url: jdbc:mysql://localhost:3306/example_db?useUnicode=true&characterEncoding=utf-8
    username: root
    password: your_password

  primary-slave:
    url: jdbc:mysql://localhost:3307/example_db?useUnicode=true&characterEncoding=utf-8
    username: root
    password: your_password
```

### 3. Run Application

```bash
mvn clean install
mvn spring-boot:run
```

Or use H2 in-memory database for testing:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 4. Test API

```bash
# Create user
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"username":"john","email":"john@example.com","age":25,"status":1}'

# Get user by ID
curl http://localhost:8080/api/users/1

# List all users
curl http://localhost:8080/api/users

# Paginate users
curl "http://localhost:8080/api/users/page?pageNum=1&pageSize=10"

# Search by username
curl "http://localhost:8080/api/users/search?username=john"

# Find by age range
curl "http://localhost:8080/api/users/age-range?minAge=20&maxAge=30"
```

## Key Components

### 1. DataSource Configuration

**File**: `config/DataSourceConfig.java`

```java
@Primary
@Bean(name = "routingDataSource")
public DataSource routingDataSource(
        @Qualifier(DS_MASTER) DataSource masterDataSource,
        @Qualifier(DS_SLAVE) DataSource slaveDataSource) {
    
    DynamicRoutingDataSource routingDataSource = new DynamicRoutingDataSource();
    routingDataSource.setDefaultTargetDataSource(masterDataSource);
    
    Map<Object, Object> dsMap = new HashMap<>();
    dsMap.put(DS_MASTER, masterDataSource);
    dsMap.put(DS_SLAVE, slaveDataSource);
    routingDataSource.setTargetDataSources(dsMap);
    
    return routingDataSource;
}
```

### 2. Read-Write Splitting

**File**: `service/UserService.java`

```java
// Write operations use Master
@DsSelector(DataSourceConfig.DS_MASTER)
@Transactional(rollbackFor = Exception.class)
public User createUser(User user) {
    userMapper.insert(user);
    return user;
}

// Read operations use Slave
@DsSelector(DataSourceConfig.DS_SLAVE)
public User getUserById(Long id) {
    return userMapper.selectById(id);
}
```

### 3. Pagination

**MyBatis-Plus built-in pagination**:

```java
@DsSelector(DataSourceConfig.DS_SLAVE)
public IPage<User> paginateUsers(int pageNum, int pageSize) {
    Page<User> page = new Page<>(pageNum, pageSize);
    return userMapper.selectPage(page, null);
}
```

### 4. Custom Queries

**Mapper Interface**: `dao/UserMapper.java`

```java
List<User> findByAgeRange(@Param("minAge") Integer minAge, 
                          @Param("maxAge") Integer maxAge);
```

**Mapper XML**: `mapper/UserMapper.xml`

```xml
<select id="findByAgeRange" resultMap="BaseResultMap">
    SELECT * FROM t_user
    WHERE is_deleted = 0
    AND age BETWEEN #{minAge} AND #{maxAge}
    ORDER BY age ASC
</select>
```

## Configuration Highlights

### bootstrap.yml Structure

```yaml
spring.datasource:
  primary-master:      # Master datasource (writes)
    url: ...
    username: ...
    password: ...
    # Connection pool settings
    initial-size: 10
    max-active: 50
    
  primary-slave:       # Slave datasource (reads)
    url: ...
    username: ...
    password: ...
    # Larger pool for read operations
    max-active: 100

mybatis:
  mapper-locations: classpath*:mapper/**/*.xml
  type-aliases-package: io.github.jerryraf.examples.mybatis.entity

mybatis-plus:
  global-config:
    db-config:
      id-type: AUTO
      logic-delete-field: isDeleted
      logic-delete-value: 1
      logic-not-delete-value: 0
```

## Best Practices

### 1. Database Driver Selection

**Framework Design Principle**: Keep framework database-agnostic

```xml
<!-- Application POM - Choose your database -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
</dependency>

<!-- Or PostgreSQL -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>
```

### 2. mybatis-config.xml is Optional

With Spring Boot and MyBatis-Plus, most configurations can be done in `bootstrap.yml`:

```yaml
mybatis:
  configuration:
    map-underscore-to-camel-case: true
    lazy-loading-enabled: true
    use-generated-keys: true
    jdbc-type-for-null: NULL
```

Only use `mybatis-config.xml` for advanced configurations like custom type handlers.

### 3. Read-Write Splitting Strategy

- **Writes**: Always use `@DsSelector(DS_MASTER)`
- **Reads**: Use `@DsSelector(DS_SLAVE)` for better performance
- **Transactions**: Automatically route to master within `@Transactional`

### 4. Pagination Best Practices

```java
// Good: Use MyBatis-Plus Page
Page<User> page = new Page<>(pageNum, pageSize);
IPage<User> result = userMapper.selectPage(page, wrapper);

// Avoid: Manual LIMIT/OFFSET in SQL
// This bypasses pagination plugin optimizations
```

## Troubleshooting

### Issue 1: ClassNotFoundException: com.mysql.cj.jdbc.Driver

**Solution**: Add MySQL driver dependency to your application POM:

```xml
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
</dependency>
```

### Issue 2: PageHelper ClassNotFoundException

**Solution**: Remove PageHelper from `mybatis-config.xml`. MyBatis-Plus has built-in pagination.

### Issue 3: Datasource routing not working

**Solution**: Ensure `@DsSelector` is on public methods and AOP is enabled:

```java
@EnableAspectJAutoProxy(proxyTargetClass = true)
```

## Project Structure

```
mybatis-example/
├── src/main/java/
│   └── com/github/raf/examples/mybatis/
│       ├── MybatisExampleApplication.java
│       ├── config/
│       │   ├── DataSourceConfig.java
│       │   └── MyBatisConfig.java
│       ├── entity/
│       │   └── User.java
│       ├── dao/
│       │   └── UserMapper.java
│       ├── service/
│       │   └── UserService.java
│       └── controller/
│           └── UserController.java
├── src/main/resources/
│   ├── bootstrap.yml
│   └── mapper/
│       └── UserMapper.xml
└── pom.xml
```

## References

- [RAF Framework Documentation](https://github.com/raf-framework)
- [MyBatis-Plus Documentation](https://baomidou.com/)
- [Druid Documentation](https://github.com/alibaba/druid)

## License

Apache License 2.0
