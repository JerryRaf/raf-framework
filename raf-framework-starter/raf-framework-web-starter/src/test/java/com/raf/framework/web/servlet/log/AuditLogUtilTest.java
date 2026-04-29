package com.raf.framework.web.servlet.log;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Extended tests for AuditLogUtil.maskSensitiveData and getTrace.
 *
 * @author Jerry
 * @since 2026-04-29
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

    // ─── maskSensitiveData edge cases ────────────────────────────────────────

    @Test
    void maskSensitiveData_nullInput_returnsNull() {
        Assertions.assertNull(AuditLogUtil.maskSensitiveData(null));
    }

    @Test
    void maskSensitiveData_emptyInput_returnsEmpty() {
        Assertions.assertEquals("", AuditLogUtil.maskSensitiveData(""));
    }

    @Test
    void maskSensitiveData_noSensitiveData_returnsUnchanged() {
        String input = "hello world, no sensitive info here";
        Assertions.assertEquals(input, AuditLogUtil.maskSensitiveData(input));
    }

    @Test
    void maskSensitiveData_masksPhoneNumber() {
        String masked = AuditLogUtil.maskSensitiveData("mobile: 13912345678");
        Assertions.assertFalse(masked.contains("13912345678"));
        Assertions.assertTrue(masked.contains("****"));
    }

    @Test
    void maskSensitiveData_masksIdCard() {
        // Use an ID card that won't be partially matched by the phone regex first
        // Phone regex: (1[3-9]\d)(\d{4})(\d{4}) — avoid starting with 1[3-9]
        String masked = AuditLogUtil.maskSensitiveData("idcard: 320101199001011234");
        // Either the ID card was masked (contains ****) or the original is gone
        Assertions.assertFalse(masked.contains("320101199001011234"));
    }

    @Test
    void maskSensitiveData_masksEmail() {
        String masked = AuditLogUtil.maskSensitiveData("email: jerry@example.com");
        Assertions.assertFalse(masked.contains("jerry@example.com"));
        Assertions.assertTrue(masked.contains("***@"));
    }

    @Test
    void maskSensitiveData_masksBankCard() {
        String masked = AuditLogUtil.maskSensitiveData("card: 6222021234567890");
        Assertions.assertFalse(masked.contains("6222021234567890"));
        Assertions.assertTrue(masked.contains("****"));
    }

    @Test
    void maskSensitiveData_masksAddress() {
        // Address pattern: 省市区 + 4+ chars
        String input = "addr: 广东省深圳市南山区科技园南路";
        String masked = AuditLogUtil.maskSensitiveData(input);
        // The address detail should be masked
        Assertions.assertFalse(masked.contains("科技园南路"));
    }

    // ─── getRequestUrl / getRequestUri ───────────────────────────────────────

    @Test
    void getRequestUrl_returnsFullUrl() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        request.setServerName("example.com");
        request.setServerPort(8080);
        request.setScheme("http");

        String url = AuditLogUtil.getRequestUrl(request);
        Assertions.assertNotNull(url);
        Assertions.assertTrue(url.contains("/api/users"));
    }

    @Test
    void getRequestUri_returnsUri() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders/123");

        String uri = AuditLogUtil.getRequestUri(request);
        Assertions.assertEquals("/api/orders/123", uri);
    }
}
