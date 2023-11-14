package com.raf.framework.autoconfigure.servlet;

import com.raf.framework.autoconfigure.common.RafConstant;
import com.raf.framework.autoconfigure.common.exception.AuthException;
import com.raf.framework.autoconfigure.common.exception.BusinessException;
import com.raf.framework.autoconfigure.common.exception.ComponentException;
import com.raf.framework.autoconfigure.common.exception.SystemException;
import com.raf.framework.autoconfigure.common.result.RafResponseEnum;
import com.raf.framework.autoconfigure.common.result.RafResult;
import com.raf.framework.autoconfigure.sentry.SentryConfig;
import com.raf.framework.autoconfigure.spring.bean.SpringContext;
import io.sentry.SentryClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
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

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.ValidationException;
import java.nio.file.AccessDeniedException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.stream.Collectors.*;

/**
 * 全局异常拦截
 *
 * @author Jerry
 * @date 2019/01/01
 */
@Slf4j
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@RestControllerAdvice
public class GlobalExceptionConfig {
    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public RafResult<?> handleBusinessException(BusinessException e) {
        log.warn("BusinessException {}", e.toString());
        return RafResult.fail(e.getResponseCode());
    }

    /**
     * json参数解析失败或者类型不对
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.OK)
    public RafResult<?> handHttpMessageNotReadableExceptionException(HttpMessageNotReadableException e) {
        log.warn(e.getMessage());
        return RafResult.fail(RafResponseEnum.PARAM_ERROR_700);
    }

    /**
     * 参数验证失败
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.OK)
    public RafResult<?> handMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.warn(e.getMessage());
        Map<String, String> errors = e.getBindingResult().getFieldErrors().stream()
                .collect(groupingBy(FieldError::getField,
                        mapping(FieldError::getDefaultMessage, joining(","))));

        return RafResult.fail(RafResponseEnum.PARAM_ERROR_700, errors);
    }

    /**
     * 参数类型错误
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.OK)
    public RafResult<?> handMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("MethodArgumentTypeMismatchException:{}", e.getMessage());
        return RafResult.fail(RafResponseEnum.PARAM_ERROR_700);
    }

    /**
     * 参数绑定失败
     */
    @ConditionalOnClass(BindException.class)
    @Configuration
    @RestControllerAdvice
    static class BindExceptionConfiguration {

        @ExceptionHandler(BindException.class)
        @ResponseStatus(HttpStatus.OK)
        public RafResult<?> handBindException(BindException e) {
            log.warn("BindException {}",BindExceptionHelper.firstErrorMessage(e.getBindingResult()));
            return RafResult.fail(RafResponseEnum.PARAM_ERROR_700);
        }

        static class BindExceptionHelper {
            static String firstErrorMessage(BindingResult bindingResult) {
                AtomicReference<String> msg = new AtomicReference<>("");
                Optional.of(bindingResult.getAllErrors()).orElseGet(ArrayList::new).stream()
                        .findFirst().ifPresent(c -> {
                    FieldError fieldError = ((FieldError) c);
                    String defaultMsg = Optional.ofNullable(fieldError.getDefaultMessage()).orElse(StringUtils.EMPTY);
                    if (defaultMsg.contains(RafConstant.EXCEPTION)) {
                        msg.set(fieldError.getField().concat(":params error"));
                    } else {
                        msg.set(defaultMsg);
                    }
                });
                return msg.get();
            }
        }
    }

    /**
     * 参数验证失败
     */
    @ConditionalOnClass(ConstraintViolationException.class)
    @Configuration
    @RestControllerAdvice
    static class ConstraintViolationExceptionConfiguration {

        @ExceptionHandler(ConstraintViolationException.class)
        @ResponseStatus(HttpStatus.OK)
        public RafResult<?> handleConstraintViolationException(ConstraintViolationException e) {
            log.warn("ConstraintViolationException {}",ConstraintViolationExceptionHelper.firstErrorMessage(e.getConstraintViolations()));
            return RafResult.fail(RafResponseEnum.PARAM_ERROR_700);
        }

