package com.raf.framework.dubbo;

import com.raf.framework.core.common.RafConstant;
import com.raf.framework.core.trace.ContextHolder;
import com.raf.framework.dubbo.core.DubboFilterOrders;
import com.raf.framework.dubbo.core.DubboTraceLog;

import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;

/**
 * Dubbo 消费者链路追踪过滤器
 * <p>
 * 负责在消费端进行链路追踪，传递 TraceId 到服务端
 * 支持通过注解控制日志打印级别
 * </p>
 */
@Slf4j
@Activate(group = CommonConstants.CONSUMER, order = DubboFilterOrders.CONSUMER_TRACE_FILTER)
public class ConsumerTraceFilter extends AbstractTraceFilter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        // 获取或生成 TraceId
        long startTime = System.currentTimeMillis();
        String traceId = ContextHolder.getOrSetTraceId();
        RpcContext.getClientAttachment().setAttachment(RafConstant.TRACE_ID, traceId);

        // 获取方法注解（带缓存），getMethodAnnotation 永远不返回 null
        DubboTraceLog traceLog = getMethodAnnotation(invoker, invocation);
        boolean logRequest = traceLog.consumerReq();
        boolean logResponse = traceLog.consumerRes();

        String address = getAddress(invoker);
        String methodName = getMethodName(invocation);

        try {
            logReq("Consumer", address, methodName, logRequest, invocation.getArguments());

            Result result = invoker.invoke(invocation);

            logRes("Consumer", methodName,
                    System.currentTimeMillis() - startTime, logResponse,
                    result.getValue());
            return result;
        } catch (Exception e) {
            log.error("Consumer invocation failed: method={} address={} error={}", methodName, address, e.getMessage());
            throw e;
        } finally {
            // 清理 RpcContext，防止上下文泄漏（consumer 侧只清理 client context）
            RpcContext.removeContext();
        }
    }
}