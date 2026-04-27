package com.raf.framework.web.servlet.log;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for AuditLogUtil.
 *
 * @author Jerry
 * @since 2026-04-27
 */
class AuditLogUtilTest {

    @Test
    void shouldMaskTypicalSensitiveData() {
        String source = "phone=13812345678,id=110101199001011234,email=test.user@example.com,bank=6222021234567890123";

        String masked = AuditLogUtil.maskSensitiveData(source);

        Assertions.assertNotEquals(source, masked);
        Assertions.assertFalse(masked.contains("13812345678"));
        Assertions.assertFalse(masked.contains("110101199001011234"));
        Assertions.assertFalse(masked.contains("6222021234567890123"));
        Assertions.assertFalse(masked.contains("test.user@example.com"));
        Assertions.assertTrue(masked.contains("****"));
    }
}
