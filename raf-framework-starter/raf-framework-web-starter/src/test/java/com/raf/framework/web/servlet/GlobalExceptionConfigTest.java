package com.raf.framework.web.servlet;

import com.raf.framework.core.common.exception.BusinessException;
import com.raf.framework.core.common.exception.InfrastructureException;
import com.raf.framework.core.common.exception.ProtocolException;
import com.raf.framework.core.common.exception.SystemException;
import com.raf.framework.core.common.result.RafResponseEnum;
import com.raf.framework.core.common.result.RafResult;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.nio.file.AccessDeniedException;
import java.sql.SQLException;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for GlobalExceptionConfig.
 *
 * @author Jerry
 * @since 2026-04-29
 */
class GlobalExceptionConfigTest {

    private GlobalExceptionConfig config;

    @BeforeEach
    void setUp() {
        config = new GlobalExceptionConfig();
    }

    // ─── BusinessException ───────────────────────────────────────────────────

    @Test
    void shouldReturnBusinessFailureResult() {
        BusinessException exception = new BusinessException(RafResponseEnum.PARAM_ERROR);

        RafResult<?> result = config.handleBusinessException(exception);

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(RafResponseEnum.PARAM_ERROR.getCode());
        assertThat(result.getMsg()).isEqualTo(RafResponseEnum.PARAM_ERROR.getMsg());
    }

    // ─── Not Found ───────────────────────────────────────────────────────────

    @Test
    void shouldReturnNotFoundFailureResult() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/not-exist");

