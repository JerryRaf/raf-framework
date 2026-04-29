package com.raf.framework.core.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests for BatchThreadHandlerUtil.
 *
 * @author Jerry
 * @since 2026-04-29
 */
class BatchThreadHandlerUtilTest {

    // ─── executeWithThreadPool(List, service, batchSize, Executor) ───────────

    @Test
    void executeWithExecutor_processesAllItems() {
        List<Integer> data = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        AtomicInteger processedCount = new AtomicInteger(0);

        BatchThreadHandlerUtil.executeWithThreadPool(
                data,
                batch -> {
                    processedCount.addAndGet(batch.size());
                    return true;
                },
                3,
                Executors.newFixedThreadPool(2)
        );

        Assertions.assertEquals(10, processedCount.get());
    }

    @Test
    void executeWithExecutor_doesNothingForEmptyList() {
        AtomicInteger callCount = new AtomicInteger(0);

        BatchThreadHandlerUtil.executeWithThreadPool(
                Collections.emptyList(),
                batch -> {
                    callCount.incrementAndGet();
                    return true;
                },
                10,
                Executors.newSingleThreadExecutor()
        );

        Assertions.assertEquals(0, callCount.get());
    }

    @Test
    void executeWithExecutor_doesNothingForNullList() {
        AtomicInteger callCount = new AtomicInteger(0);

        BatchThreadHandlerUtil.executeWithThreadPool(
                null,
                batch -> {
                    callCount.incrementAndGet();
                    return true;
                },
                10,
                Executors.newSingleThreadExecutor()
        );

        Assertions.assertEquals(0, callCount.get());
    }

    // ─── executeWithThreadPool(List, service, batchSize, threadCount) ────────

    @Test
    void executeWithThreadCount_processesAllItems() {
        List<String> data = Arrays.asList("a", "b", "c", "d", "e");
        AtomicInteger processedCount = new AtomicInteger(0);

        BatchThreadHandlerUtil.executeWithThreadPool(
                data,
                batch -> {
                    processedCount.addAndGet(batch.size());
                    return true;
                },
                2,
                2
        );

        Assertions.assertEquals(5, processedCount.get());
    }

    @Test
    void executeWithThreadCount_doesNothingForEmptyList() {
        AtomicInteger callCount = new AtomicInteger(0);

        BatchThreadHandlerUtil.executeWithThreadPool(
                Collections.emptyList(),
                batch -> {
                    callCount.incrementAndGet();
                    return true;
                },
                10,
                2
        );

        Assertions.assertEquals(0, callCount.get());
    }

    @Test
    void executeWithThreadCount_singleBatchWhenDataFitsInOneBatch() {
        List<Integer> data = Arrays.asList(1, 2, 3);
        AtomicInteger batchCallCount = new AtomicInteger(0);

        BatchThreadHandlerUtil.executeWithThreadPool(
                data,
                batch -> {
                    batchCallCount.incrementAndGet();
                    return true;
                },
                10,
                2
        );

        Assertions.assertEquals(1, batchCallCount.get());
    }
}
