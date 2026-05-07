package io.github.jerryraf.examples.monitor.controller;

import com.raf.framework.autoconfigure.common.annotation.ResponseResult;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 监控指标演示接口
 */
@RestController
@RequestMapping("/api/monitor")
@ResponseResult
@Slf4j
public class MonitorDemoController {

    private final MeterRegistry meterRegistry;
    private final Counter orderCounter;
    private final TaskExecutor orderExecutor;

    public MonitorDemoController(
        MeterRegistry meterRegistry,
        @Qualifier("orderThreadPoolTaskExecutor") TaskExecutor orderExecutor
    ) {
        this.meterRegistry = meterRegistry;
        this.orderExecutor = orderExecutor;
        // 自定义业务计数器
        this.orderCounter = Counter.builder("business.order.created")
            .description("订单创建总数")
            .tag("service", "order")
            .register(meterRegistry);
    }

    /**
     * 模拟创建订单，触发业务指标计数
     */
    @PostMapping("/orders")
    @Timed(value = "business.order.create.time", description = "订单创建耗时")
    public Map<String, Object> createOrder(@RequestBody Map<String, Object> req) {
        // 业务计数器 +1
        orderCounter.increment();

        // 提交到异步线程池
        orderExecutor.execute(() -> {
            log.info("异步处理订单，orderId={}", req.get("orderId"));
            // 模拟处理耗时
            try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", req.get("orderId"));
        result.put("status", "PROCESSING");
        return result;
    }

    /**
     * 查看线程池当前状态
     */
    @GetMapping("/thread-pools")
    public Map<String, Object> getThreadPoolStats() {
        Map<String, Object> stats = new HashMap<>();
        if (orderExecutor instanceof org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor taskExecutor) {
            ThreadPoolExecutor pool = taskExecutor.getThreadPoolExecutor();
            stats.put("poolName", "order");
            stats.put("corePoolSize", pool.getCorePoolSize());
            stats.put("maximumPoolSize", pool.getMaximumPoolSize());
            stats.put("activeCount", pool.getActiveCount());
            stats.put("queueSize", pool.getQueue().size());
            stats.put("completedTaskCount", pool.getCompletedTaskCount());
        }
        return stats;
    }

    /**
     * 查看 Prometheus 指标端点说明
     */
    @GetMapping("/endpoints")
    public Map<String, String> getEndpoints() {
        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("prometheus", "GET /actuator/prometheus  — Prometheus 抓取端点");
        endpoints.put("health",     "GET /actuator/health      — 健康检查");
        endpoints.put("metrics",    "GET /actuator/metrics     — 指标列表");
        endpoints.put("info",       "GET /actuator/info        — 应用信息");
        return endpoints;
    }
}
