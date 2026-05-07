package com.raf.framework.sentry.trace;

import io.sentry.Sentry;
import io.sentry.protocol.SentryId;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;

/**
 * APM 平台 TraceId 提供者
 * 优先从 APM 平台（SkyWalking、Zipkin、Jaeger、Elastic APM 等）获取 traceId
 * 支持多种 APM 平台的 MDC key
 *
 * @author Jerry
 * @date 2026/04/20
 */
public class ApmTraceIdProvider implements TraceIdProvider {

    /**
     * 常见 APM 平台的 MDC traceId key
     */
    private static final String[] TRACE_ID_KEYS = {
            "traceId",           // 通用
            "X-B3-TraceId",      // Zipkin B3
            "tid",               // SkyWalking
            "trace.id",          // Elastic APM
            "trace_id",          // Jaeger
            "dd.trace_id"        // Datadog
    };

    @Override
    public String getTraceId() {
        // 1. 尝试从 MDC 获取 APM 平台的 traceId
        for (String key : TRACE_ID_KEYS) {
            String traceId = MDC.get(key);
            if (StringUtils.isNotBlank(traceId)) {
                return traceId;
            }
        }

        // 2. 尝试从 Sentry 自身获取（如果 Sentry 已经生成了 traceId）
        SentryId sentryTraceId = Sentry.getSpan() != null
                ? Sentry.getSpan().getSpanContext().getTraceId()
                : null;

        if (sentryTraceId != null) {
            return sentryTraceId.toString();
        }

        // 3. 都没有则返回 null
        return null;
    }

    @Override
    public String getProviderName() {
        return "apm";
    }
}
