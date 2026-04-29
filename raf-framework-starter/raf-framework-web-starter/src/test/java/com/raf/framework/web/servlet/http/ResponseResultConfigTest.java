package com.raf.framework.web.servlet.http;

import com.raf.framework.core.common.result.RafResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.mockito.Mockito.mock;

/**
 * Tests for ResponseResultConfig.
 *
 * @author Jerry
 * @since 2026-04-29
 */
class ResponseResultConfigTest {

    private final ResponseResultConfig config = new ResponseResultConfig();

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    // ─── supports ────────────────────────────────────────────────────────────

    @Test
    void supports_withResponseResultAttribute_returnsTrue() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        // Get a real ResponseResult annotation instance from an annotated element
        @ResponseResult
        class AnnotatedClass {}
        ResponseResult annotation = AnnotatedClass.class.getAnnotation(ResponseResult.class);
        request.setAttribute(ResponseResultConfig.RESPONSE_RESULT, annotation);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        boolean result = config.supports(mock(MethodParameter.class), null);

        Assertions.assertTrue(result);
    }

    @Test
    void supports_withoutResponseResultAttribute_returnsFalse() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        // No RESPONSE_RESULT attribute set
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        boolean result = config.supports(mock(MethodParameter.class), null);

        Assertions.assertFalse(result);
    }

    // ─── beforeBodyWrite ─────────────────────────────────────────────────────

    @Test
    void beforeBodyWrite_withRafResult_returnsAsIs() {
        RafResult<String> rafResult = RafResult.success("data");

        Object result = config.beforeBodyWrite(rafResult, mock(MethodParameter.class),
                MediaType.APPLICATION_JSON, null, null, null);

        Assertions.assertSame(rafResult, result);
    }

    @Test
    void beforeBodyWrite_withPlainObject_wrapsInRafResult() {
        String payload = "hello";

        Object result = config.beforeBodyWrite(payload, mock(MethodParameter.class),
                MediaType.APPLICATION_JSON, null, null, null);

        Assertions.assertInstanceOf(RafResult.class, result);
        @SuppressWarnings("unchecked")
        RafResult<String> rafResult = (RafResult<String>) result;
        Assertions.assertEquals("hello", rafResult.getData());
    }

    @Test
    void beforeBodyWrite_withNull_wrapsInRafResult() {
        Object result = config.beforeBodyWrite(null, mock(MethodParameter.class),
                MediaType.APPLICATION_JSON, null, null, null);

        Assertions.assertInstanceOf(RafResult.class, result);
    }
}
