# raf-example-mongodb-starter

> 演示 `raf-framework-mongodb-starter` 的核心能力：Spring Data MongoDB Repository、MongoTemplate 文档操作、多数据源切换。

## 功能概述

`raf-framework-mongodb-starter` 提供：

- **多数据源支持**：通过 `raf.mongodb.dataSources.{name}` 配置多个 MongoDB 实例
- **Spring Data MongoDB**：完整支持 MongoTemplate 和 Repository 模式
- **自动路由**：通过 `@Qualifier` 注入指定数据源的 MongoTemplate
- **文档审计**：支持 `@CreatedDate`、`@LastModifiedDate` 自动填充

## 快速接入

### 1. 引入依赖

```xml
<dependency>
    <groupId>io.github.jerryraf</groupId>
    <artifactId>raf-framework-mongodb-starter</artifactId>
</dependency>
<!-- 或直接使用 Spring Boot MongoDB Starter -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-mongodb</artifactId>
</dependency>
```

### 2. 配置（`application.yml`）

单数据源（主数据源必须通过 `raf.mongodb.uri` 配置）：

```yaml
spring:
  application:
    name: raf-example-mongodb-starter

raf:
  mongodb:
    enabled: true
    uri: mongodb://${MONGO_USERNAME:}:${MONGO_PASSWORD:}@${MONGO_HOST:localhost}:27017/primary_db
    database: primary_db
```

多数据源（额外数据源通过 `dataSources` 配置）：

```yaml
raf:
  mongodb:
    enabled: true
    uri: mongodb://${MONGO_HOST:localhost}:27017/primary_db
    database: primary_db
    dataSources:
      log:
        uri: mongodb://${MONGO_HOST:localhost}:27017/log_db
        database: log_db
```

## 配置项详解

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `raf.mongodb.enabled` | boolean | `false` | 是否启用 MongoDB 自动配置 |
| `raf.mongodb.uri` | string | — | 主数据源连接 URI（必填） |
| `raf.mongodb.database` | string | — | 主数据源数据库名（URI 中未指定时使用） |
| `raf.mongodb.traceEnabled` | boolean | `true` | 是否启用链路追踪 |
| `raf.mongodb.pool.maxSize` | int | `100` | 连接池最大连接数 |
| `raf.mongodb.pool.minSize` | int | `10` | 连接池最小连接数 |
| `raf.mongodb.pool.maxWaitTime` | long | `2000` | 获取连接最大等待时间（毫秒） |
| `raf.mongodb.pool.connectTimeout` | int | `10000` | 连接超时时间（毫秒） |
| `raf.mongodb.pool.socketTimeout` | int | `0` | Socket 超时时间（毫秒，0 不限制） |
| `raf.mongodb.dataSources.{name}.uri` | string | — | 额外数据源连接 URI |
| `raf.mongodb.dataSources.{name}.database` | string | — | 额外数据源数据库名 |
| `raf.mongodb.dataSources.{name}.pool.*` | — | 全局配置 | 额外数据源独立连接池配置（不配置则继承全局） |

多数据源访问方式：注入 `Map<String, MongoTemplate> mongoTemplateMap`，通过 `mongoTemplateMap.get("log")` 获取对应数据源的 MongoTemplate。主数据源直接注入 `MongoTemplate`（`@Primary` Bean）。

## 核心用法

### 文档定义

```java
@Document(collection = "articles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String title;

    @Field("content")
    private String content;

    private String author;

    private List<String> tags;

    private Integer viewCount;

    @CreatedDate
    private LocalDateTime createTime;

    @LastModifiedDate
    private LocalDateTime updateTime;
}
```

> 启用审计功能需在启动类或配置类上加 `@EnableMongoAuditing`。

### Repository 模式

```java
@Repository
public interface ArticleRepository extends MongoRepository<ArticleDocument, String> {

    // 按作者查询
    List<ArticleDocument> findByAuthor(String author);

    // 按标签查询（tags 是 List 字段）
    List<ArticleDocument> findByTagsContaining(String tag);

    // 按标题模糊查询
    List<ArticleDocument> findByTitleContaining(String keyword);

    // 分页查询
    Page<ArticleDocument> findByAuthor(String author, Pageable pageable);
}
```

### Service 层

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleService {

    private final ArticleRepository articleRepository;

    public ArticleDocument createArticle(ArticleCreateReq req) {
        // 检查标题唯一性
        if (articleRepository.findByTitleContaining(req.getTitle()).stream()
            .anyMatch(a -> a.getTitle().equals(req.getTitle()))) {
            throw new BusinessException(ArticleErrorCode.TITLE_ALREADY_EXISTS);
        }

        ArticleDocument article = ArticleDocument.builder()
            .title(req.getTitle())
            .content(req.getContent())
            .author(req.getAuthor())
            .tags(req.getTags())
            .viewCount(0)
            .build();

        return articleRepository.save(article);
    }

    public ArticleDocument getById(String id) {
        return articleRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ArticleErrorCode.ARTICLE_NOT_FOUND));
    }

    public List<ArticleDocument> getByAuthor(String author) {
        return articleRepository.findByAuthor(author);
    }

    public List<ArticleDocument> getByTag(String tag) {
        return articleRepository.findByTagsContaining(tag);
    }

    public void deleteArticle(String id) {
        if (!articleRepository.existsById(id)) {
            throw new BusinessException(ArticleErrorCode.ARTICLE_NOT_FOUND);
        }
        articleRepository.deleteById(id);
        log.info("文章已删除，id={}", id);
    }
}
```

### MongoTemplate 高级操作

```java
@Autowired
private MongoTemplate mongoTemplate;

