package com.raf.framework.core.util;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Tests for IpUtil using Mockito to mock HttpServletRequest.
 *
 * @author Jerry
 * @since 2026-04-29
 */
class IpUtilTest {

    // ─── getIpAddr (Servlet) ─────────────────────────────────────────────────

    @Test
    void getIpAddr_returnsIpFromXForwardedForHeader() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader("x-forwarded-for")).thenReturn("192.168.1.100");
        Mockito.when(request.getRemoteAddr()).thenReturn("10.0.0.1");

        String ip = IpUtil.getIpAddr(request);

        Assertions.assertEquals("192.168.1.100", ip);
    }

    @Test
    void getIpAddr_returnsFirstIpWhenMultipleForwarded() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader("x-forwarded-for")).thenReturn("192.168.1.100, 10.0.0.1");
        Mockito.when(request.getRemoteAddr()).thenReturn("10.0.0.2");

        String ip = IpUtil.getIpAddr(request);

        Assertions.assertEquals("192.168.1.100", ip);
    }

    @Test
    void getIpAddr_fallsBackToRemoteAddrWhenHeadersUnknown() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader(Mockito.anyString())).thenReturn("unknown");
        Mockito.when(request.getRemoteAddr()).thenReturn("172.16.0.5");

        String ip = IpUtil.getIpAddr(request);

        Assertions.assertEquals("172.16.0.5", ip);
    }

    @Test
    void getIpAddr_fallsBackToRemoteAddrWhenHeadersNull() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader(Mockito.anyString())).thenReturn(null);
        Mockito.when(request.getRemoteAddr()).thenReturn("10.10.10.10");

        String ip = IpUtil.getIpAddr(request);

        Assertions.assertEquals("10.10.10.10", ip);
    }

    @Test
    void getIpAddr_convertsIpv6LoopbackToIpv4() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader(Mockito.anyString())).thenReturn(null);
        Mockito.when(request.getRemoteAddr()).thenReturn("0:0:0:0:0:0:0:1");

        String ip = IpUtil.getIpAddr(request);

        Assertions.assertEquals("127.0.0.1", ip);
    }

    @Test
    void getIpAddr_stripsPortFromRemoteAddr() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader(Mockito.anyString())).thenReturn(null);
        Mockito.when(request.getRemoteAddr()).thenReturn("192.168.1.1:8080");

        String ip = IpUtil.getIpAddr(request);

        Assertions.assertEquals("192.168.1.1", ip);
    }

    @Test
    void getIpAddr_returnsEmptyStringWhenRemoteAddrNull() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader(Mockito.anyString())).thenReturn(null);
        Mockito.when(request.getRemoteAddr()).thenReturn(null);

        String ip = IpUtil.getIpAddr(request);

        Assertions.assertEquals("", ip);
    }
}
