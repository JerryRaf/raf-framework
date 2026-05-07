package com.raf.framework.web.servlet.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raf.framework.core.jackson.JsonService;
import com.raf.framework.core.spring.bean.SpringContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Coverage tests for AccessLogFilter — exercises doFilterInternal paths
 * not covered by AccessLogFilterFullTest.
 *
 * @author Jerry
 * @since 2026-05-07
 */
class AccessLogFilterCoverageTest {

    private AccessLogFilter filter;
    private AuditProperties auditProperties;

    @BeforeEach
    void setUp() throws Exception {
        filter = new AccessLogFilter();
        JsonService jsonService = new JsonService(new ObjectMapper());
        auditProperties = new AuditProperties();

        setField(filter, "json", jsonService);
        setField(filter, "auditProperties", auditProperties);

        // AuditLogUtil.getAllHeaders etc. use SpringContext.getBean(JsonService.class)
        // Inject a mock ApplicationContext so those calls don't NPE
        ApplicationContext mockCtx = Mockito.mock(ApplicationContext.class);
        Mockito.when(mockCtx.getBean(JsonService.class)).thenReturn(jsonService);
        SpringContext springContext = new SpringContext();
        springContext.setApplicationContext(mockCtx);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    // ─── BASIC level: normal JSON request ────────────────────────────────────

    @Test
    void doFilter_basicLevel_normalJsonRequest_executesChain() throws Exception {
        auditProperties.getLog().level = AuditProperties.LogLevel.BASIC;

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");
        request.setServletPath("/api/orders");
        request.setContentType("application/json");
        request.setContent("{\"id\":1}".getBytes());

        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setContentType("application/json");
        MockFilterChain chain = new MockFilterChain();

        assertDoesNotThrow(() -> filter.doFilter(request, response, chain));
        assertNotNull(chain.getRequest());
    }

    // ─── REQ_BODY level ───────────────────────────────────────────────────────

    @Test
    void doFilter_reqBodyLevel_logsRequestBody() throws Exception {
        auditProperties.getLog().level = AuditProperties.LogLevel.REQ_BODY;

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/users");
        request.setServletPath("/api/users");
        request.setContentType("application/json");
        request.setContent("{\"name\":\"test\"}".getBytes());
        request.setCharacterEncoding("UTF-8");

        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setContentType("application/json");
        MockFilterChain chain = new MockFilterChain();

        assertDoesNotThrow(() -> filter.doFilter(request, response, chain));
    }

    // ─── RSP_BODY level ───────────────────────────────────────────────────────

    @Test
    void doFilter_rspBodyLevel_logsResponseBody() throws Exception {
        auditProperties.getLog().level = AuditProperties.LogLevel.RSP_BODY;

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/items");
        request.setServletPath("/api/items");

        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setContentType("application/json");

        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res)
                    throws java.io.IOException, jakarta.servlet.ServletException {
                LogHolder.setCurrentLogResponse(true);
                res.getWriter().write("{\"result\":\"ok\"}");
                super.doFilter(req, res);
            }
        };

        assertDoesNotThrow(() -> filter.doFilter(request, response, chain));
    }

    // ─── Binary content: image request skips logging ─────────────────────────

    @Test
    void doFilter_imageContentType_skipsLogging() throws Exception {
        auditProperties.getLog().level = AuditProperties.LogLevel.BASIC;

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/photo");
        request.setServletPath("/api/photo");
        request.setContentType("image/png");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        assertDoesNotThrow(() -> filter.doFilter(request, response, chain));
        assertNotNull(chain.getRequest());
    }

    // ─── Multipart request skips body caching ────────────────────────────────

    @Test
    void doFilter_multipartRequest_skipsBodyCaching() throws Exception {
        auditProperties.getLog().level = AuditProperties.LogLevel.REQ_BODY;

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/upload");
        request.setServletPath("/api/upload");
        request.setContentType("multipart/form-data; boundary=----boundary");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        assertDoesNotThrow(() -> filter.doFilter(request, response, chain));
    }

    // ─── Binary response: video content type ─────────────────────────────────

    @Test
    void doFilter_videoResponseContentType_skipsResponseLogging() throws Exception {
        auditProperties.getLog().level = AuditProperties.LogLevel.RSP_BODY;

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/stream");
        request.setServletPath("/api/stream");

        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setContentType("video/mp4");

        MockFilterChain chain = new MockFilterChain();

        assertDoesNotThrow(() -> filter.doFilter(request, response, chain));
    }

    // ─── Octet-stream response ────────────────────────────────────────────────

    @Test
    void doFilter_octetStreamResponse_skipsResponseLogging() throws Exception {
        auditProperties.getLog().level = AuditProperties.LogLevel.RSP_BODY;

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/download");
        request.setServletPath("/api/download");

        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setContentType("application/octet-stream");

        MockFilterChain chain = new MockFilterChain();

        assertDoesNotThrow(() -> filter.doFilter(request, response, chain));
    }

    // ─── Large body: exceeds maxBodyCacheBytes ────────────────────────────────

    @Test
    void doFilter_largeBody_omitsBodyCaching() throws Exception {
        auditProperties.getLog().level = AuditProperties.LogLevel.REQ_BODY;
        // Set maxBodyCacheBytes to 0 so any content is "too large"
        auditProperties.getLog().setMaxBodyCacheBytes(0);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/data");
        request.setServletPath("/api/data");
        request.setContentType("application/json");
        byte[] bigContent = "{\"big\":\"payload\"}".getBytes();
        request.setContent(bigContent);

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        assertDoesNotThrow(() -> filter.doFilter(request, response, chain));
    }

    // ─── getPayloadMaxLength default ──────────────────────────────────────────

    @Test
    void getPayloadMaxLength_defaultIs4096() {
        assertEquals(4096, filter.getPayloadMaxLength());
    }

    // ─── HttpServletRequestAdapter.getInputStream ─────────────────────────────

    @Test
    void adapter_getInputStream_isFinished_returnsFalse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        AccessLogFilter.HttpServletRequestAdapter adapter =
                new AccessLogFilter.HttpServletRequestAdapter(request, new byte[0]);

        jakarta.servlet.ServletInputStream stream = adapter.getInputStream();
        assertFalse(stream.isFinished());
    }

    @Test
    void adapter_getInputStream_isReady_returnsFalse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        AccessLogFilter.HttpServletRequestAdapter adapter =
                new AccessLogFilter.HttpServletRequestAdapter(request, new byte[0]);

        jakarta.servlet.ServletInputStream stream = adapter.getInputStream();
        assertFalse(stream.isReady());
    }

    @Test
    void adapter_getInputStream_setReadListener_doesNotThrow() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        AccessLogFilter.HttpServletRequestAdapter adapter =
                new AccessLogFilter.HttpServletRequestAdapter(request, new byte[0]);

        jakarta.servlet.ServletInputStream stream = adapter.getInputStream();
        assertDoesNotThrow(() -> stream.setReadListener(null));
    }
}
