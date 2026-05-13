package com.raf.framework.web.servlet.http;

import com.raf.framework.core.common.result.RafResult;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * @author Jerry
 * @date 2020/05/07
 */
@Slf4j
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ControllerAdvice
public class ResponseResultConfig implements ResponseBodyAdvice<Object> {

    public static final String RESPONSE_RESULT = "RESPONSE-RESULT";
    public static final String SKIP_RESPONSE_WRAP = "SKIP-RESPONSE-WRAP";

    @Override
    public boolean supports(MethodParameter methodParameter, Class<? extends HttpMessageConverter<?>> aClass) {
        ServletRequestAttributes servletRequestAttributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (servletRequestAttributes == null) {
            return false;
        }
        HttpServletRequest request = servletRequestAttributes.getRequest();
        // 显式跳过包装（@SkipResponseWrap）
        if (Boolean.TRUE.equals(request.getAttribute(SKIP_RESPONSE_WRAP))) {
            return false;
        }
        // 默认全局生效：只要 preHandle 设置了标记就包装
        return Boolean.TRUE.equals(request.getAttribute(RESPONSE_RESULT));
    }

    @Override
    public Object beforeBodyWrite(
            Object o,
            MethodParameter methodParameter,
            MediaType mediaType,
            Class<? extends HttpMessageConverter<?>> aClass,
            ServerHttpRequest serverHttpRequest,
            ServerHttpResponse serverHttpResponse) {
        if (o instanceof RafResult) {
            return o;
        }
        return RafResult.success(o);
    }
}