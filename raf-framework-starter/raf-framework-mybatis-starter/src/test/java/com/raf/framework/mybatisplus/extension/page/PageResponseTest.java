package com.raf.framework.mybatisplus.extension.page;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for PageResponse
 *
 * @author Jerry
 * @since 2026-04-20
 */
class PageResponseTest {

    @Test
    void testCreateFromPage() {
        Page<String> page = new Page<>(2, 10);
        page.setTotal(25);
        page.setRecords(Arrays.asList("item1", "item2", "item3"));

        PageResponse<String> response = new PageResponse<>(page);

        assertEquals(2, response.getPageNum());
        assertEquals(10, response.getPageSize());
        assertEquals(25, response.getTotal());
        assertEquals(3, response.getPages());
        assertEquals(3, response.getRecords().size());
        assertEquals("item1", response.getRecords().get(0));
    }

    @Test
    void testEmptyPage() {
        Page<String> page = new Page<>(1, 10);
        page.setTotal(0);
        page.setRecords(Arrays.asList());

        PageResponse<String> response = new PageResponse<>(page);

        assertEquals(1, response.getPageNum());
        assertEquals(10, response.getPageSize());
        assertEquals(0, response.getTotal());
        assertEquals(0, response.getPages());
        assertTrue(response.getRecords().isEmpty());
    }

    @Test
    void testCalculatePages() {
        Page<String> page = new Page<>(1, 10);
        page.setTotal(95);
        page.setRecords(Arrays.asList());

        PageResponse<String> response = new PageResponse<>(page);

        assertEquals(10, response.getPages());
    }
}
