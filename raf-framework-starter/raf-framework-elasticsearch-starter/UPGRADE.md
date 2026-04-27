# Elasticsearch Starter 升级说明

## 升级内容

将 Elasticsearch 客户端从已废弃的 `RestHighLevelClient` 升级到 Elasticsearch 8.x 官方推荐的 Java API Client。

## 主要变更

### 1. 依赖变更

**移除的依赖：**
- `elasticsearch-rest-high-level-client` (已在 ES 8.x 中废弃)
- `elasticsearch` (核心库，不再直接依赖)

**新增的依赖：**
- `co.elastic.clients:elasticsearch-java:8.17.0` - 新版 Java API Client
- `org.elasticsearch.client:elasticsearch-rest-client:8.17.0` - 底层 REST 客户端

### 2. API 变更

#### ElasticsearchAutoConfiguration
- 移除 `RestHighLevelClient` Bean
- 新增 `RestClient` Bean - 底层 REST 客户端
- 新增 `ElasticsearchClient` Bean - 新版 Java API Client
- `ElasticsearchTemplate` 现在依赖 `ElasticsearchClient`

#### ElasticsearchTemplate
完全重写以使用新的 API：
- 所有方法签名保持兼容
- 内部实现使用新的 `ElasticsearchClient`
- 移除了部分高级功能（高亮搜索、聚合查询），可通过 `getClient()` 获取原生客户端自行实现

#### ElasticsearchHelper
查询构建器 API 变更：
- 返回类型从 `QueryBuilder` 改为 `Query`
- 移除 `SortBuilder` 相关方法（排序建议直接使用 API）
- 移除 `rangeQuery` 辅助方法（建议直接使用 `Query.of(q -> q.range(...))`)

## 迁移指南

### 旧代码示例
```java
// 旧 API (RestHighLevelClient)
QueryBuilder query = QueryBuilders.matchQuery("title", "elasticsearch");
SortBuilder<?> sort = SortBuilders.fieldSort("createTime").order(SortOrder.DESC);
```

### 新代码示例
```java
// 新 API (ElasticsearchClient)
Query query = ElasticsearchHelper.matchQuery("title", "elasticsearch");

// 或直接使用 Query.of
Query query = Query.of(q -> q.match(m -> m.field("title").query("elasticsearch")));

// 范围查询
Query rangeQuery = Query.of(q -> q.range(r -> r
    .field("age")
    .gte(JsonData.of(18))
    .lte(JsonData.of(65))
));
```

### ElasticsearchTemplate 使用
```java
@Autowired
private ElasticsearchTemplate elasticsearchTemplate;

// 基本操作保持不变
String id = elasticsearchTemplate.save("my-index", "1", document);
Document doc = elasticsearchTemplate.getById("my-index", "1", Document.class);

// 搜索
Query query = ElasticsearchHelper.matchQuery("title", "test");
List<Document> results = elasticsearchTemplate.search("my-index", query, Document.class);

// 分页搜索
EsPageResult<Document> page = elasticsearchTemplate.searchPage("my-index", query, 0, 10, Document.class);
```

### 高级用法
如需使用高级功能，可直接获取原生客户端：
```java
ElasticsearchClient client = elasticsearchTemplate.getClient();

// 使用原生 API
SearchResponse<Document> response = client.search(s -> s
    .index("my-index")
    .query(q -> q.match(m -> m.field("title").query("test")))
    .highlight(h -> h.fields("title", f -> f))
    .aggregations("by_category", a -> a.terms(t -> t.field("category")))
    , Document.class
);
```

## 兼容性说明

- **Elasticsearch 版本**: 8.17.0
- **Spring Boot 版本**: 3.4.7
- **JDK 版本**: 17+

## 注意事项

1. 新 API 使用函数式编程风格，代码更简洁但需要适应
2. 部分高级功能（如高亮、聚合）需要直接使用原生客户端
3. 查询构建器不再支持链式调用，改为 lambda 表达式
4. 建议阅读官方文档：https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/current/index.html

## 升级日期

2026-04-20
