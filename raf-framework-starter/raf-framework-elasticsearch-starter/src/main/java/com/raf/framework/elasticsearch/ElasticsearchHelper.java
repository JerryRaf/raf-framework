package com.raf.framework.elasticsearch;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;

/**
 * Elasticsearch 辅助工具类
 * 提供常用查询构建方法（基于 Elasticsearch 8.x Java API Client）
 *
 * @author RAF Framework
 */
public class ElasticsearchHelper {

    /**
     * 私有构造器，防止实例化工具类
     */
    private ElasticsearchHelper() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 构建布尔查询
     */
    public static Query boolQuery(BoolQuery.Builder builder) {
        return Query.of(q -> q.bool(builder.build()));
    }

    /**
     * 构建精确匹配查询
     */
    public static Query termQuery(String field, String value) {
        return Query.of(q -> q.term(t -> t.field(field).value(value)));
    }

    /**
     * 构建精确匹配查询（数值）
     */
    public static Query termQuery(String field, long value) {
        return Query.of(q -> q.term(t -> t.field(field).value(value)));
    }

    /**
     * 构建多值精确匹配查询
     */
    public static Query termsQuery(String field, String... values) {
        List<FieldValue> fieldValues = Arrays.stream(values)
                .map(FieldValue::of)
                .collect(Collectors.toList());
        return Query.of(q -> q.terms(t -> t.field(field).terms(tv -> tv.value(fieldValues))));
    }

    /**
     * 构建全文检索查询
     */
    public static Query matchQuery(String field, String value) {
        return Query.of(q -> q.match(m -> m.field(field).query(value)));
    }

    /**
     * 构建多字段全文检索查询
     */
    public static Query multiMatchQuery(String value, String... fields) {
        return Query.of(q -> q.multiMatch(m -> m.query(value).fields(Arrays.asList(fields))));
    }

    /**
     * 构建前缀查询
     */
    public static Query prefixQuery(String field, String prefix) {
        return Query.of(q -> q.prefix(p -> p.field(field).value(prefix)));
    }

    /**
     * 构建通配符查询
     */
    public static Query wildcardQuery(String field, String value) {
        return Query.of(q -> q.wildcard(w -> w.field(field).value(value)));
    }

    /**
     * 构建模糊查询
     */
    public static Query fuzzyQuery(String field, String value) {
        return Query.of(q -> q.fuzzy(f -> f.field(field).value(value)));
    }

    /**
     * 构建存在查询（字段存在）
     */
    public static Query existsQuery(String field) {
        return Query.of(q -> q.exists(e -> e.field(field)));
    }

    /**
     * 构建查询所有
     */
    public static Query matchAllQuery() {
        return Query.of(q -> q.matchAll(m -> m));
    }

    /**
     * 构建嵌套查询
     */
    public static Query nestedQuery(String path, Query query) {
        return Query.of(q -> q.nested(n -> n.path(path).query(query)));
    }
}