        RafResult<?> result = config.handleNotFoundException(request);

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(RafResponseEnum.NOT_FOUND.getCode());
    }

    // ─── MethodArgumentNotValidException ─────────────────────────────────────

    @Test
    void handleMethodArgumentNotValidException_returnsParamError() throws Exception {
        // Build a BindingResult with one FieldError
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "name", "must not be blank"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        RafResult<?> result = config.handleMethodArgumentNotValidException(ex);

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(RafResponseEnum.PARAM_ERROR.getCode());
    }

    @Test
    void handleMethodArgumentNotValidException_withExceptionInMessage_returnsFieldParamsError() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        // message containing "Exception" triggers the special branch
        bindingResult.addError(new FieldError("target", "amount", "NumberFormatException occurred"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        RafResult<?> result = config.handleMethodArgumentNotValidException(ex);

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(RafResponseEnum.PARAM_ERROR.getCode());
    }

    // ─── BindException ───────────────────────────────────────────────────────

    @Test
    void handleBindException_returnsParamError() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "age", "must be positive"));

        BindException ex = new BindException(bindingResult);

        RafResult<?> result = config.handleBindException(ex);

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(RafResponseEnum.PARAM_ERROR.getCode());
    }

    @Test
    void handleBindException_withNoErrors_returnsDefaultMsg() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        BindException ex = new BindException(bindingResult);

        RafResult<?> result = config.handleBindException(ex);

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(RafResponseEnum.PARAM_ERROR.getCode());
    }

    // ─── ConstraintViolationException ────────────────────────────────────────

    @Test
    void handleConstraintViolationException_returnsParamError() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("size must be between 1 and 50");

        ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));

        RafResult<?> result = config.handleConstraintViolationException(ex);

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(RafResponseEnum.PARAM_ERROR.getCode());
    }

    // ─── HttpMessageNotReadableException ─────────────────────────────────────

    @Test
    void handleHttpMessageNotReadableException_returnsParamError() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("Required request body is missing");

        RafResult<?> result = config.handleHttpMessageNotReadableException(ex);

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(RafResponseEnum.PARAM_ERROR.getCode());
    }

    // ─── MethodArgumentTypeMismatchException ─────────────────────────────────

    @Test
    void handleMethodArgumentTypeMismatchException_returnsParamError() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("id");
        when(ex.getMessage()).thenReturn("Failed to convert value");

        RafResult<?> result = config.handleMethodArgumentTypeMismatchException(ex);

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(RafResponseEnum.PARAM_ERROR.getCode());
    }

    // ─── MissingServletRequestParameterException ─────────────────────────────

    @Test
    void handleMissingServletRequestParameterException_returnsParamError() {
        MissingServletRequestParameterException ex =
                new MissingServletRequestParameterException("userId", "Long");

        RafResult<?> result = config.handleMissingServletRequestParameterException(ex);

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(RafResponseEnum.PARAM_ERROR.getCode());
    }

    // ─── ValidationException ─────────────────────────────────────────────────

    @Test
    void handleValidationException_returnsParamError() {
        ValidationException ex = new ValidationException("validation failed");

        RafResult<?> result = config.handleValidationException(ex);

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(RafResponseEnum.PARAM_ERROR.getCode());
    }

    // ─── HttpRequestMethodNotSupportedException ───────────────────────────────

    @Test
    void handleHttpRequestMethodNotSupportedException_returnsMethodNotAllowed() {
        HttpRequestMethodNotSupportedException ex =
                new HttpRequestMethodNotSupportedException("DELETE");

        RafResult<?> result = config.handleHttpRequestMethodNotSupportedException(ex);

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(RafResponseEnum.METHOD_NOT_ALLOWED.getCode());
    }

    // ─── HttpMediaTypeNotSupportedException ──────────────────────────────────

    @Test
    void handleHttpMediaTypeNotSupportedException_returnsUnsupportedMediaType() {
        HttpMediaTypeNotSupportedException ex =
                new HttpMediaTypeNotSupportedException("text/plain");

        RafResult<?> result = config.handleHttpMediaTypeNotSupportedException(ex);

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(RafResponseEnum.UNSUPPORTED_MEDIA_TYPE.getCode());
    }

    // ─── ProtocolException ───────────────────────────────────────────────────

    @Test
    void handleAuthException_returnsUnauthorized() {
        ProtocolException ex = new ProtocolException(RafResponseEnum.UNAUTHORIZED);

        RafResult<?> result = config.handleAuthException(ex);

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(RafResponseEnum.UNAUTHORIZED.getCode());
    }

    // ─── SQLException ────────────────────────────────────────────────────────

    @Test
    void handleSqlException_returnsServerError() {
        SQLException ex = new SQLException("connection refused");

        RafResult<?> result = config.handleSqlException(ex);

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(RafResponseEnum.SERVER_ERROR.getCode());
    }

    // ─── InfrastructureException ─────────────────────────────────────────────

    @Test
    void handleInfrastructureException_returnsServerError() {
        InfrastructureException ex = new InfrastructureException("redis timeout");

        RafResult<?> result = config.handleInfrastructureException(ex);

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(RafResponseEnum.SERVER_ERROR.getCode());
    }

    // ─── AccessDeniedException ───────────────────────────────────────────────

    @Test
    void handleAccessDeniedException_returnsForbidden() {
        AccessDeniedException ex = new AccessDeniedException("/admin/users");

        RafResult<?> result = config.handleAccessDeniedException(ex);

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(RafResponseEnum.FORBIDDEN.getCode());
    }

    // ─── Catch-all handleException ───────────────────────────────────────────

    @Test
    void handleException_withGenericException_returnsServerError() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");

        RafResult<?> result = config.handleException(request, new RuntimeException("unexpected"), null);

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(RafResponseEnum.SERVER_ERROR.getCode());
    }

    @Test
    void handleException_withSystemException_returnsServerError() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");

        RafResult<?> result = config.handleException(request, new SystemException(new RuntimeException("system error")), null);

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(RafResponseEnum.SERVER_ERROR.getCode());
    }

    @Test
    void handleException_withClientAbortException_returnsNull() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/stream");

        // The check is: StringUtils.containsIgnoreCase(e.getClass().getName(), "ClientAbortException")
        // Our inner class is named ClientAbortException so its getName() contains the keyword
        Exception fakeAbort = new ClientAbortException("pipe broken");

        RafResult<?> result = config.handleException(request, fakeAbort, null);

        Assertions.assertNull(result);
    }

    /**
     * Inner class named ClientAbortException so getClass().getName() contains "ClientAbortException".
     */
    static class ClientAbortException extends RuntimeException {
        ClientAbortException(String msg) {
            super(msg);
        }
    }

    @Test
    void handleException_withNullHandler_usesUnknownMethod() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");

        RafResult<?> result = config.handleException(request, new IllegalStateException("bad state"), null);

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(RafResponseEnum.SERVER_ERROR.getCode());
    }
}
