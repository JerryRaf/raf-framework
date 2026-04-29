# MongoDB 多数据源（mongodb-starter）

## 功能概述

- **多数据源支持**：通过 `raf.mongodb.dataSources.{name}` 配置多个 MongoDB 实例
- **自动路由**：`@MongoDs` 注解切换数据源
- **Spring Data MongoDB**：完整支持 MongoTemplate 和 Repository

## 配置项

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `raf.mongodb.enabled` | boolean | `false` | 是否启用 MongoDB |
| `raf.mongodb.dataSources.{name}.uri` | string | — | MongoDB 连接 URI |
| `raf.mongodb.dataSources.{name}.database` | string | — | 数据库名 |
| `raf.mongodb.dataSources.{name}.primary` | boolean | `false` | 是否为主数据源 |

## 快速接入

**1. 引入依赖**

```xml
<dependency>
    <groupId>io.github.jerryraf</groupId>
    <artifactId>raf-framework-mongodb-starter</artifactId>
</dependency>
```

**2. 配置**（`application.yml`）

```yaml
raf:
  mongodb:
    enabled: true
    dataSources:
      primary:
        uri: mongodb://user:password@localhost:27017/primary_db
        database: primary_db
        primary: true
      log:
        uri: mongodb://user:password@localhost:27017/log_db
        database: log_db
```

## 核心用法

### 使用 MongoTemplate

```java
// 注入主数据源的 MongoTemplate
@Autowired
@Qualifier("primaryMongoTemplate")
private MongoTemplate mongoTemplate;

// 插入文档
UserDocument user = new UserDocument();
user.setName("张三");
user.setAge(25);
mongoTemplate.insert(user, "users");

// 查询
Query query = new Query(Criteria.where("name").is("张三"));
UserDocument found = mongoTemplate.findOne(query, UserDocument.class, "users");

// 更新
Update update = new Update().set("age", 26);
mongoTemplate.updateFirst(query, update, UserDocument.class, "users");
```

### 切换数据源

```java
// 注入日志数据源的 MongoTemplate
@Autowired
@Qualifier("logMongoTemplate")
private MongoTemplate logMongoTemplate;

public void saveLog(OperationLog log) {
    logMongoTemplate.insert(log, "operation_logs");
}
```

### 文档定义

```java
@Document(collection = "users")
@Data
public class UserDocument {

    @Id
    private String id;

    @Field("name")
    private String name;

    private Integer age;

    @CreatedDate
    private LocalDateTime createTime;

    @LastModifiedDate
    private LocalDateTime updateTime;
}
```

## 常见问题

**Q: 多数据源时 MongoTemplate Bean 名称是什么？**

A: 命名规则为 `{name}MongoTemplate`，例如配置了 `dataSources.primary`，Bean 名称为 `primaryMongoTemplate`。

**Q: `@CreatedDate` 和 `@LastModifiedDate` 不生效？**

A: 需要在启动类或配置类上加 `@EnableMongoAuditing` 注解。

**Q: 连接 MongoDB Atlas（云服务）时 SSL 报错？**

A: 在 URI 中添加 `?tls=true&tlsAllowInvalidCertificates=true`，或配置 JVM 信任证书。
