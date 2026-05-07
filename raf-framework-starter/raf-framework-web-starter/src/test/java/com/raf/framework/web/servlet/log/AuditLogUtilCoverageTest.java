package com.raf.framework.web.servlet.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raf.framework.core.jackson.JsonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Coverage tests for AuditLogUtil methods that require JsonService.
 * Injects JsonService via reflection to avoid starting a Spring context.
 *
 * @author Jerry
 * @since 2026-05-07
 */
class AuditLogUtilCoverageTest {

    @BeforeEach
    void injectJsonService() throws Exception {
        // AuditLogUtil.json is a private static volatile field
        Field jsonField = AuditLogUtil.class.getDeclaredField("json");
        jsonField.setAccessible(true);
        jsonField.set(null, new JsonService(new ObjectMapper()));
    }

    // ─── getAllHeaders(request) ───────────────────────────────────────────────

    @Test
    void getAllHeaders_request_returnsJsonWithWhitelistedHeaders() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("x-user-id", "user-123");
        request.addHeader("x-tenant-id", "tenant-456");
        request.addHeader("x-secret", "should-not-appear");

        String result = AuditLogUtil.getAllHeaders(request);

        assertNotNull(result);
        assertTrue(result.contains("x-user-id") || result.contains("user-123"));
        assertFalse(result.contains("should-not-appear"));
    }

    @Test
    void getAllHeaders_request_noWhitelistedHeaders_returnsEmptyJson() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("x-custom-header", "value");

        String result = AuditLogUtil.getAllHeaders(request);

        assertNotNull(result);
        // No whitelisted headers → empty map JSON
        assertTrue(result.contains("{}") || result.equals("{}"));
    }

    // ─── getAllCookies ────────────────────────────────────────────────────────

    @Test
    void getAllCookies_withCookies_returnsMaskedJson() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(
                new jakarta.servlet.http.Cookie("session", "abc123"),
                new jakarta.servlet.http.Cookie("pref", "dark-mode")
        );

        String result = AuditLogUtil.getAllCookies(request);

        assertNotNull(result);
        // Cookie values should be masked as "***"
        assertTrue(result.contains("***"));
        assertFalse(result.contains("abc123"));
    }

    @Test
    void getAllCookies_noCookies_returnsEmpty() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        // No cookies set

        String result = AuditLogUtil.getAllCookies(request);

        assertEquals("", result);
    }

    // ─── getRequestParams ─────────────────────────────────────────────────────

    @Test
    void getRequestParams_normalParams_returnsJson() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("page", "1");
        request.addParameter("size", "20");

        String result = AuditLogUtil.getRequestParams(request);

        assertNotNull(result);
        assertTrue(result.contains("page") || result.contains("1"));
    }

    @Test
    void getRequestParams_sensitiveParam_masksSensitiveValue() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("password", "secret123");
        request.addParameter("username", "jerry");

        String result = AuditLogUtil.getRequestParams(request);

        assertNotNull(result);
        assertFalse(result.contains("secret123"));
        assertTrue(result.contains("***"));
    }

    @Test
    void getRequestParams_tokenParam_masksToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("token", "eyJhbGciOiJIUzI1NiJ9");

        String result = AuditLogUtil.getRequestParams(request);

        assertFalse(result.contains("eyJhbGciOiJIUzI1NiJ9"));
        assertTrue(result.contains("***"));
    }

    // ─── getAllHeaders(response) ──────────────────────────────────────────────

    @Test
    void getAllHeaders_response_returnsJson() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.addHeader("Content-Length", "128");

        String result = AuditLogUtil.getAllHeaders(response);

        assertNotNull(result);
        // Should contain "length" key from the response headers map
        assertTrue(result.contains("length"));
    }

    // ─── getTrace ─────────────────────────────────────────────────────────────

    @Test
    void getTrace_returnsStackTraceString() {
        RuntimeException ex = new RuntimeException("test error");

        String trace = AuditLogUtil.getTrace(ex);

        assertNotNull(trace);
        assertTrue(trace.contains("RuntimeException"));
        assertTrue(trace.contains("test error"));
    }

    // ─── getRequestUrl / getRequestUri (already in AuditLogUtilTest, verify) ──

    @Test
    void getRequestUrl_includesSchemeAndHost() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/orders");
        request.setScheme("https");
        request.setServerName("api.example.com");
        request.setServerPort(443);

        String url = AuditLogUtil.getRequestUrl(request);

        assertTrue(url.contains("api.example.com"));
        assertTrue(url.contains("/api/v1/orders"));
    }
}
