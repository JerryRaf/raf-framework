package com.raf.framework.sentry.processor;

import com.raf.framework.sentry.RafSentryProperties;
import com.raf.framework.sentry.trace.TraceIdProvider;
import io.sentry.Hint;
import io.sentry.SentryEvent;
import io.sentry.protocol.SentryException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;

/**
 * Tests for RafSentryEventProcessor.
 *
 * @author Jerry
 * @since 2026-04-27
 */
class RafSentryEventProcessorTest {

    @Test
    void shouldInjectTraceTagsAndFrameworkTagsWhenEventAccepted() {
        RafSentryProperties properties = new RafSentryProperties();
        properties.getExceptionFilter().setEnabled(false);

        TraceIdProvider traceIdProvider = new FixedTraceIdProvider("trace-id-001", "framework");
        RafSentryEventProcessor processor = new RafSentryEventProcessor(traceIdProvider, properties);

        SentryEvent event = new SentryEvent();
        SentryEvent processed = processor.process(event, new Hint());

        Assertions.assertNotNull(processed);
        Assertions.assertEquals("trace-id-001", processed.getTag("traceId"));
        Assertions.assertEquals("trace-id-001", processed.getTag("trace_id"));
        Assertions.assertEquals("raf-framework", processed.getTag("framework"));
        Assertions.assertEquals("3.0.0", processed.getTag("framework.version"));
    }

    @Test
    void shouldFilterExcludedExceptionFirst() {
        RafSentryProperties properties = new RafSentryProperties();
        properties.getExceptionFilter().setEnabled(true);
        properties.getExceptionFilter().setIncludeExceptions(new HashSet<>(Collections.singleton("java.lang.RuntimeException")));
        properties.getExceptionFilter().setExcludeExceptions(new HashSet<>(Collections.singleton("java.lang.RuntimeException")));

        RafSentryEventProcessor processor = new RafSentryEventProcessor(new FixedTraceIdProvider(null, "framework"), properties);

        SentryEvent event = new SentryEvent();
        event.setExceptions(Collections.singletonList(exception("java.lang.RuntimeException")));

        SentryEvent processed = processor.process(event, new Hint());

        Assertions.assertNull(processed);
    }

    @Test
    void shouldFilterBusinessExceptionByDefault() {
        RafSentryProperties properties = new RafSentryProperties();
        properties.getExceptionFilter().setEnabled(true);
        properties.getExceptionFilter().setReportBusinessException(false);
        properties.getExceptionFilter().setIncludeExceptions(new HashSet<>());

        RafSentryEventProcessor processor = new RafSentryEventProcessor(new FixedTraceIdProvider(null, "framework"), properties);

        SentryEvent event = new SentryEvent();
        event.setExceptions(Collections.singletonList(exception("com.foo.BusinessException")));

        SentryEvent processed = processor.process(event, new Hint());

        Assertions.assertNull(processed);
    }

    @Test
    void shouldAcceptBusinessExceptionWhenConfigured() {
        RafSentryProperties properties = new RafSentryProperties();
        properties.getExceptionFilter().setEnabled(true);
        properties.getExceptionFilter().setReportBusinessException(true);
        properties.getExceptionFilter().setIncludeExceptions(new HashSet<>());

        RafSentryEventProcessor processor = new RafSentryEventProcessor(new FixedTraceIdProvider(null, "framework"), properties);

        SentryEvent event = new SentryEvent();
        event.setExceptions(Collections.singletonList(exception("com.foo.BusinessException")));

        SentryEvent processed = processor.process(event, new Hint());

        Assertions.assertNotNull(processed);
    }

    @Test
    void shouldRespectIncludeExceptionWhitelist() {
        RafSentryProperties properties = new RafSentryProperties();
        properties.getExceptionFilter().setEnabled(true);
        properties.getExceptionFilter().setIncludeExceptions(new HashSet<>(Collections.singleton("java.lang.IllegalStateException")));
        properties.getExceptionFilter().setExcludeExceptions(new HashSet<>());

        RafSentryEventProcessor processor = new RafSentryEventProcessor(new FixedTraceIdProvider(null, "framework"), properties);

        SentryEvent filteredEvent = new SentryEvent();
        filteredEvent.setExceptions(Collections.singletonList(exception("java.lang.RuntimeException")));

        SentryEvent allowedEvent = new SentryEvent();
        allowedEvent.setExceptions(Collections.singletonList(exception("java.lang.IllegalStateException")));

        Assertions.assertNull(processor.process(filteredEvent, new Hint()));
        Assertions.assertNotNull(processor.process(allowedEvent, new Hint()));
    }

    private static SentryException exception(String type) {
        SentryException exception = new SentryException();
        exception.setType(type);
        return exception;
    }

    private record FixedTraceIdProvider(String traceId, String providerName) implements TraceIdProvider {

        @Override
        public String getTraceId() {
            return traceId;
        }

        @Override
        public String getProviderName() {
            return providerName;
        }
    }
}
