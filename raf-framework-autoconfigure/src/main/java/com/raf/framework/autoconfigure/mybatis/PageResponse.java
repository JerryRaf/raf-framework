package com.raf.framework.autoconfigure.mybatis;

import com.github.pagehelper.Page;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 通用分页输出
 * @author Jerry
 * @date 2019/01/01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    /**
     * 第几页
     */
    private int pageNum;

    /**
     * 每页记录数
     */
    private int pageSize;

    /**
     * 总页数
     */
    private int pages;

    /**
     * 当前页的数量 <= pageSize，该属性来自ArrayList的size属性
     */
    private int size;

    /**
     * 总记录数
     */
    private int total;

    /**
     * 分页结果集合
     */
    private List<T> list;

    public PageResponse(List<T> list) {
        if (list instanceof Page) {
            Page<T> page = (Page<T>) list;
            this.pageNum = page.getPageNum();
            this.pageSize = page.getPageSize();
            this.total = (int)page.getTotal();
            this.pages = page.getPages();
            this.list = page;
            this.size = page.size();
        }
    }
}
