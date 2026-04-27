package com.raf.framework.web.servlet;

import com.raf.framework.core.common.RafConstant;
import com.raf.framework.core.common.exception.BusinessException;
import com.raf.framework.core.common.exception.InfrastructureException;
import com.raf.framework.core.common.exception.ProtocolException;
import com.raf.framework.core.common.exception.SystemException;
import com.raf.framework.core.common.result.RafResponseEnum;
import com.raf.framework.core.common.result.RafResult;

import java.nio.file.AccessDeniedException;
import java.sql.SQLException;
import java.util.Optional;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Global exception handler.
 *
 * @author Jerry
 * @since 2019-01-01
 */
@Slf4j
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@RestControllerAdvice
public class GlobalExceptionConfig {

    // ================= Business / parameter exceptions (WARN, HTTP 200/4xx) =================

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public RafResult<?> handleBusinessException(BusinessException e) {
        log.warn("BusinessException: code={}, msg={}", e.getCode(), e.getMsg());
        return RafResult.fail(e);
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public RafResult<?> handleNotFoundException(HttpServletRequest request) {
        log.warn("Resource not found (404): {}", request.getRequestURI());
        return RafResult.fail(RafResponseEnum.NOT_FOUND);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.OK)
    public RafResult<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String msg = getBindingErrorMsg(e.getBindingResult());
        log.warn("Validation failed: {}", msg);
        return RafResult.fail(RafResponseEnum.PARAM_ERROR, msg);
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.OK)
    public RafResult<?> handleBindException(BindException e) {
        String msg = getBindingErrorMsg(e.getBindingResult());
        log.warn("Binding failed: {}", msg);
        return RafResult.fail(RafResponseEnum.PARAM_ERROR, msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.OK)
    public RafResult<?> handleConstraintViolationException(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        log.warn("Constraint violation: {}", msg);
        return RafResult.fail(RafResponseEnum.PARAM_ERROR);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.OK)
    public RafResult<?> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("Message not readable: {}", e.getMessage());
        return RafResult.fail(RafResponseEnum.PARAM_ERROR);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.OK)
    public RafResult<?> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("Argument type mismatch: param={}, msg={}", e.getName(), e.getMessage());
        return RafResult.fail(RafResponseEnum.PARAM_ERROR);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.OK)
    public RafResult<?> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.warn("Missing required parameter: {}", e.getParameterName());
        return RafResult.fail(RafResponseEnum.PARAM_ERROR);
    }

    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.OK)
    public RafResult<?> handleValidationException(ValidationException e) {
        log.warn("Validation exception: {}", e.getMessage());
        return RafResult.fail(RafResponseEnum.PARAM_ERROR);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public RafResult<?> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("Method not allowed: {}", e.getMessage());
        return RafResult.fail(RafResponseEnum.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public RafResult<?> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e) {
        log.warn("Media type not supported: {}", e.getMessage());
        return RafResult.fail(RafResponseEnum.UNSUPPORTED_MEDIA_TYPE);
    }

    @ExceptionHandler(ProtocolException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public RafResult<?> handleAuthException(ProtocolException e) {
        log.warn("Unauthorized: {}", e.getMessage());
        return RafResult.fail(RafResponseEnum.UNAUTHORIZED);
    }

    // ================= System exceptions (ERROR, HTTP 500) =================

    @ExceptionHandler(SQLException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RafResult<?> handleSqlException(SQLException e) {
        log.error("Database exception", e);
        return RafResult.fail(RafResponseEnum.SERVER_ERROR);
    }

    @ExceptionHandler(InfrastructureException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RafResult<?> handleInfrastructureException(InfrastructureException e) {
        log.error("Infrastructure exception: code={}, msg={}", e.getCode(), e.getMsg(), e);
        return RafResult.fail(RafResponseEnum.SERVER_ERROR);
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public RafResult<?> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("Access denied: {}", e.getMessage());
        return RafResult.fail(RafResponseEnum.FORBIDDEN);
    }

    /**
     * Catch-all exception handler.
     */
    @ExceptionHandler({Exception.class, Throwable.class, SystemException.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RafResult<?> handleException(HttpServletRequest request, Exception e, Object handler) {
        if (StringUtils.containsIgnoreCase(e.getClass().getName(), "ClientAbortException")) {
            log.warn("Client aborted connection: {}", e.getMessage());
            return null;
        }

        String method = Optional.ofNullable(handler)
                .filter(h -> h instanceof HandlerMethod)
                .map(h -> {
                    HandlerMethod hm = (HandlerMethod) h;
                    return hm.getBean().getClass().getName() + "#" + hm.getMethod().getName();
                })
                .orElse("unknown");

        log.error("Unhandled exception: uri={}, handler={}", request.getRequestURI(), method, e);
        return RafResult.fail(RafResponseEnum.SERVER_ERROR);
    }

    // ================= Private helpers =================

    private String getBindingErrorMsg(BindingResult bindingResult) {
        return bindingResult.getAllErrors().stream()
                .findFirst()
                .map(error -> {
                    if (error instanceof FieldError) {
                        FieldError fieldError = (FieldError) error;
                        String defaultMsg = StringUtils.defaultString(fieldError.getDefaultMessage());
                        if (defaultMsg.contains(RafConstant.EXCEPTION)) {
                            return fieldError.getField() + ":params error";
                        }
                        return defaultMsg;
                    }
                    return error.getDefaultMessage();
                })
                .orElse("Param validation error");
    }
}