        static class ConstraintViolationExceptionHelper {
            static String firstErrorMessage(Set<ConstraintViolation<?>> constraintViolations) {
                return Optional.ofNullable(constraintViolations).orElseGet(HashSet::new).stream()
                        .findFirst()
                        .map(ConstraintViolation::getMessage).orElse("");
            }
        }
    }

    /**
     * 参数验证失败
     */
    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.OK)
    public RafResult<?> handleValidationException(ValidationException e) {
        log.warn(e.getMessage());
        return RafResult.fail(RafResponseEnum.PARAM_ERROR_700);
    }

    /**
     * 参数验证失败
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.OK)
    public RafResult<?> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.warn(String.format("参数%s未传", e.getParameterName()));
        return RafResult.fail(RafResponseEnum.PARAM_ERROR_700);
    }


    /**
     * 404 - Not Found
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public RafResult<?> handleNoHandlerFoundException(NoHandlerFoundException e) {
        log.warn(e.getMessage());
        return RafResult.fail(RafResponseEnum.NOT_FOUND);
    }

    /**
     * 405 - Method Not Allowed
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public RafResult<?> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn(e.getMessage());
        return RafResult.fail(RafResponseEnum.METHOD_NOT_ALLOWED);
    }

    /**
     * 415 - Unsupported Media Type
     */
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public RafResult<?> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e) {
        log.warn(e.getMessage());
        return RafResult.fail(RafResponseEnum.UNSUPPORTED_MEDIA_TYPE);
    }

    @ConditionalOnClass(SQLException.class)
    @Configuration
    @RestControllerAdvice
    static class SqlExceptionConfiguration {

        @ExceptionHandler(SQLException.class)
        @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
        public RafResult<?> handleSqlException(SQLException e) {
            log.error("SQLException:", e);
            return RafResult.fail(RafResponseEnum.SERVER_ERROR);
        }
    }


    @ConditionalOnClass(ComponentException.class)
    @Configuration
    @RestControllerAdvice
    static class ComponentExceptionConfiguration {
        @ExceptionHandler(ComponentException.class)
        @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
        public RafResult<?> handlerOkHttpException(ComponentException e) {
            log.error("ComponentException,code:{},msg:{}", e.getCode(), e.getMsg());
            return RafResult.fail(RafResponseEnum.SERVER_ERROR);
        }
    }


    @ConditionalOnClass(AccessDeniedException.class)
    @Configuration
    @RestControllerAdvice
    static class AccessDeniedExceptionConfiguration {
        @ExceptionHandler(AccessDeniedException.class)
        @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
        public RafResult<?> handAccessDeniedException(AccessDeniedException e) {
            log.error("AccessDeniedException", e);
            return RafResult.fail(RafResponseEnum.SERVER_ERROR);
        }
    }

    @ExceptionHandler(AuthException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public RafResult<?> handleAuthException(Throwable e) {
        log.error("AuthException", e);
        return RafResult.fail(RafResponseEnum.UNAUTHORIZED);
    }


    @ExceptionHandler(Throwable.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RafResult<?> handleThrowable(Throwable e) {
        log.error("Throwable", e);
        return RafResult.fail(RafResponseEnum.SERVER_ERROR);
    }

    @ExceptionHandler(SystemException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RafResult<?> handleSystemException(SystemException e) {
        log.error("SystemException", e);
        return RafResult.fail(RafResponseEnum.SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RafResult<?> handleException(HttpServletRequest httpServletRequest, Exception e, Object handler) {
        // 解决Broken pipe异常文本太长
        String value = "org.apache.catalina.connector.ClientAbortException";
        if (value.equals(e.getClass().getName())) {
            log.warn(e.getMessage());
            return null;
        }
        String message;
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            message = String.format("接口 [%s] 出现异常，方法：%s.%s，异常摘要：%s",
                    httpServletRequest.getRequestURI(),
                    handlerMethod.getBean().getClass().getName(),
                    handlerMethod.getMethod().getName(),
                    e.getMessage());
        } else {
            message = e.getMessage();
        }

        if (SpringContext.containsBean(SentryConfig.SENTRY_CLIENT)) {
            SpringContext.getBean(SentryConfig.SENTRY_CLIENT, SentryClient.class).sendException(e);
        }
        log.error(message, e);
        return RafResult.fail(RafResponseEnum.SERVER_ERROR);
    }

}
