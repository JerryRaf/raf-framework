package com.raf.framework.web.async;

import org.junit.jupiter.api.Test;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ThreadPoolConfig and its inner ThreadExceptionHandler.
 * ThreadPoolConfig is @ConditionalOnProperty so we test it directly
 * without a Spring context.
 *
 * @author Jerry
 * @since 2026-05-07
 */
class ThreadPoolConfigTest {

    // ─── getAsyncUncaughtExceptionHandler ─────────────────────────────────────

    @Test
    void getAsyncUncaughtExceptionHandler_returnsNonNull() {
        ThreadPoolConfig config = new ThreadPoolConfig();
        AsyncUncaughtExceptionHandler handler = config.getAsyncUncaughtExceptionHandler();
        assertNotNull(handler);
    }

    // ─── ThreadExceptionHandler.handleUncaughtException ───────────────────────

    @Test
    void handleUncaughtException_withArgs_doesNotThrow() throws Exception {
        ThreadPoolConfig config = new ThreadPoolConfig();
        AsyncUncaughtExceptionHandler handler = config.getAsyncUncaughtExceptionHandler();

        Method method = SampleService.class.getMethod("doWork", String.class, int.class);
        RuntimeException ex = new RuntimeException("async failure");

        // Should log and not throw
        assertDoesNotThrow(() -> handler.handleUncaughtException(ex, method, "arg1", 42));
    }

    @Test
    void handleUncaughtException_withNoArgs_doesNotThrow() throws Exception {
        ThreadPoolConfig config = new ThreadPoolConfig();
        AsyncUncaughtExceptionHandler handler = config.getAsyncUncaughtExceptionHandler();

        Method method = SampleService.class.getMethod("noArgs");
        RuntimeException ex = new RuntimeException("no-arg failure");

        assertDoesNotThrow(() -> handler.handleUncaughtException(ex, method));
    }

    @Test
    void handleUncaughtException_withNullArg_doesNotThrow() throws Exception {
        ThreadPoolConfig config = new ThreadPoolConfig();
        AsyncUncaughtExceptionHandler handler = config.getAsyncUncaughtExceptionHandler();

        Method method = SampleService.class.getMethod("doWork", String.class, int.class);
        RuntimeException ex = new RuntimeException("null arg");

        assertDoesNotThrow(() -> handler.handleUncaughtException(ex, method, (Object) null, 1));
    }

    // ─── Helper class for method reflection ──────────────────────────────────

    @SuppressWarnings("unused")
    public static class SampleService {
        public void doWork(String name, int count) {}
        public void noArgs() {}
    }
}
