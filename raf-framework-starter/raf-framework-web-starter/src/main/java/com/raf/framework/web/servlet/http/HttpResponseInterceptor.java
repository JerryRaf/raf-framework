package com.raf.framework.web.servlet.http;

import com.raf.framework.core.common.RafConstant;
import com.raf.framework.core.trace.ContextHolder;

import java.lang.reflect.Method;
import cn.hutool.core.util.IdUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * @author Jerry
 * @date 2020/05/07
 */
@Slf4j
public class HttpResponseInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod) {
            final HandlerMethod handlerMethod = (HandlerMethod) handler;
            final Class<?> clazz = handlerMethod.getBeanType();
            final Method method = handlerMethod.getMethod();
            if (clazz.isAnnotationPresent(ResponseResult.class)) {
                request.setAttribute(ResponseResultConfig.RESPONSE_RESULT, clazz.getAnnotation(ResponseResult.class));
            } else if (method.isAnnotationPresent(ResponseResult.class)) {
                request.setAttribute(ResponseResultConfig.RESPONSE_RESULT, method.getAnnotation(ResponseResult.class));
            }
        }

        String traceId = request.getHeader(RafConstant.TRACE_ID);
        if (StringUtils.isEmpty(traceId)) {
            traceId = IdUtil.fastSimpleUUID();
        }

        MDC.put(RafConstant.TRACE_ID, traceId);
        ContextHolder.setTraceId(traceId);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        ContextHolder.clearContext();
        MDC.remove(RafConstant.TRACE_ID);
    }
}