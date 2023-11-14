package com.raf.framework.autoconfigure.servlet.http;

import cn.hutool.core.util.IdUtil;
import com.raf.framework.autoconfigure.common.RafConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

/**
 * @author Jerry
 * @date 2020/05/07
 */
@Slf4j
@Component
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

        return true;
    }

}