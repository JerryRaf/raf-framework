package com.raf.framework.web.async;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 异步线程池配置属性
 * 对应配置文件前缀: raf.async
 *
 * @author Jerry
 */
@Data
@ConfigurationProperties(prefix = "raf.async")
public class RafAsyncProperties {

    /**
     * 是否开启异步线程池，默认 false
     */
    private boolean enabled = false;

    /**
     * 默认全局配置（如果某个池子没配具体参数，可以用这个兜底，这里简化处理，仅做多池配置）
     */

    /**
     * 多线程池配置容器
     * Key: 线程池名称 (例如: order, pay, report)
     * Value: 具体配置
     */
    private Map<String, PoolConfig> pools = new HashMap<>();

    /**
     * 内部静态类：单个线程池的配置参数
     */
    @Data
    public static class PoolConfig {
        private int coreSize = 10;
        private int maxSize = 50;
        private int queueCapacity = 100;
        private int keepAliveSeconds = 60;
        private String threadNamePrefix = "raf-async-";
    }
}