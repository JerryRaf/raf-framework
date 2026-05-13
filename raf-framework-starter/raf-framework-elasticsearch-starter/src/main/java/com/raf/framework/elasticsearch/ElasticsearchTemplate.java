package com.raf.framework.elasticsearch;

import com.raf.framework.core.common.exception.InfrastructureException;
import com.raf.framework.core.common.result.RafResponseEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/**
 * Elasticsearch 核心操作模板类
 * 封装常用的 ES 操作，提供简洁的 API
 * 基于 Elasticsearch 8.x Java API Client
 *
 * @author RAF Framework
 */
@Slf4j
public class ElasticsearchTemplate {

    private final ElasticsearchClient client;
    private final ObjectMapper objectMapper;
    private final ElasticsearchProperties properties;

    public ElasticsearchTemplate(ElasticsearchClient client,
                                 ObjectMapper objectMapper,
                                 ElasticsearchProperties properties) {
        this.client = client;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    // ========== 索引管理 ==========

    /**
     * 创建索引
     */
    public boolean createIndex(String indexName) {
        return createIndex(indexName, null, null);
    }

    /**
     * 创建索引（带 Mapping 和 Settings）
     */
    public boolean createIndex(String indexName, String mapping, String settings) {
        try {
            if (existsIndex(indexName)) {
                log.warn("Index [{}] already exists", indexName);
                return true;
            }

            CreateIndexRequest.Builder builder = new CreateIndexRequest.Builder();
            builder.index(indexName);

            // 设置 Settings
            if (StringUtils.hasText(settings)) {
                // 使用 JSON 字符串设置
                builder.settings(s -> s.withJson(new java.io.StringReader(settings)));
            } else {
                // 使用默认配置
                builder.settings(s -> s
                        .numberOfShards(String.valueOf(properties.getIndex().getNumberOfShards()))
                        .numberOfReplicas(String.valueOf(properties.getIndex().getNumberOfReplicas()))
                        .refreshInterval(t -> t.time(properties.getIndex().getRefreshInterval()))
                );
            }

            // 设置 Mapping
            if (StringUtils.hasText(mapping)) {
                builder.mappings(m -> m.withJson(new java.io.StringReader(mapping)));
            }

            CreateIndexResponse response = client.indices().create(builder.build());
            log.info("Create index [{}] result: {}", indexName, response.acknowledged());
            return response.acknowledged();
        } catch (Exception e) {
            log.error("Failed to create index [{}]", indexName, e);
            throw new InfrastructureException(RafResponseEnum.SERVER_ERROR, "创建索引失败: " + indexName, e);
        }
    }

    /**
     * 删除索引
     */
    public boolean deleteIndex(String indexName) {
        try {
            if (!existsIndex(indexName)) {
                log.warn("Index [{}] does not exist", indexName);
                return true;
            }

            DeleteIndexResponse response = client.indices().delete(d -> d.index(indexName));
            log.info("Delete index [{}] result: {}", indexName, response.acknowledged());
            return response.acknowledged();
        } catch (Exception e) {
            log.error("Failed to delete index [{}]", indexName, e);
            throw new InfrastructureException(RafResponseEnum.SERVER_ERROR, "删除索引失败: " + indexName, e);
        }
    }
    /**
     * 判断索引是否存在
     */
    public boolean existsIndex(String indexName) {
        try {
            return client.indices().exists(e -> e.index(indexName)).value();
        } catch (Exception e) {
            log.error("Failed to check index existence [{}]", indexName, e);
            throw new InfrastructureException(RafResponseEnum.SERVER_ERROR, "检查索引存在性失败: " + indexName, e);
        }
    }

    // ========== 文档操作 ==========

    /**
     * 保存文档（新增或更新）
     */
    public <T> String save(String indexName, String id, T document) {
        try {
            IndexResponse response = client.index(i -> i
                    .index(indexName)
                    .id(id)
                    .document(document)
            );
            log.debug("Save document to index [{}] with id [{}], result: {}", indexName, id, response.result());
            return response.id();
        } catch (Exception e) {
            log.error("Failed to save document to index [{}] with id [{}]", indexName, id, e);
            throw new InfrastructureException(RafResponseEnum.SERVER_ERROR, "保存文档失败: " + indexName, e);
        }
    }

    /**
     * 更新文档
     */
    public <T> void update(String indexName, String id, T document) {
        try {
            UpdateResponse<T> response = client.update(u -> u
                    .index(indexName)
                    .id(id)
                    .doc(document),
                    (Class<T>) document.getClass()
            );
            log.debug("Update document in index [{}] with id [{}], result: {}", indexName, id, response.result());
        } catch (Exception e) {
            log.error("Failed to update document in index [{}] with id [{}]", indexName, id, e);
            throw new InfrastructureException(RafResponseEnum.SERVER_ERROR, "更新文档失败: " + indexName, e);
        }
    }

    /**
     * 根据 ID 删除文档
     */
    public boolean delete(String indexName, String id) {
        try {
            DeleteResponse response = client.delete(d -> d
                    .index(indexName)
                    .id(id)
            );
            log.debug("Delete document from index [{}] with id [{}], result: {}", indexName, id, response.result());
            return response.result() == Result.Deleted;
        } catch (Exception e) {
            log.error("Failed to delete document from index [{}] with id [{}]", indexName, id, e);
            throw new InfrastructureException(RafResponseEnum.SERVER_ERROR, "删除文档失败: " + indexName, e);
        }
    }

    /**
     * 根据 ID 查询文档
     */
    public <T> T getById(String indexName, String id, Class<T> clazz) {
        try {
            GetResponse<T> response = client.get(g -> g
                    .index(indexName)
                    .id(id),
                    clazz
            );

            if (!response.found()) {
                return null;
            }

            return response.source();
        } catch (Exception e) {
            log.error("Failed to get document from index [{}] with id [{}]", indexName, id, e);
            throw new InfrastructureException(RafResponseEnum.SERVER_ERROR, "查询文档失败: " + indexName, e);
        }
    }

    /**
     * 批量保存文档
     */
    public <T> boolean bulkSave(String indexName, Map<String, T> documents) {
        try {
            List<BulkOperation> operations = new ArrayList<>();
            for (Map.Entry<String, T> entry : documents.entrySet()) {
                operations.add(BulkOperation.of(b -> b
                        .index(i -> i
                                .index(indexName)
                                .id(entry.getKey())
                                .document(entry.getValue())
                        )
                ));
            }

            BulkResponse response = client.bulk(b -> b.operations(operations));
            log.info("Bulk save {} documents to index [{}], hasFailures: {}",
                    documents.size(), indexName, response.errors());

            if (response.errors()) {
                log.error("Bulk save has failures");
            }

            return !response.errors();
        } catch (Exception e) {
            log.error("Failed to bulk save documents to index [{}]", indexName, e);
            throw new InfrastructureException(RafResponseEnum.SERVER_ERROR, "批量保存文档失败: " + indexName, e);
        }
    }

    /**
     * 批量删除文档
     */
    public boolean bulkDelete(String indexName, List<String> ids) {
        try {
            List<BulkOperation> operations = new ArrayList<>();
            for (String id : ids) {
                operations.add(BulkOperation.of(b -> b
                        .delete(d -> d
                                .index(indexName)
                                .id(id)
                        )
                ));
            }

            BulkResponse response = client.bulk(b -> b.operations(operations));
            log.info("Bulk delete {} documents from index [{}], hasFailures: {}",
                    ids.size(), indexName, response.errors());

            if (response.errors()) {
                log.error("Bulk delete has failures");
            }

            return !response.errors();
        } catch (Exception e) {
            log.error("Failed to bulk delete documents from index [{}]", indexName, e);
            throw new InfrastructureException(RafResponseEnum.SERVER_ERROR, "批量删除文档失败: " + indexName, e);
        }
    }

    // ========== 搜索操作 ==========

    /**
     * 搜索文档
     */
    public <T> List<T> search(String indexName, Query query, Class<T> clazz) {
        return search(indexName, query, properties.getDefaultSearchSize(), clazz);
    }

    /**
     * 搜索文档（指定大小）
     */
    public <T> List<T> search(String indexName, Query query, int size, Class<T> clazz) {
        try {
            if (size > properties.getMaxSearchSize()) {
                log.warn("Search size [{}] exceeds max limit [{}], using max limit", size, properties.getMaxSearchSize());
                size = properties.getMaxSearchSize();
            }

            int finalSize = size;
            SearchResponse<T> response = client.search(s -> s
                    .index(indexName)
                    .query(query)
                    .size(finalSize)
            , clazz);

            return response.hits().hits().stream()
                    .map(Hit::source)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to search in index [{}]", indexName, e);
            throw new InfrastructureException(RafResponseEnum.SERVER_ERROR, "搜索文档失败: " + indexName, e);
        }
    }

    /**
     * 分页搜索文档
     */
    public <T> EsPageResult<T> searchPage(String indexName, Query query, int page, int size, Class<T> clazz) {
        try {
            SearchResponse<T> response = client.search(s -> s
                    .index(indexName)
                    .query(query)
                    .from(page * size)
                    .size(size),
                    clazz
            );

            List<T> documents = response.hits().hits().stream()
                    .map(Hit::source)
                    .collect(Collectors.toList());

            long total = response.hits().total() != null ? response.hits().total().value() : 0;
            return new EsPageResult<>(total, documents);
        } catch (Exception e) {
            log.error("Failed to search page in index [{}]", indexName, e);
            throw new InfrastructureException(RafResponseEnum.SERVER_ERROR, "分页搜索失败: " + indexName, e);
        }
    }

    /**
     * 统计文档数量
     */
    public long count(String indexName, Query query) {
        try {
            CountResponse response = client.count(c -> c
                    .index(indexName)
                    .query(query)
            );
            return response.count();
        } catch (Exception e) {
            log.error("Failed to count in index [{}]", indexName, e);
            throw new InfrastructureException(RafResponseEnum.SERVER_ERROR, "统计文档数量失败: " + indexName, e);
        }
    }

    /**
     * 获取 ElasticsearchClient（用于高级定制）
     */
    public ElasticsearchClient getClient() {
        return client;
    }
}
