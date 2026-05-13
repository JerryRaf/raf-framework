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

            // 检查是否显式跳过包装（优先级最高）
            if (clazz.isAnnotationPresent(SkipResponseWrap.class)
                    || method.isAnnotationPresent(SkipResponseWrap.class)) {
                request.setAttribute(ResponseResultConfig.SKIP_RESPONSE_WRAP, Boolean.TRUE);
            } else {
                // 默认对所有 HandlerMethod 开启响应包装（无需 @ResponseResult 注解）
                // 向后兼容：@ResponseResult 显式标注时行为不变
                request.setAttribute(ResponseResultConfig.RESPONSE_RESULT, Boolean.TRUE);
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