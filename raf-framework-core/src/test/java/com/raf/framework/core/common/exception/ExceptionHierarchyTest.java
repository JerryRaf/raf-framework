package com.raf.framework.core.common.exception;

import com.raf.framework.core.common.result.RafResponseEnum;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for the exception hierarchy: BusinessException, InfrastructureException,
 * SystemException, ProtocolException.
 *
 * @author Jerry
 * @since 2026-04-29
 */
class ExceptionHierarchyTest {

    // ─── BusinessException ──────────────────────────────────────────────────

    @Test
    void businessException_constructsWithEnum() {
        BusinessException ex = new BusinessException(RafResponseEnum.PARAM_ERROR);

        Assertions.assertEquals(RafResponseEnum.PARAM_ERROR.getCode(), ex.getCode());
        Assertions.assertEquals(RafResponseEnum.PARAM_ERROR.getMsg(), ex.getMsg());
        Assertions.assertEquals(RafResponseEnum.PARAM_ERROR, ex.getResponseEnum());
    }

    @Test
    void businessException_constructsWithEnumAndCustomMsg() {
        BusinessException ex = new BusinessException(RafResponseEnum.PARAM_ERROR, "用户名不能为空");

        Assertions.assertEquals(RafResponseEnum.PARAM_ERROR.getCode(), ex.getCode());
        Assertions.assertEquals("用户名不能为空", ex.getMsg());
    }

    @Test
    void businessException_fillInStackTrace_returnsSelf() {
        BusinessException ex = new BusinessException(RafResponseEnum.PARAM_ERROR);
        // Performance optimization: fillInStackTrace should return this (no stack fill)
        Assertions.assertSame(ex, ex.fillInStackTrace());
    }

    // ─── InfrastructureException ─────────────────────────────────────────────

    @Test
    void infrastructureException_constructsWithEnum() {
        InfrastructureException ex = new InfrastructureException(RafResponseEnum.HTTP_ERROR);

        Assertions.assertEquals(RafResponseEnum.HTTP_ERROR.getCode(), ex.getCode());
        Assertions.assertEquals(RafResponseEnum.HTTP_ERROR.getMsg(), ex.getMsg());
    }

    @Test
    void infrastructureException_constructsWithEnumAndCause() {
        RuntimeException cause = new RuntimeException("Redis timeout");
        InfrastructureException ex = new InfrastructureException(RafResponseEnum.SERVER_ERROR, cause);

        Assertions.assertEquals(RafResponseEnum.SERVER_ERROR.getCode(), ex.getCode());
        Assertions.assertSame(cause, ex.getCause());
    }

    @Test
    void infrastructureException_constructsWithEnumMsgAndCause() {
        RuntimeException cause = new RuntimeException("ES unavailable");
        InfrastructureException ex = new InfrastructureException(
                RafResponseEnum.SERVER_ERROR, "ES 集群不可用", cause);

        Assertions.assertEquals(RafResponseEnum.SERVER_ERROR.getCode(), ex.getCode());
        Assertions.assertEquals("ES 集群不可用", ex.getMsg());
        Assertions.assertSame(cause, ex.getCause());
    }

    @Test
    void infrastructureException_constructsWithEnumAndCustomMsg() {
        InfrastructureException ex = new InfrastructureException(RafResponseEnum.SERVER_ERROR, "自定义消息");

        Assertions.assertEquals(RafResponseEnum.SERVER_ERROR.getCode(), ex.getCode());
        Assertions.assertEquals("自定义消息", ex.getMsg());
    }

    @Test
    void infrastructureException_constructsWithMsgOnly() {
        InfrastructureException ex = new InfrastructureException("Dubbo 调用超时");

        Assertions.assertEquals(RafResponseEnum.SERVER_ERROR.getCode(), ex.getCode());
        Assertions.assertEquals("Dubbo 调用超时", ex.getMsg());
    }

    @Test
    void infrastructureException_constructsWithMsgAndCause() {
        RuntimeException cause = new RuntimeException("timeout");
        InfrastructureException ex = new InfrastructureException("Dubbo 调用超时", cause);

        Assertions.assertEquals(RafResponseEnum.SERVER_ERROR.getCode(), ex.getCode());
        Assertions.assertSame(cause, ex.getCause());
    }

    // ─── SystemException ────────────────────────────────────────────────────

    @Test
    void systemException_constructsWithCause() {
        RuntimeException cause = new RuntimeException("reflection error");
        SystemException ex = new SystemException(cause);

        Assertions.assertEquals(RafResponseEnum.SERVER_ERROR.getCode(), ex.getCode());
        Assertions.assertSame(cause, ex.getCause());
    }

    @Test
    void systemException_constructsWithMsgAndCause() {
        RuntimeException cause = new RuntimeException("class load error");
        SystemException ex = new SystemException("类加载失败", cause);

        Assertions.assertEquals(RafResponseEnum.SERVER_ERROR.getCode(), ex.getCode());
        Assertions.assertEquals("类加载失败", ex.getMsg());
        Assertions.assertSame(cause, ex.getCause());
    }

    // ─── ProtocolException ──────────────────────────────────────────────────

    @Test
    void protocolException_constructsWithEnum() {
        ProtocolException ex = new ProtocolException(RafResponseEnum.UNAUTHORIZED);

        Assertions.assertEquals(RafResponseEnum.UNAUTHORIZED.getCode(), ex.getCode());
        Assertions.assertEquals(RafResponseEnum.UNAUTHORIZED.getMsg(), ex.getMsg());
    }

    @Test
    void protocolException_constructsWithEnumAndCustomMsg() {
        ProtocolException ex = new ProtocolException(RafResponseEnum.UNAUTHORIZED, "Token 已过期");

        Assertions.assertEquals(RafResponseEnum.UNAUTHORIZED.getCode(), ex.getCode());
        Assertions.assertEquals("Token 已过期", ex.getMsg());
    }

    @Test
    void protocolException_constructsWithEnumAndCause() {
        RuntimeException cause = new RuntimeException("signature mismatch");
        ProtocolException ex = new ProtocolException(RafResponseEnum.UNAUTHORIZED, cause);

        Assertions.assertEquals(RafResponseEnum.UNAUTHORIZED.getCode(), ex.getCode());
        Assertions.assertSame(cause, ex.getCause());
    }

    @Test
    void protocolException_fillInStackTrace_returnsSelf() {
        ProtocolException ex = new ProtocolException(RafResponseEnum.UNAUTHORIZED);
        Assertions.assertSame(ex, ex.fillInStackTrace());
    }
}
