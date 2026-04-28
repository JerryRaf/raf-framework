package com.raf.framework.core.trace;

import com.raf.framework.core.common.RafConstant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

/**
 * Tests for ContextHolder.
 *
 * @author Jerry
 * @since 2026-04-27
 */
class ContextHolderTest {

    @AfterEach
    void tearDown() {
        ContextHolder.clearContext();
    }

    @Test
    void shouldGenerateAndStoreTraceIdWhenAbsent() {
        String traceId = ContextHolder.getOrSetTraceId();

        Assertions.assertNotNull(traceId);
        Assertions.assertFalse(traceId.isEmpty());
        Assertions.assertEquals(traceId, ContextHolder.getTraceId());
        Assertions.assertEquals(traceId, MDC.get(RafConstant.TRACE_ID));
    }

    @Test
    void shouldUseProvidedTraceId() {
        String traceId = "trace-id-from-upstream";

        ContextHolder.setTraceId(traceId);

        Assertions.assertEquals(traceId, ContextHolder.getTraceId());
        Assertions.assertEquals(traceId, MDC.get(RafConstant.TRACE_ID));
    }

    @Test
    void shouldClearContextAndMdc() {
        ContextHolder.setTraceId("trace-id-to-clear");
        ContextHolder.setTenantId("tenant-a");

        ContextHolder.clearContext();

        Assertions.assertNull(ContextHolder.getTraceId());
        Assertions.assertNull(ContextHolder.getTenantId());
        Assertions.assertNull(MDC.get(RafConstant.TRACE_ID));
    }
}
