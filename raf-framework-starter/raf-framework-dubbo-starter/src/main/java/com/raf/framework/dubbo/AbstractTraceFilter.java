package com.raf.framework.dubbo;

import com.raf.framework.dubbo.core.DubboTraceLog;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;

/**
 * Dubbo 链路追踪过滤器抽象基类
 *
 * @author Jerry
 * @since 2024-01-01
 */
@Slf4j
public abstract class AbstractTraceFilter implements Filter {

    protected static final int INITIAL_CACHE_SIZE = 128;
    protected final Map<String, DubboTraceLog> methodCache = new ConcurrentHashMap<>(INITIAL_CACHE_SIZE);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 默认追踪日志配置（当方法未标注 @DubboTraceLog 注解时使用）
     * <p>
     * 默认行为：Consumer 打印请求参数，Provider 打印响应结果
     * </p>
     */
    private static final DubboTraceLog DEFAULT_TRACE_LOG = new DubboTraceLog() {
        @Override
        public Class<? extends Annotation> annotationType() {
            return DubboTraceLog.class;
        }

        @Override
        public boolean consumerReq() {
            return true;
        }

        @Override
        public boolean consumerRes() {
            return false;
        }

        @Override
        public boolean providerReq() {
            return false;
        }

        @Override
        public boolean providerRes() {
            return true;
        }
    };

    protected String buildCacheKey(Invoker<?> invoker, Invocation invocation) {
        return String.format("%s#%s#%d", invoker.getInterface()
                .getName(), invocation.getMethodName(), Arrays.hashCode(invocation.getParameterTypes()));
    }

    protected String getAddress(Invoker<?> invoker) {
        return invoker.getUrl().getHost() + ":" + invoker.getUrl().getPort();
    }

    protected String getMethodName(Invocation invocation) {
        return invocation.getMethodName();
    }

    protected DubboTraceLog getMethodAnnotation(Invoker<?> invoker, Invocation invocation) {
        String cacheKey = buildCacheKey(invoker, invocation);
        return methodCache.computeIfAbsent(cacheKey, k -> {
            try {
                Method method = invoker.getInterface()
                        .getMethod(invocation.getMethodName(), invocation.getParameterTypes());
                DubboTraceLog annotation = method.getAnnotation(DubboTraceLog.class);
                return annotation != null ? annotation : DEFAULT_TRACE_LOG;
            } catch (NoSuchMethodException e) {
                return DEFAULT_TRACE_LOG;
            }
        });
    }

    private static String toJsonString(Object obj) {
        if (obj == null) {
            return "null";
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }

    /**
     * Log dubbo request.
     *
     * @param role       consumer or provider
     * @param address    remote address
     * @param methodName method name
     * @param logArgs    whether to log arguments
     * @param args       method arguments
     */
    protected static void logReq(String role, String address, String methodName, boolean logArgs, Object[] args) {
        String template = "Dubbo {} req [{}][{}]";
        if (logArgs) {
            log.info(template + ",Args:{}", role, address, methodName, toJsonString(args));
        } else {
            log.info(template, role, address, methodName);
        }
    }

    /**
     * Log dubbo response.
     *
     * @param role       consumer or provider
     * @param methodName method name
     * @param costTime   cost time in ms
     * @param logResult  whether to log result
     * @param result     method result
     */
    protected static void logRes(String role, String methodName, long costTime, boolean logResult, Object result) {
        String template = "Dubbo {} res [{}],Cost:{}ms";
        if (logResult) {
            log.info(template + ",Result:{}", role, methodName, costTime, toJsonString(result));
        } else {
            log.info(template, role, methodName, costTime);
        }
    }
}
