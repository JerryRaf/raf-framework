package com.raf.framework.autoconfigure.util;

import com.github.pagehelper.ISelect;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Slf4j
public class BatchHandlerUtil {
    /**
     * 批处理工具
     *
     * @param select     查询
     * @param service    查询结果具体如何执行
     * @param batchLimit 每次处理多少
     * @param <T>
     */
    public static <T> void execute(ISelect select, BatchHandlerService<T> service, int batchLimit) {
        long count = PageHelper.count(select);
        log.info("all count:{}", count);

        int index = 1;
        while (count > 0) {
            log.info("start process page:{}", index);
            final Page<T> page = PageHelper.startPage(index, batchLimit).doSelectPage(select);
            if (page == null || CollectionUtils.isEmpty(page.getResult())) {
                return;
            }

            service.execute(page.getResult());
            count = count - batchLimit;
            index++;
        }
    }

    public interface BatchHandlerService<T> {
        void execute(List<T> models);
    }

}
