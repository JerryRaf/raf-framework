package com.raf.framework.dubbo.core;

/**
 * Dubbo 过滤器顺序常量
 * <p>
 * Provider 侧执行顺序（order 从小到大）：
 * 100 CustGenericFilter → 200 ProviderTraceFilter → 300 ValidFilter → 400 CustExceptionFilter
 * </p>
 */
public class DubboFilterOrders {
    /**
     * 泛化调用类型推导过滤器（Provider 侧）
     * 必须最先执行，为后续 Filter 提供正确的参数类型
     */
    public static final int CUST_GENERIC_FILTER = 100;

    /**
     * Provider 侧链路追踪过滤器
     * 注入 traceId 到 ContextHolder，记录请求/响应日志
     */
    public static final int PROVIDER_TRACE_FILTER = 200;

    /**
     * 参数校验过滤器（Provider 侧）
     * 在 traceId 已注入后执行，校验日志可携带 traceId
     */
    public static final int VALID_FILTER = 300;

    /**
     * 异常包装过滤器（Provider 侧）
     * 必须在所有业务 Filter 之后执行，捕获并包装异常
     */
    public static final int CUST_EXCEPTION_FILTER = 400;

    /**
     * Consumer 侧链路追踪过滤器
     * 传播 traceId 到下游服务
     */
    public static final int CONSUMER_TRACE_FILTER = 10;

    /**
     * 私有构造器，防止实例化工具类
     */
    private DubboFilterOrders() {
        throw new UnsupportedOperationException("Utility class");
    }
}