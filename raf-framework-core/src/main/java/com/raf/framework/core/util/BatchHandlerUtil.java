package com.raf.framework.core.util;

import java.util.List;
import com.github.pagehelper.ISelect;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

/**
 * 批处理工具类
 * <p>
 * 用于分批处理大量数据，避免一次性加载过多数据导致内存溢出
 * 使用 PageHelper 进行分页查询，支持自定义批次大小
 * </p>
 * <p>
 * 使用示例：
 * <pre>
 * BatchHandlerUtil.execute(
 *     () -> xxDao.queryList("condition"),
 *     models -> models.forEach(model -> log.info("Processing: {}", model)),
 *     1000
 * );
 * </pre>
 * </p>
 *
 * @author Jerry
 * @date 2020/05/07
 */
@Slf4j
public class BatchHandlerUtil {

    /**
     * 私有构造器,防止实例化工具类
     */
    private BatchHandlerUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 批处理执行方法
     *
     * @param select     查询方法（使用 Lambda 表达式）
     * @param service    每批数据的处理逻辑
     * @param batchLimit 每批处理的数据量
     * @param <T>        数据类型
     */
    public static <T> void execute(ISelect select, BatchHandlerService<T> service, int batchLimit) {
        if (batchLimit <= 0) {
            throw new IllegalArgumentException("batchLimit must be greater than 0");
        }

        long totalCount = PageHelper.count(select);
        log.info("Total count: {}", totalCount);

        if (totalCount == 0) {
            log.info("No data to process");
            return;
        }

        // 计算总页数，避免使用 count 递减的方式
        int totalPages = (int) Math.ceil((double) totalCount / batchLimit);

        for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
            log.info("Processing page {}/{}", pageNum, totalPages);

            final Page<T> page = PageHelper.startPage(pageNum, batchLimit).doSelectPage(select);

            if (page == null || CollectionUtils.isEmpty(page.getResult())) {
                log.warn("Page {} returned empty result, stopping", pageNum);
                break;
            }

            try {
                service.execute(page.getResult());
                log.debug("Page {} processed successfully, {} records", pageNum, page.getResult().size());
            } catch (Exception e) {
                log.error("Error processing page {}: {}", pageNum, e.getMessage(), e);
                throw e; // 根据业务需要，可以选择继续或中断
            }
        }

        log.info("Batch processing completed. Total {} pages processed", totalPages);
    }

    /**
     * 批处理服务接口
     *
     * @param <T> 数据类型
     */
    @FunctionalInterface
    public interface BatchHandlerService<T> {
        /**
         * 执行批处理逻辑
         *
         * @param models 当前批次的数据列表
         */
        void execute(List<T> models);
    }
}
