package com.raf.framework.mybatisplus.extension.page;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Page response result wrapping MyBatis-Plus Page
 *
 * @author Jerry
 * @since 2026-04-20
 */
@Data
@NoArgsConstructor
public class PageResponse<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Current page number
     */
    private long pageNum;

    /**
     * Page size
     */
    private long pageSize;

    /**
     * Total records count
     */
    private long total;

    /**
     * Total pages count
     */
    private long pages;

    /**
     * Data records
     */
    private List<T> records;

    /**
     * Create PageResponse from MyBatis-Plus Page object
     *
     * @param page MyBatis-Plus Page object
     */
    public PageResponse(Page<T> page) {
        this.pageNum = page.getCurrent();
        this.pageSize = page.getSize();
        this.total = page.getTotal();
        this.pages = page.getPages();
        this.records = page.getRecords();
    }
}
