package com.raf.framework.elasticsearch;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Elasticsearch 分页结果
 *
 * @param <T> 文档类型
 * @author RAF Framework
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EsPageResult<T> {

    /**
     * 总记录数
     */
    private long total;

    /**
     * 文档列表
     */
    private List<T> records;
}
