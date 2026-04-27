package com.raf.framework.dubbo;

import com.raf.framework.core.common.RafConstant;
import com.raf.framework.dubbo.core.DubboFilterOrders;
import com.raf.framework.dubbo.core.DubboTraceLog;
import com.raf.framework.core.trace.ContextHolder;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;

/**
 * Dubbo 提供者链路追踪过滤器
 * <p>
 * 负责在服务端接收并设置 TraceId，实现分布式链路追踪
 * 支持通过注解控制日志打印级别
 * </p>
 */
@Slf4j
@Activate(group = CommonConstants.PROVIDER, order = DubboFilterOrders.PROVIDER_TRACE_FILTER)
public class ProviderTraceFilter extends AbstractTraceFilter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        long startTime = System.currentTimeMillis();

        // 从客户端传递的上下文中获取 TraceId
        String traceId = RpcContext.getServerAttachment().getAttachment(RafConstant.TRACE_ID);
        if (StringUtils.isNotEmpty(traceId)) {
            ContextHolder.setTraceId(traceId);
        }

        // 获取方法注解（带缓存），getMethodAnnotation 永远不返回 null
        DubboTraceLog traceLog = getMethodAnnotation(invoker, invocation);
        boolean logRequest = traceLog.providerReq();
        boolean logResponse = traceLog.providerRes();

        String address = getAddress(invoker);
        String methodName = getMethodName(invocation);

        try {
            logReq("Provider", address, methodName,
                    logRequest, invocation.getArguments());

            Result result = invoker.invoke(invocation);

            logRes("Provider", methodName,
                    System.currentTimeMillis() - startTime, logResponse,
                    result.getValue());

            return result;
        } catch (Exception e) {
            log.error("Provider invocation failed: method={} address={} error={}", methodName, address, e.getMessage());
            throw e;
        } finally {
            // 清理 TraceId，防止线程池复用时污染
            ContextHolder.clearTraceId();
            // 清理 RpcContext
            RpcContext.removeServerContext();
        }
    }
}