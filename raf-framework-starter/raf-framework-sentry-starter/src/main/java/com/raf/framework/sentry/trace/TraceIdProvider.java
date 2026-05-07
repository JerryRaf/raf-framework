package com.raf.framework.sentry.trace;

/**
 * TraceId 提供者接口
 * 支持从不同来源获取 traceId（框架自定义或 APM 平台）
 *
 * @author Jerry
 * @date 2026/04/20
 */
public interface TraceIdProvider {

    /**
     * 获取当前请求的 traceId
     *
     * @return traceId，如果不存在返回 null
     */
    String getTraceId();

    /**
     * 获取提供者名称（用于日志和调试）
     *
     * @return 提供者名称
     */
    String getProviderName();
}
