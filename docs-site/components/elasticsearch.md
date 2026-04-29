# Elasticsearch（elasticsearch-starter）

## 功能概述

- **ES 客户端封装**：基于 Elasticsearch Java Client 7.x
- **索引管理**：自动创建/更新索引映射
- **文档操作**：增删改查、批量操作
- **全文搜索**：支持复杂查询 DSL

## 配置项

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `raf.elasticsearch.enabled` | boolean | `false` | 是否启用 Elasticsearch |
| `raf.elasticsearch.hosts` | string | — | ES 节点地址，逗号分隔，格式 `host:port` |
| `raf.elasticsearch.username` | string | — | 用户名（X-Pack 安全认证） |
| `raf.elasticsearch.password` | string | — | 密码 |
| `raf.elasticsearch.connectTimeout` | int | `5000` | 连接超时（毫秒） |
| `raf.elasticsearch.socketTimeout` | int | `30000` | Socket 超时（毫秒） |
| `raf.elasticsearch.maxConnTotal` | int | `100` | 最大连接数 |

## 快速接入

```xml
<dependency>
    <groupId>io.github.jerryraf</groupId>
    <artifactId>raf-framework-elasticsearch-starter</artifactId>
</dependency>
```

```yaml
raf:
  elasticsearch:
    enabled: true
    hosts: 127.0.0.1:9200
    username: elastic
    password: your_password
    connectTimeout: 5000
    socketTimeout: 30000
```

## 核心用法

### 索引文档

```java
@Autowired
private ElasticsearchClient esClient;

// 索引单个文档
ProductDocument product = new ProductDocument();
product.setId("1001");
product.setName("iPhone 15");
product.setPrice(new BigDecimal("7999"));
product.setCategory("手机");

IndexResponse response = esClient.index(i -> i
    .index("products")
    .id(product.getId())
    .document(product)
);
log.info("索引结果: {}", response.result());
```

### 全文搜索

```java
// 多字段全文搜索 + 过滤
SearchResponse<ProductDocument> response = esClient.search(s -> s
    .index("products")
    .query(q -> q
        .bool(b -> b
            .must(m -> m.multiMatch(mm -> mm
                .query("iPhone")
                .fields("name", "description")
            ))
            .filter(f -> f.range(r -> r
                .field("price")
                .gte(JsonData.of(5000))
                .lte(JsonData.of(10000))
            ))
        )
    )
    .from(0)
    .size(10)
    .sort(so -> so.field(f -> f.field("price").order(SortOrder.Asc))),
    ProductDocument.class
);

List<ProductDocument> products = response.hits().hits().stream()
    .map(Hit::source)
    .collect(Collectors.toList());
```

### 批量操作

```java
List<BulkOperation> operations = products.stream()
    .map(p -> BulkOperation.of(op -> op
        .index(i -> i.index("products").id(p.getId()).document(p))
    ))
    .collect(Collectors.toList());

BulkResponse bulkResponse = esClient.bulk(b -> b.operations(operations));
if (bulkResponse.errors()) {
    log.error("批量索引存在错误");
}
```

## 常见问题

**Q: ES 版本与框架版本不匹配怎么办？**

A: 框架集成的是 ES 7.17.x 客户端。如果你的 ES 服务端版本不同，可以在应用层覆盖 `elasticsearch` 依赖版本。

**Q: 索引映射变更后如何更新？**

A: ES 不支持修改已有字段类型。需要创建新索引，通过 Reindex API 迁移数据，再切换别名。

**Q: 搜索结果评分不准确？**

A: 检查分词器配置，中文场景推荐使用 `ik_max_word`（索引时）和 `ik_smart`（搜索时）分词器。
