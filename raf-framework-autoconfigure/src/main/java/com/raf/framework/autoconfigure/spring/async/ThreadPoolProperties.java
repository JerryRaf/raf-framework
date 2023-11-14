package com.raf.framework.autoconfigure.spring.async;

import com.raf.framework.autoconfigure.common.RafConstant;
import lombok.Data;

/**
 * @author Jerry
 * @date 2019/01/01
 */
@Data
public class ThreadPoolProperties {
    private boolean enabled = false;
    private int corePoolSize = 10;
    private int queueCapacity = 100;
    private int maxPoolSize = 120;
    private int keepAliveSeconds = 5;
    private String threadNamePrefix = RafConstant.ASYNC_POOL;
    private boolean metricEnabled = false;
}
