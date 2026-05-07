package com.raf.framework.elasticsearch;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Elasticsearch 高亮文档
 *
 * @param <T> 文档类型
 * @author RAF Framework
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EsHighlightDocument<T> {

    /**
     * 原始文档
     */
    private T document;

    /**
     * 高亮内容 (字段名 -> 高亮内容)
     */
    private Map<String, String> highlights;
}
