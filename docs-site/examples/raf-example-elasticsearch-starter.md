# raf-example-elasticsearch-starter

> 演示 `raf-framework-elasticsearch-starter` 的核心能力：索引管理、文档 CRUD、全文搜索（IK 分词）、复杂查询 DSL。

## 功能概述

`raf-framework-elasticsearch-starter` 提供：

- **ES 客户端封装**：基于 Spring Data Elasticsearch
- **索引管理**：通过 `@Document` 注解自动创建/更新索引映射
- **文档操作**：增删改查、批量操作
- **全文搜索**：支持 IK 分词、多字段搜索、高亮显示
- **复杂查询**：bool 查询、范围过滤、聚合统计

## 快速接入

### 1. 引入依赖

```xml
<dependency>
    <groupId>io.github.jerryraf</groupId>
    <artifactId>raf-framework-elasticsearch-starter</artifactId>
</dependency>
```

### 2. 配置（`application.yml`）

```yaml
spring:
  application:
    name: raf-example-elasticsearch-starter

raf:
  elasticsearch:
    enabled: true
    hosts:
      - ${ES_HOST:127.0.0.1}:${ES_PORT:9200}
    username: ${ES_USERNAME:}
    password: ${ES_PASSWORD:}
    connect-timeout: 5s
    socket-timeout: 30s
    connection-request-timeout: 3s
    max-connections: 100
    max-connections-per-route: 50
```

## 配置项详解

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `raf.elasticsearch.enabled` | boolean | `false` | 是否启用 Elasticsearch |
| `raf.elasticsearch.hosts` | list | — | ES 节点地址列表，格式 `host:port` |
| `raf.elasticsearch.username` | string | — | 用户名（X-Pack 安全认证） |
| `raf.elasticsearch.password` | string | — | 密码 |
| `raf.elasticsearch.connect-timeout` | duration | `10s` | 连接超时 |
| `raf.elasticsearch.socket-timeout` | duration | `30s` | Socket 超时 |
| `raf.elasticsearch.connection-request-timeout` | duration | `5s` | 从连接池获取连接的超时 |
| `raf.elasticsearch.max-connections` | int | `100` | 最大连接数 |
| `raf.elasticsearch.max-connections-per-route` | int | `50` | 每路由最大连接数 |

## 核心用法

### 文档定义

```java
@Document(indexName = "products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDocument {

    @Id
    private String id;

    // IK 分词：索引时细粒度分词，搜索时智能分词
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String name;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String description;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Double)
    private BigDecimal price;

    @Field(type = FieldType.Integer)
    private Integer stock;

    @Field(type = FieldType.Keyword)
    private List<String> tags;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime createTime;
}
```

### Repository 模式

```java
@Repository
public interface ProductRepository extends ElasticsearchRepository<ProductDocument, String> {

    // 按分类查询
    List<ProductDocument> findByCategory(String category);

    // 按名称模糊查询
    List<ProductDocument> findByNameContaining(String keyword);

    // 价格范围查询
    List<ProductDocument> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);
}
```

### 全文搜索（CriteriaQuery）

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSearchService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final ProductRepository productRepository;

    /**
     * 跨字段全文搜索（name OR description）
     */
    public List<ProductDocument> search(String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return Collections.emptyList();
        }

        Criteria criteria = new Criteria("name").matches(keyword)
            .or(new Criteria("description").matches(keyword));

        Query query = new CriteriaQuery(criteria);
        SearchHits<ProductDocument> hits =
            elasticsearchOperations.search(query, ProductDocument.class);

        return hits.stream()
            .map(SearchHit::getContent)
            .collect(Collectors.toList());
    }

    /**
     * 复合查询：关键词 + 分类过滤 + 价格范围 + 分页
     */
    public SearchPage<ProductDocument> complexSearch(ProductSearchReq req) {
        BoolQuery.Builder boolQuery = new BoolQuery.Builder();

        // 全文搜索（must）
        if (StringUtils.isNotBlank(req.getKeyword())) {
            boolQuery.must(m -> m.multiMatch(mm -> mm
                .query(req.getKeyword())
                .fields("name^2", "description")  // name 字段权重 x2
            ));
        }

        // 分类过滤（filter，不影响评分）
        if (StringUtils.isNotBlank(req.getCategory())) {
            boolQuery.filter(f -> f.term(t -> t
                .field("category")
                .value(req.getCategory())
            ));
        }

        // 价格范围过滤
        if (req.getMinPrice() != null || req.getMaxPrice() != null) {
            boolQuery.filter(f -> f.range(r -> {
                r.field("price");
                if (req.getMinPrice() != null) r.gte(JsonData.of(req.getMinPrice()));
                if (req.getMaxPrice() != null) r.lte(JsonData.of(req.getMaxPrice()));
                return r;
            }));
        }

        NativeQuery query = NativeQuery.builder()
            .withQuery(q -> q.bool(boolQuery.build()))
            .withPageable(PageRequest.of(req.getPageNum() - 1, req.getPageSize()))
            .withSort(Sort.by(Sort.Direction.DESC, "_score"))
            .build();

        SearchHits<ProductDocument> hits =
            elasticsearchOperations.search(query, ProductDocument.class);

        return SearchHitSupport.searchPageFor(hits, query.getPageable());
    }

    /**
     * 批量索引文档
     */
    public void bulkIndex(List<ProductDocument> products) {
        productRepository.saveAll(products);
        log.info("批量索引完成，数量={}", products.size());
    }

    /**
     * 按 ID 删除文档
     */
    public void deleteProduct(String id) {
        productRepository.deleteById(id);
        log.info("文档已删除，id={}", id);
    }
}
```

### Controller 层

```java
@Tag(name = "商品搜索", description = "Elasticsearch 全文搜索示例")
@RestController
@RequestMapping("/products")
@ResponseResult
@RequiredArgsConstructor
public class ProductSearchController {

