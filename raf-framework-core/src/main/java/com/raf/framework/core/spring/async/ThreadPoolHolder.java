package com.raf.framework.core.spring.async;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.StringUtils;

/**
 * Thread pool holder utility.
 *
 * @author Jerry
 * @since 2019-01-01
 */
public class ThreadPoolHolder {

    /**
     * 私有构造器,防止实例化工具类
     */
    private ThreadPoolHolder() {
        throw new UnsupportedOperationException("Utility class");
    }

    private static final Map<String, ThreadPoolTaskExecutor> CONTAINER = new ConcurrentHashMap<>();

    public static void register(String threadPoolName, ThreadPoolTaskExecutor executorService) {
        Objects.requireNonNull(executorService, "executorService must not be null");
        CONTAINER.put(
                StringUtils.hasText(threadPoolName) ? threadPoolName : UUID.randomUUID().toString(),
                executorService);
    }

    public static ThreadPoolTaskExecutor getInstance(String threadPoolName) {
        return CONTAINER.get(threadPoolName);
    }

    public static Map<String, ThreadPoolTaskExecutor> threadPools() {
        return CONTAINER;
    }
}
