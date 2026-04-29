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
 * Tests for RafContextTaskDecorator.
 *
 * @author Jerry
 * @since 2026-04-29
 */
class RafContextTaskDecoratorTest {

    private final RafContextTaskDecorator decorator = new RafContextTaskDecorator();

    @Test
    void decorate_propagatesMdcToChildThread() throws InterruptedException {
        MDC.put("traceId", "raf-trace-456");
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        AtomicReference<String> capturedTrace = new AtomicReference<>();

        Runnable decorated = decorator.decorate(() -> capturedTrace.set(MDC.get("traceId")));

        Thread t = new Thread(decorated);
        t.start();
        t.join();

        Assertions.assertEquals("raf-trace-456", capturedTrace.get());

        MDC.clear();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void decorate_propagatesRequestAttributesToChildThread() throws InterruptedException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/order");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        AtomicBoolean hasRequestAttrs = new AtomicBoolean(false);

        Runnable decorated = decorator.decorate(
                () -> hasRequestAttrs.set(RequestContextHolder.getRequestAttributes() != null));

        Thread t = new Thread(decorated);
        t.start();
        t.join();

        Assertions.assertTrue(hasRequestAttrs.get());

        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void decorate_withNullMdc_doesNotThrow() throws InterruptedException {
        // No MDC set, no request context
        MDC.clear();
        RequestContextHolder.resetRequestAttributes();

        AtomicBoolean ran = new AtomicBoolean(false);
        Runnable decorated = decorator.decorate(() -> ran.set(true));

        Thread t = new Thread(decorated);
        t.start();
        t.join();

        Assertions.assertTrue(ran.get());
    }

    @Test
    void decorate_cleansUpContextAfterRun() throws InterruptedException {
        MDC.put("key", "val");
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        AtomicReference<String> mdcAfter = new AtomicReference<>();
        AtomicReference<Object> attrAfter = new AtomicReference<>();

        Runnable decorated = decorator.decorate(() -> {
            // do nothing
        });

        Thread t = new Thread(() -> {
            decorated.run();
            mdcAfter.set(MDC.get("key"));
            attrAfter.set(RequestContextHolder.getRequestAttributes());
        });
        t.start();
        t.join();

        Assertions.assertNull(mdcAfter.get());
        Assertions.assertNull(attrAfter.get());

        MDC.clear();
        RequestContextHolder.resetRequestAttributes();
    }
}
