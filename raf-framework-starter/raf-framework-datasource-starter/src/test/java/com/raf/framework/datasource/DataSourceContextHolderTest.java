package com.raf.framework.datasource;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

/**
 * DataSourceContextHolder Test
 *
 * @author Jerry
 * @since 2026-04-20
 */
class DataSourceContextHolderTest {

    @AfterEach
    void tearDown() {
        DataSourceContextHolder.clearDB();
    }

    @Test
    void testSetAndGetDB() {
        DataSourceContextHolder.setDB("slave");
        assertEquals("slave", DataSourceContextHolder.getDB());
    }

    @Test
    void testGetDBWhenNotSet() {
        assertNull(DataSourceContextHolder.getDB());
    }

    @Test
    void testClearDB() {
        DataSourceContextHolder.setDB("slave");
        DataSourceContextHolder.clearDB();
        assertNull(DataSourceContextHolder.getDB());
    }

    @Test
    void testThreadLocalIsolation() throws InterruptedException {
        DataSourceContextHolder.setDB("master");

        Thread thread = new Thread(() -> {
            DataSourceContextHolder.setDB("slave");
            assertEquals("slave", DataSourceContextHolder.getDB());
        });

        thread.start();
        thread.join();

        assertEquals("master", DataSourceContextHolder.getDB());
    }
}
