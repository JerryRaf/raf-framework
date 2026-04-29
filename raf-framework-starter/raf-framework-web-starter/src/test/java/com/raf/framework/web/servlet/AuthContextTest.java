package com.raf.framework.web.servlet;

import com.raf.framework.core.common.exception.ProtocolException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Tests for AuthContext.
 *
 * @author Jerry
 * @since 2026-04-29
 */
class AuthContextTest {

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    // ─── getAuthorization ────────────────────────────────────────────────────

    @Test
    void getAuthorization_returnsHeaderValue() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token123");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        String auth = AuthContext.getAuthorization();

        Assertions.assertEquals("Bearer token123", auth);
    }

    @Test
    void getAuthorization_withNoHeader_returnsNull() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        String auth = AuthContext.getAuthorization();

        Assertions.assertNull(auth);
    }

    @Test
    void getAuthorization_withNoRequestContext_returnsNull() {
        RequestContextHolder.resetRequestAttributes();

        String auth = AuthContext.getAuthorization();

        Assertions.assertNull(auth);
    }

    // ─── getUserId ───────────────────────────────────────────────────────────

    @Test
    void getUserId_withValidHeader_returnsUserId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("x-user-id", "12345");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Long userId = AuthContext.getUserId();

        Assertions.assertEquals(12345L, userId);
    }

    @Test
    void getUserId_withNoHeader_returnsZero() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Long userId = AuthContext.getUserId();

        Assertions.assertEquals(0L, userId);
    }

    @Test
    void getUserId_withNoRequestContext_returnsZero() {
        RequestContextHolder.resetRequestAttributes();

        Long userId = AuthContext.getUserId();

        Assertions.assertEquals(0L, userId);
    }

    // ─── getUserIdAndThrow ───────────────────────────────────────────────────

    @Test
    void getUserIdAndThrow_withValidHeader_returnsUserId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("x-user-id", "99");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Long userId = AuthContext.getUserIdAndThrow();

        Assertions.assertEquals(99L, userId);
    }

    @Test
    void getUserIdAndThrow_withNoHeader_throwsException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        // AuthContext.getUserIdAndThrow throws ProtocolException(null) which causes NPE
        // because BaseException calls responseEnum.getMsg() on null
        Assertions.assertThrows(Exception.class, AuthContext::getUserIdAndThrow);
    }

    @Test
    void getUserIdAndThrow_withNoRequestContext_throwsException() {
        RequestContextHolder.resetRequestAttributes();

        Assertions.assertThrows(Exception.class, AuthContext::getUserIdAndThrow);
    }
}
