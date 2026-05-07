package com.raf.framework.core.common.result;

import com.raf.framework.core.common.exception.BusinessException;
import com.raf.framework.core.common.exception.InfrastructureException;
import com.raf.framework.core.common.exception.SystemException;
import com.raf.framework.core.trace.ContextHolder;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for RafResult factory methods and exception hierarchy.
 *
 * @author Jerry
 * @since 2026-04-29
 */
class RafResultTest {

    @AfterEach
    void tearDown() {
        ContextHolder.clearContext();
    }

    // ─── success() ──────────────────────────────────────────────────────────

    @Test
    void success_noArg_returnsSuccessCodeWithNullData() {
        RafResult<Object> result = RafResult.success();

        Assertions.assertEquals(RafResponseEnum.SUCCESS.getCode(), result.getCode());
        Assertions.assertEquals(RafResponseEnum.SUCCESS.getMsg(), result.getMsg());
        Assertions.assertNull(result.getData());
        Assertions.assertTrue(result.isSuccess());
    }

    @Test
    void success_withData_returnsSuccessCodeWithData() {
        RafResult<String> result = RafResult.success("hello");

        Assertions.assertEquals(RafResponseEnum.SUCCESS.getCode(), result.getCode());
        Assertions.assertEquals("hello", result.getData());
        Assertions.assertTrue(result.isSuccess());
    }

    @Test
    void success_includesTraceIdWhenSet() {
        ContextHolder.setTraceId("test-trace-001");

        RafResult<Void> result = RafResult.success();

        Assertions.assertEquals("test-trace-001", result.getTraceId());
    }

    // ─── fail() ─────────────────────────────────────────────────────────────

    @Test
    void fail_noArg_returnsServerErrorCode() {
        RafResult<Object> result = RafResult.fail();

        Assertions.assertEquals(RafResponseEnum.SERVER_ERROR.getCode(), result.getCode());
        Assertions.assertFalse(result.isSuccess());
    }

    @Test
    void fail_withEnum_returnsCorrectCodeAndMsg() {
        RafResult<Object> result = RafResult.fail(RafResponseEnum.PARAM_ERROR);

        Assertions.assertEquals(RafResponseEnum.PARAM_ERROR.getCode(), result.getCode());
        Assertions.assertEquals(RafResponseEnum.PARAM_ERROR.getMsg(), result.getMsg());
        Assertions.assertFalse(result.isSuccess());
    }

    @Test
    void fail_withEnumAndData_returnsDataInResult() {
        RafResult<String> result = RafResult.fail(RafResponseEnum.PARAM_ERROR, "extra info");

        Assertions.assertEquals(RafResponseEnum.PARAM_ERROR.getCode(), result.getCode());
        Assertions.assertEquals("extra info", result.getData());
    }

    @Test
    void fail_withException_usesExceptionCodeAndMsg() {
        BusinessException ex = new BusinessException(RafResponseEnum.PARAM_ERROR, "自定义错误");

        RafResult<Object> result = RafResult.fail(ex);

        Assertions.assertEquals(RafResponseEnum.PARAM_ERROR.getCode(), result.getCode());
        Assertions.assertEquals("自定义错误", result.getMsg());
        Assertions.assertFalse(result.isSuccess());
    }

    // ─── RafResponseEnum ────────────────────────────────────────────────────

    @Test
    void rafResponseEnum_allEntriesHaveCodeAndMsg() {
        for (RafResponseEnum e : RafResponseEnum.values()) {
            Assertions.assertNotNull(e.getMsg(), "msg should not be null for " + e.name());
        }
    }

    @Test
    void rafResponseEnum_successCodeIsZero() {
        Assertions.assertEquals(0, RafResponseEnum.SUCCESS.getCode());
    }
}
