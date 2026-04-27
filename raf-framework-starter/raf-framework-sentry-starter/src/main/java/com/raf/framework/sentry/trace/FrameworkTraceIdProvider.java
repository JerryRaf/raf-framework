package com.raf.framework.sentry.trace;

import com.raf.framework.core.trace.ContextHolder;
import org.apache.commons.lang3.StringUtils;

/**
 * 框架自定义 TraceId 提供者
 * 从 ContextHolder 获取 traceId
 *
 * @author Jerry
 * @date 2026/04/20
 */
public class FrameworkTraceIdProvider implements TraceIdProvider {

    @Override
    public String getTraceId() {
        String traceId = ContextHolder.getTraceId();
        return StringUtils.isNotBlank(traceId) ? traceId : null;
    }

    @Override
    public String getProviderName() {
        return "framework";
    }
}
