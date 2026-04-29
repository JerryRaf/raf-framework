package com.raf.framework.web.async;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tests for ContextCopyingDecorator.
 *
 * @author Jerry
 * @since 2026-04-29
 */
class ContextCopyingDecoratorTest {

    private final ContextCopyingDecorator decorator = new ContextCopyingDecorator();

    @Test
    void decorate_withRequestContext_propagatesContextToRunnable() throws InterruptedException {
        // Set up request context in main thread
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/test");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        MDC.put("traceId", "test-trace-123");

        AtomicReference<String> capturedTraceId = new AtomicReference<>();
        AtomicBoolean contextWasSet = new AtomicBoolean(false);

        Runnable decorated = decorator.decorate(() -> {
            capturedTraceId.set(MDC.get("traceId"));
            contextWasSet.set(RequestContextHolder.getRequestAttributes() != null);
        });

        // Run in a new thread to simulate async execution
        Thread t = new Thread(decorated);
        t.start();
        t.join();

        Assertions.assertEquals("test-trace-123", capturedTraceId.get());
        Assertions.assertTrue(contextWasSet.get());

        // Cleanup
        RequestContextHolder.resetRequestAttributes();
        MDC.clear();
    }

    @Test
    void decorate_withoutRequestContext_returnsOriginalRunnable() {
        // No request context set
        RequestContextHolder.resetRequestAttributes();

        AtomicBoolean ran = new AtomicBoolean(false);
        Runnable original = () -> ran.set(true);

        Runnable decorated = decorator.decorate(original);

        // Should return the original runnable (IllegalStateException branch)
        decorated.run();
        Assertions.assertTrue(ran.get());
    }

    @Test
    void decorate_cleansUpContextAfterRun() throws InterruptedException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        MDC.put("key", "value");

        AtomicReference<String> mdcAfterRun = new AtomicReference<>();
        AtomicReference<Object> requestAttrAfterRun = new AtomicReference<>();

        Runnable decorated = decorator.decorate(() -> {
            // do nothing
        });

        Thread t = new Thread(() -> {
            decorated.run();
            // After run, context should be cleared
            mdcAfterRun.set(MDC.get("key"));
            requestAttrAfterRun.set(RequestContextHolder.getRequestAttributes());
        });
        t.start();
        t.join();

        Assertions.assertNull(mdcAfterRun.get());
        Assertions.assertNull(requestAttrAfterRun.get());

        RequestContextHolder.resetRequestAttributes();
        MDC.clear();
    }
}