    private final ProductSearchService productSearchService;

    @Operation(summary = "全文搜索商品")
    @GetMapping("/search")
    public List<ProductDocument> search(
        @Parameter(description = "搜索关键词") @RequestParam String keyword) {
        return productSearchService.search(keyword);
    }

    @Operation(summary = "复合条件搜索")
    @PostMapping("/search/complex")
    public SearchPage<ProductDocument> complexSearch(@RequestBody @Valid ProductSearchReq req) {
        return productSearchService.complexSearch(req);
    }

    @Operation(summary = "索引商品文档")
    @PostMapping
    public ProductDocument indexProduct(@RequestBody @Valid ProductCreateReq req) {
        ProductDocument product = ProductDocument.builder()
            .id(String.valueOf(SnowFlakeBuilder.generateId()))
            .name(req.getName())
            .description(req.getDescription())
            .category(req.getCategory())
            .price(req.getPrice())
            .stock(req.getStock())
            .tags(req.getTags())
            .createTime(LocalDateTime.now())
            .build();
        return productSearchService.bulkIndex(List.of(product)), product;
    }

    @Operation(summary = "删除商品文档")
    @DeleteMapping("/{id}")
    public void deleteProduct(@PathVariable String id) {
        productSearchService.deleteProduct(id);
    }
}
```

## 示例项目结构

```
raf-example-elasticsearch-starter/
├── src/main/java/io/github/jerryraf/examples/elasticsearch/
│   ├── ElasticsearchExampleApplication.java
│   ├── document/
│   │   └── ProductDocument.java        # @Document 索引定义
│   ├── repository/
│   │   └── ProductRepository.java      # ElasticsearchRepository
│   ├── service/
│   │   └── ProductSearchService.java   # 搜索业务逻辑
│   ├── controller/
│   │   └── ProductSearchController.java
│   └── dto/
│       ├── ProductCreateReq.java
│       └── ProductSearchReq.java
└── src/main/resources/
    └── application.yml
```

## 最佳实践

1. **IK 分词器**：中文场景必须安装 IK 分词插件，索引时用 `ik_max_word`（细粒度），搜索时用 `ik_smart`（智能合并）
2. **索引映射变更**：ES 不支持修改已有字段类型，需要创建新索引 → Reindex → 切换别名
3. **批量操作**：大量文档写入使用 `saveAll` 或 Bulk API，避免逐条写入
4. **分页深度**：避免深度分页（from > 10000），使用 `search_after` 或 Scroll API
5. **字段权重**：搜索时通过 `^N` 提升重要字段权重（如 `name^2`）
6. **连接池**：生产环境根据并发量调整 `max-connections` 和 `max-connections-per-route`
7. **密码安全**：ES 密码通过环境变量注入，配合 Jasypt 加密

## 常见问题

**Q: ES 版本与框架版本不匹配怎么办？**

A: 框架集成的是 ES 7.17.9 Spring Data Elasticsearch。服务端版本建议与客户端主版本保持一致；如需接入 8.x 集群，需升级框架依赖版本。

**Q: 索引映射变更后如何更新？**

A: ES 不支持修改已有字段类型。需要创建新索引，通过 Reindex API 迁移数据，再切换别名：

```bash
# 创建新索引
PUT /products_v2 { "mappings": {...} }

# 迁移数据
POST /_reindex
{
  "source": { "index": "products_v1" },
  "dest": { "index": "products_v2" }
}

# 切换别名
POST /_aliases
{
  "actions": [
    { "remove": { "index": "products_v1", "alias": "products" } },
    { "add": { "index": "products_v2", "alias": "products" } }
  ]
}
```

**Q: 搜索结果评分不准确？**

A: 检查分词器配置，中文场景推荐使用 `ik_max_word`（索引时）和 `ik_smart`（搜索时）分词器。

**Q: 连接 ES 时报 SSL 证书错误？**

A: 生产环境建议配置正确的 CA 证书。测试环境可在 URI 中添加 `?ssl=true&ssl.verification_mode=none`（不推荐生产使用）。
