package com.raf.framework.autoconfigure.spring.async;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.UUID;

/**
 * @author Jerry
 * @date 2019/01/01
 */
public class ThreadPoolHolder {

    private static final Map<String, ThreadPoolTaskExecutor> CONTAINER = Maps.newConcurrentMap();

    public static void register(String threadPoolName, ThreadPoolTaskExecutor executorService) {
        Preconditions.checkNotNull(executorService);
        CONTAINER.put(StringUtils.defaultString(threadPoolName,UUID.randomUUID().toString()), executorService);
    }

    public static ThreadPoolTaskExecutor getInstance(String threadPoolName) {
        return CONTAINER.get(threadPoolName);
    }

    public static Map<String, ThreadPoolTaskExecutor> threadPools() {
        return CONTAINER;
    }
}
