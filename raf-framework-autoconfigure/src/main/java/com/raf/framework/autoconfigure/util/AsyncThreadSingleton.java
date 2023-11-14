package com.raf.framework.autoconfigure.util;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.raf.framework.autoconfigure.common.RafConstant;

import java.util.concurrent.*;

/**
 * @author Jerry
 * @date 2019/01/01
 */
public class AsyncThreadSingleton {
    static class AsyncThreadUtilHolder {
        static AsyncThreadSingleton instance = new AsyncThreadSingleton();
    }

    public static AsyncThreadSingleton getInstance() {
        return AsyncThreadUtilHolder.instance;
    }

    private ThreadPoolExecutor executor;

    private AsyncThreadSingleton() {
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat(RafConstant.ASYNC_CUST_POOL).build();
        this.executor = new ThreadPoolExecutor(4,
                16, 5, TimeUnit.SECONDS,
                new LinkedBlockingQueue(100), namedThreadFactory);
    }


    public Future submit(Callable able) {
        return this.executor.submit(able);
    }

    public void execute(Runnable runnable) {
        this.executor.execute(runnable);
    }
}
