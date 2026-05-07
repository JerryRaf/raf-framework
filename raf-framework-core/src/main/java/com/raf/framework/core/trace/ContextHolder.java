package com.raf.framework.core.trace;

import com.raf.framework.core.common.RafConstant;

import java.util.HashMap;
import java.util.Map;
import cn.hutool.core.util.IdUtil;
import com.alibaba.ttl.TransmittableThreadLocal;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;

/**
 * Thread-safe request context holder using TransmittableThreadLocal.
 * Propagates context across thread boundaries (async tasks, thread pools).
 *
 * @author Jerry
 * @since 2019-01-01
 */
public final class ContextHolder {

    private static final ThreadLocal<Map<String, String>> CONTEXT = new TransmittableThreadLocal<>() {
        @Override
        protected Map<String, String> initialValue() {
            return new HashMap<>(4);
        }
    };

    private ContextHolder() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Get the current traceId, generating and storing a new one if absent.
     *
     * @return traceId
     */
    public static String getOrSetTraceId() {
        String traceId = getTraceId();
        if (StringUtils.isNotEmpty(traceId)) {
            return traceId;
        }
        return setTraceId();
    }

    /**
     * Generate a new traceId and store it in context and MDC.
     *
     * @return generated traceId
     */
    public static String setTraceId() {
        String traceId = IdUtil.fastSimpleUUID();
        MDC.put(RafConstant.TRACE_ID, traceId);
        CONTEXT.get().put(RafConstant.TRACE_ID, traceId);
        return traceId;
    }

    /**
     * Set an externally provided traceId (e.g. from upstream service).
     *
     * @param traceId traceId from upstream
     */
    public static void setTraceId(String traceId) {
        if (traceId != null && !traceId.isEmpty()) {
            MDC.put(RafConstant.TRACE_ID, traceId);
            CONTEXT.get().put(RafConstant.TRACE_ID, traceId);
        }
    }

    /**
     * Get the current traceId.
     *
     * @return traceId, or null if not set
     */
    public static String getTraceId() {
        return CONTEXT.get().get(RafConstant.TRACE_ID);
    }

    /**
     * Set the tenant ID.
     *
     * @param tenantId tenant identifier
     */
    public static void setTenantId(String tenantId) {
        if (StringUtils.isNotEmpty(tenantId)) {
            CONTEXT.get().put(RafConstant.TENANT_ID, tenantId);
        }
    }

    /**
     * Get the current tenant ID.
     *
     * @return tenantId, or null if not set
     */
    public static String getTenantId() {
        return CONTEXT.get().get(RafConstant.TENANT_ID);
    }

    /**
     * Remove only the traceId from context and MDC.
     * Prefer {@link #clearContext()} at request boundaries.
     */
    public static void clearTraceId() {
        MDC.remove(RafConstant.TRACE_ID);
        CONTEXT.get().remove(RafConstant.TRACE_ID);
    }

    /**
     * Clear all context values and MDC entries.
     * Must be called at the end of each request or async task to prevent thread-local leaks.
     */
    public static void clearContext() {
        MDC.clear();
        CONTEXT.remove();
    }
}
