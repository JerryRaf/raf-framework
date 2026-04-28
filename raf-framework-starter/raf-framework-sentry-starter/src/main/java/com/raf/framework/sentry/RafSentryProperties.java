package com.raf.framework.sentry;

import java.util.HashSet;
import java.util.Set;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Sentry 配置属性
 *
 * @author Jerry
 * @date 2026/04/20
 */
@Data
@ConfigurationProperties(prefix = "raf.sentry")
public class RafSentryProperties {

    /**
     * 是否启用 Sentry
     */
    private Boolean enabled = false;

    /**
     * Sentry DSN（必填）
     */
    private String dsn;

    /**
     * TraceId 来源
     * - framework: 使用框架的 ContextHolder.getTraceId()
     * - apm: 使用 APM 平台的 traceId（SkyWalking、Zipkin、Jaeger、Elastic APM 等）
     */
    private TraceIdSource traceIdSource = TraceIdSource.FRAMEWORK;

    /**
     * 异常过滤策略
     */
    private ExceptionFilter exceptionFilter = new ExceptionFilter();

    /**
     * 是否启用性能监控
     */
    private Boolean enablePerformance = false;

    /**
     * 错误采样率（0.0 - 1.0）
     * 1.0 表示 100% 采样，0.0 表示不采样
     */
    private Double sampleRate = 1.0;

    /**
     * 性能追踪采样率（0.0 - 1.0）
     * 仅在 enablePerformance=true 时生效
     */
    private Double tracesSampleRate = 0.1;

    /**
     * 环境标识（默认从 spring.profiles.active 获取）
     */
    private String environment;

    /**
     * 发布版本（默认从 spring.application.name 获取）
     */
    private String release;

    /**
     * 服务器名称（默认从 spring.application.name 获取）
     */
    private String serverName;

    /**
     * 是否在控制台打印 Sentry 日志（调试用）
     */
    private Boolean debug = false;

    /**
     * 是否发送默认的 PII（个人身份信息）
     */
    private Boolean sendDefaultPii = false;

    /**
     * 附加标签
     */
    private java.util.Map<String, String> tags;

    /**
     * TraceId 来源枚举
     */
    public enum TraceIdSource {
        /**
         * 使用框架的 ContextHolder.getTraceId()
         */
        FRAMEWORK,

        /**
         * 使用 APM 平台的 traceId
         */
        APM
    }

    /**
     * 异常过滤配置
     */
    @Data
    public static class ExceptionFilter {

        /**
         * 是否启用异常过滤
         */
        private Boolean enabled = true;

        /**
         * 需要上报的异常类型（全限定类名）
         * 默认只上报 SystemException 和 InfrastructureException
         */
        private Set<String> includeExceptions = new HashSet<>(Set.of(
                "com.raf.framework.autoconfigure.common.exception.SystemException",
                "com.raf.framework.autoconfigure.common.exception.InfrastructureException"
        ));

        /**
         * 需要排除的异常类型（全限定类名）
         * 优先级高于 includeExceptions
         */
        private Set<String> excludeExceptions = new HashSet<>();

        /**
         * 是否上报 BusinessException（业务异常）
         * 默认 false，因为业务异常通常不需要告警
         */
        private Boolean reportBusinessException = false;
    }
}
