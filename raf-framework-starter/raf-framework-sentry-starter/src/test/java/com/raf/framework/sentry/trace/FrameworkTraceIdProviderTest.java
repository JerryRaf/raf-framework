package com.raf.framework.sentry.trace;

import com.raf.framework.core.trace.ContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for FrameworkTraceIdProvider.
 *
 * @author Jerry
 * @since 2026-04-27
 */
class FrameworkTraceIdProviderTest {

    private final FrameworkTraceIdProvider provider = new FrameworkTraceIdProvider();

    @AfterEach
    void tearDown() {
        ContextHolder.clearContext();
    }

    @Test
    void shouldReturnTraceIdFromContextHolder() {
        ContextHolder.setTraceId("framework-trace-id");

        String traceId = provider.getTraceId();

        Assertions.assertEquals("framework-trace-id", traceId);
        Assertions.assertEquals("framework", provider.getProviderName());
    }

    @Test
    void shouldReturnNullWhenTraceIdMissing() {
        String traceId = provider.getTraceId();

        Assertions.assertNull(traceId);
    }
}
