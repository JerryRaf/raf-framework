package com.raf.framework.core.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

/**
 * Parallel batch processing utility.
 * Supports both managed executor injection and ad-hoc thread pool creation.
 *
 * @author Jerry
 * @since 2019-01-01
 */
@Slf4j
public class BatchThreadHandlerUtil {

    private BatchThreadHandlerUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Execute batch tasks using a provided executor.
     * Preferred overload — reuses a managed thread pool instead of creating a new one.
     *
     * @param dataList  data to process
     * @param service   batch handler
     * @param batchSize items per batch
     * @param executor  managed executor to submit tasks to
     * @param <T>       element type
     */
    public static <T> void executeWithThreadPool(List<T> dataList, BatchThreadHandlerService<T> service,
                                                 int batchSize, Executor executor) {
        if (CollectionUtils.isEmpty(dataList)) {
            return;
        }

        int count = dataList.size();
        log.info("Batch processing started, total={}", count);

        ExecutorService executorService = (executor instanceof ExecutorService)
                ? (ExecutorService) executor
                : null;

        List<Future<?>> futures = new ArrayList<>();
        int totalBatches = (int) Math.ceil((double) count / batchSize);

        for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
            final int startIndex = batchIndex * batchSize;
            final int endIndex = Math.min(startIndex + batchSize, count);
            List<T> sublist = dataList.subList(startIndex, endIndex);

            if (executorService != null) {
                futures.add(executorService.submit(() -> service.execute(sublist)));
            } else {
                // Fallback: wrap Executor as CompletableFuture
                futures.add(CompletableFuture.runAsync(() -> service.execute(sublist), executor));
            }
        }

        awaitAll(futures);
        log.info("Batch processing completed, total={}", count);
    }

    /**
     * Execute batch tasks using an ad-hoc fixed thread pool.
     * Use only when no managed executor is available.
     *
     * @param dataList    data to process
     * @param service     batch handler
     * @param batchSize   items per batch
     * @param threadCount thread pool size
     * @param <T>         element type
     */
    public static <T> void executeWithThreadPool(List<T> dataList, BatchThreadHandlerService<T> service,
                                                 int batchSize, int threadCount) {
        if (CollectionUtils.isEmpty(dataList)) {
            return;
        }

        ExecutorService executor = Executors.newFixedThreadPool(threadCount,
                r -> new Thread(r, "batch-handler-" + r.hashCode()));
        try {
            executeWithThreadPool(dataList, service, batchSize, executor);
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    log.warn("Batch thread pool did not terminate within 60s, forcing shutdown");
                    executor.shutdownNow();
                    if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                        log.error("Batch thread pool failed to terminate");
                    }
                }
            } catch (InterruptedException e) {
                log.error("Interrupted while waiting for batch thread pool termination", e);
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private static void awaitAll(List<Future<?>> futures) {
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException e) {
                log.error("Interrupted while waiting for batch task", e);
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                log.error("Batch task execution failed", e);
            }
        }
    }

    /**
     * Batch handler service interface.
     *
     * @param <T> element type
     */
    public interface BatchThreadHandlerService<T> {
        /**
         * Process a batch of items.
         *
         * @param models batch of items
         * @return true if successful
         */
        Boolean execute(List<T> models);
    }
}
