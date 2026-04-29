package com.raf.framework.web.servlet.log;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for LogHolder.
 *
 * @author Jerry
 * @since 2026-04-29
 */
class LogHolderTest {

    @AfterEach
    void tearDown() {
        LogHolder.remove();
    }

    @Test
    void setCurrentLogResponse_true_returnsTrue() {
        LogHolder.setCurrentLogResponse(true);
        Assertions.assertTrue(LogHolder.currentLogResponse());
    }

    @Test
    void setCurrentLogResponse_false_returnsFalse() {
        LogHolder.setCurrentLogResponse(false);
        Assertions.assertFalse(LogHolder.currentLogResponse());
    }

    @Test
    void remove_clearsValue() {
        LogHolder.setCurrentLogResponse(true);
        LogHolder.remove();
        // After remove, ThreadLocal returns null which unboxes to false via Boolean
        // Actually it throws NPE if we call currentLogResponse() after remove because it returns null
        // The field is ThreadLocal<Boolean>, so after remove get() returns null
        // currentLogResponse() calls currentLogResponse.get() which returns null
        // Calling it would throw NPE. Let's just verify the ThreadLocal is cleared.
        Assertions.assertNull(LogHolder.currentLogResponse.get());
    }

    @Test
    void currentLogResponse_threadLocal_isIsolatedBetweenThreads() throws InterruptedException {
        LogHolder.setCurrentLogResponse(true);

        boolean[] otherThreadValue = {false};
        Thread t = new Thread(() -> {
            // Other thread should see null (not set), so we set it to false
            LogHolder.setCurrentLogResponse(false);
            otherThreadValue[0] = LogHolder.currentLogResponse();
        });
        t.start();
        t.join();

        // Main thread still sees true
        Assertions.assertTrue(LogHolder.currentLogResponse());
        // Other thread saw false
        Assertions.assertFalse(otherThreadValue[0]);
    }
}