// 条件查询
public List<ArticleDocument> searchArticles(String keyword, String author) {
    Criteria criteria = new Criteria();
    List<Criteria> conditions = new ArrayList<>();

    if (StringUtils.isNotBlank(keyword)) {
        conditions.add(Criteria.where("title").regex(keyword, "i"));
    }
    if (StringUtils.isNotBlank(author)) {
        conditions.add(Criteria.where("author").is(author));
    }

    if (!conditions.isEmpty()) {
        criteria.andOperator(conditions.toArray(new Criteria[0]));
    }

    Query query = new Query(criteria)
        .with(Sort.by(Sort.Direction.DESC, "createTime"))
        .limit(20);

    return mongoTemplate.find(query, ArticleDocument.class);
}

// 原子更新（增加浏览量）
public void incrementViewCount(String articleId) {
    Query query = new Query(Criteria.where("_id").is(articleId));
    Update update = new Update().inc("viewCount", 1);
    mongoTemplate.updateFirst(query, update, ArticleDocument.class);
}

// 聚合查询（按作者统计文章数）
public List<AuthorStats> getAuthorStats() {
    Aggregation aggregation = Aggregation.newAggregation(
        Aggregation.group("author").count().as("articleCount"),
        Aggregation.sort(Sort.Direction.DESC, "articleCount"),
        Aggregation.limit(10)
    );
    AggregationResults<AuthorStats> results =
        mongoTemplate.aggregate(aggregation, "articles", AuthorStats.class);
    return results.getMappedResults();
}
```

### 多数据源切换

框架将所有数据源的 MongoTemplate 注册到 `mongoTemplateMap` Bean 中（key 为数据源名称），主数据源同时注册为 `@Primary` 的 `mongoTemplate` Bean。

```java
@Service
@RequiredArgsConstructor
public class MultiSourceService {

    // 主数据源：直接注入 @Primary MongoTemplate
    private final MongoTemplate mongoTemplate;

    // 所有数据源 Map：key 为 dataSources 中配置的名称
    @Autowired
    private Map<String, MongoTemplate> mongoTemplateMap;

    public void saveArticle(ArticleDocument article) {
        // 保存到主数据源
        mongoTemplate.insert(article, "articles");
    }

    public void saveOperationLog(OperationLog log) {
        // 保存到日志数据源（对应 raf.mongodb.dataSources.log）
        MongoTemplate logTemplate = mongoTemplateMap.get("log");
        logTemplate.insert(log, "operation_logs");
    }
}
```

## 示例项目结构

```
raf-example-mongodb-starter/
├── src/main/java/io/github/jerryraf/examples/mongodb/
│   ├── MongodbExampleApplication.java
│   ├── document/
│   │   └── ArticleDocument.java        # @Document 文档定义
│   ├── repository/
│   │   └── ArticleRepository.java      # Spring Data Repository
│   ├── service/
│   │   └── ArticleService.java         # 业务逻辑
│   ├── controller/
│   │   └── ArticleController.java      # REST API
│   └── dto/
│       ├── ArticleCreateReq.java
│       └── ArticleRes.java
└── src/main/resources/
    └── application.yml
```

## API 接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/articles` | 创建文章 |
| GET | `/articles/{id}` | 根据 ID 查询 |
| GET | `/articles/author/{author}` | 按作者查询 |
| GET | `/articles/tag/{tag}` | 按标签查询 |
| DELETE | `/articles/{id}` | 删除文章 |

## 最佳实践

1. **索引设计**：在 `@Document` 类上使用 `@Indexed` 或 `@CompoundIndex` 声明索引，避免全集合扫描
2. **文档大小**：单个文档不超过 16MB（MongoDB 限制），大文件使用 GridFS
3. **嵌套 vs 引用**：频繁一起查询的数据嵌套存储；独立查询的数据用引用（存 ID）
4. **审计字段**：使用 `@CreatedDate`/`@LastModifiedDate` 自动填充时间，需启用 `@EnableMongoAuditing`
5. **连接池**：生产环境配置合理的连接池大小，避免连接耗尽
6. **事务**：MongoDB 4.0+ 支持多文档事务，使用 `@Transactional` 注解，但性能开销较大，谨慎使用

## 常见问题

**Q: 多数据源时如何获取指定数据源的 MongoTemplate？**

A: 框架将所有数据源注册到 `Map<String, MongoTemplate> mongoTemplateMap` Bean 中，key 为 `dataSources` 中配置的名称。主数据源同时注册为 `@Primary` 的 `mongoTemplate` Bean，可直接注入。额外数据源通过 `mongoTemplateMap.get("数据源名称")` 获取。

**Q: `@CreatedDate` 和 `@LastModifiedDate` 不生效？**

A: 需要在启动类或配置类上加 `@EnableMongoAuditing` 注解。

**Q: 连接 MongoDB Atlas（云服务）时 SSL 报错？**

A: 在 URI 中添加 `?tls=true&tlsAllowInvalidCertificates=true`，或配置 JVM 信任证书。

**Q: 查询性能慢？**

A: 使用 `mongoTemplate.indexOps("collection").ensureIndex(...)` 或 `@Indexed` 注解创建索引。通过 MongoDB Compass 或 `explain()` 分析查询计划。

**Q: 如何处理 ObjectId 与 String 的转换？**

A: Spring Data MongoDB 自动处理 `@Id` 字段的 ObjectId ↔ String 转换，无需手动处理。
