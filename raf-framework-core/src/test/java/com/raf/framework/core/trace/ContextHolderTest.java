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

    @Test
    void shouldReturnExistingTraceIdWhenAlreadySet() {
        ContextHolder.setTraceId("existing-trace");

        String traceId = ContextHolder.getOrSetTraceId();

        Assertions.assertEquals("existing-trace", traceId);
    }

    @Test
    void shouldIgnoreNullOrEmptyTraceId() {
        ContextHolder.setTraceId(null);
        Assertions.assertNull(ContextHolder.getTraceId());

        ContextHolder.setTraceId("");
        Assertions.assertNull(ContextHolder.getTraceId());
    }

    @Test
    void shouldSetAndGetTenantId() {
        ContextHolder.setTenantId("tenant-xyz");

        Assertions.assertEquals("tenant-xyz", ContextHolder.getTenantId());
    }

    @Test
    void shouldIgnoreNullOrEmptyTenantId() {
        ContextHolder.setTenantId("tenant-a");
        ContextHolder.setTenantId(null);
        // null should not overwrite existing value
        Assertions.assertEquals("tenant-a", ContextHolder.getTenantId());

        ContextHolder.setTenantId("");
        Assertions.assertEquals("tenant-a", ContextHolder.getTenantId());
    }

    @Test
    void clearTraceId_removesOnlyTraceId() {
        ContextHolder.setTraceId("trace-to-remove");
        ContextHolder.setTenantId("tenant-keep");

        ContextHolder.clearTraceId();

        Assertions.assertNull(ContextHolder.getTraceId());
        Assertions.assertNull(MDC.get(RafConstant.TRACE_ID));
        Assertions.assertEquals("tenant-keep", ContextHolder.getTenantId());
    }

    @Test
    void setTraceId_generatesNewIdWhenCalledWithoutArg() {
        String traceId = ContextHolder.setTraceId();

        Assertions.assertNotNull(traceId);
        Assertions.assertFalse(traceId.isEmpty());
        Assertions.assertEquals(traceId, ContextHolder.getTraceId());
        Assertions.assertEquals(traceId, MDC.get(RafConstant.TRACE_ID));
    }
}
