package com.raf.framework.sentry.trace;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

/**
 * Tests for ApmTraceIdProvider.
 *
 * @author Jerry
 * @since 2026-04-27
 */
class ApmTraceIdProviderTest {

    private final ApmTraceIdProvider provider = new ApmTraceIdProvider();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void shouldReturnTraceIdFromMdcPreferredKeys() {
        MDC.put("traceId", "trace-id-from-mdc");

        String traceId = provider.getTraceId();

        Assertions.assertEquals("trace-id-from-mdc", traceId);
        Assertions.assertEquals("apm", provider.getProviderName());
    }

    @Test
    void shouldReturnNullWhenNoMdcAndNoSentrySpan() {
        String traceId = provider.getTraceId();

        Assertions.assertNull(traceId);
    }
}
