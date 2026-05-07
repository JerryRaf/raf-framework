package com.raf.framework.web.servlet.log;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Tests for AccessLogFilter.HttpServletRequestAdapter (XSS sanitization).
 * The full filter integration requires a running servlet container, so we test
 * the inner adapter class directly.
 *
 * @author Jerry
 * @since 2026-04-29
 */
class AccessLogFilterTest {

    // ─── HttpServletRequestAdapter ───────────────────────────────────────────

    @Test
    void adapter_getHeader_sanitizesXssCharacters() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("x-custom", "<script>alert(1)</script>");

        AccessLogFilter.HttpServletRequestAdapter adapter =
                new AccessLogFilter.HttpServletRequestAdapter(request, new byte[0]);

        String header = adapter.getHeader("x-custom");
        Assertions.assertNotNull(header);
        Assertions.assertFalse(header.contains("<script>"));
    }

    @Test
    void adapter_getHeader_withNullName_returnsNull() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        AccessLogFilter.HttpServletRequestAdapter adapter =
                new AccessLogFilter.HttpServletRequestAdapter(request, new byte[0]);

        Assertions.assertNull(adapter.getHeader(null));
    }

    @Test
    void adapter_getParameter_sanitizesXssCharacters() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("name", "<img src=x onerror=alert(1)>");

        AccessLogFilter.HttpServletRequestAdapter adapter =
                new AccessLogFilter.HttpServletRequestAdapter(request, new byte[0]);

        String param = adapter.getParameter("name");
        Assertions.assertNotNull(param);
        Assertions.assertFalse(param.contains("<img"));
    }

    @Test
    void adapter_getParameter_withNullName_returnsNull() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        AccessLogFilter.HttpServletRequestAdapter adapter =
                new AccessLogFilter.HttpServletRequestAdapter(request, new byte[0]);

        Assertions.assertNull(adapter.getParameter(null));
    }

    @Test
    void adapter_getParameterValues_sanitizesAllValues() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("tags", "<b>bold</b>");
        request.addParameter("tags", "normal");

        AccessLogFilter.HttpServletRequestAdapter adapter =
                new AccessLogFilter.HttpServletRequestAdapter(request, new byte[0]);

        String[] values = adapter.getParameterValues("tags");
        Assertions.assertNotNull(values);
        Assertions.assertEquals(2, values.length);
        for (String v : values) {
            Assertions.assertFalse(v.contains("<b>"));
        }
    }

    @Test
    void adapter_getParameterValues_withNoValues_returnsEmptyArray() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        AccessLogFilter.HttpServletRequestAdapter adapter =
                new AccessLogFilter.HttpServletRequestAdapter(request, new byte[0]);

        String[] values = adapter.getParameterValues("nonexistent");
        Assertions.assertNotNull(values);
        Assertions.assertEquals(0, values.length);
    }

    @Test
    void adapter_getInputStream_returnsPayloadBytes() throws Exception {
        byte[] payload = "hello".getBytes();
        MockHttpServletRequest request = new MockHttpServletRequest();
        AccessLogFilter.HttpServletRequestAdapter adapter =
                new AccessLogFilter.HttpServletRequestAdapter(request, payload);

        jakarta.servlet.ServletInputStream stream = adapter.getInputStream();
        Assertions.assertNotNull(stream);
        // Read first byte
        int firstByte = stream.read();
        Assertions.assertEquals('h', firstByte);
    }

    @Test
    void adapter_getHeader_trimsWhitespace() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("x-id", "  123  ");

        AccessLogFilter.HttpServletRequestAdapter adapter =
                new AccessLogFilter.HttpServletRequestAdapter(request, new byte[0]);

        String header = adapter.getHeader("x-id");
        Assertions.assertNotNull(header);
        // HtmlUtils.htmlEscape trims then escapes; "123" has no HTML chars
        Assertions.assertEquals("123", header);
    }
}
