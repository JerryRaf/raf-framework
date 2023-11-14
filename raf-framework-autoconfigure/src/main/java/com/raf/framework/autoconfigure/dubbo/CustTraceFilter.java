package com.raf.framework.autoconfigure.dubbo;

import cn.hutool.core.util.IdUtil;
import com.raf.framework.autoconfigure.common.RafConstant;
import com.raf.framework.autoconfigure.jackson.Json;
import com.raf.framework.autoconfigure.spring.bean.SpringContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;
import org.slf4j.MDC;

/**
 * dubbo调用 链路追踪traceId传输以及日志记录
 *
 * @author Jerry
 * @date 2019/01/01 12:00
 */
@Slf4j
@Activate(group = {CommonConstants.PROVIDER, CommonConstants.CONSUMER}, order = -10000)
public class CustTraceFilter implements Filter {
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        long startTime = System.currentTimeMillis();
        RpcContext rpcContext = RpcContext.getContext();
        Json json = SpringContext.getBean(Json.class);
        String traceId;
        String logStr = StringUtils.EMPTY;

        if (rpcContext.isConsumerSide()) {
            traceId = MDC.get(RafConstant.TRACE_ID);
            if (traceId == null) {
                traceId = IdUtil.fastSimpleUUID();
            }
            rpcContext.setAttachment(RafConstant.TRACE_ID, traceId);

            logStr = String.format("dubbo request [%s][%s]:%s", getAddress(invoker), getMethod(invocation), json.objToString(invocation.getArguments()));
        } else {
            traceId = rpcContext.getAttachment(RafConstant.TRACE_ID);
            MDC.put(RafConstant.TRACE_ID, traceId);
        }

        Result result = invoker.invoke(invocation);
        long timeCost = System.currentTimeMillis() - startTime;

        if (rpcContext.isConsumerSide()) {
            logStr = String.format("%s res [%sms]:%s", logStr,timeCost, json.objToString(result.getValue()));
        } else {
            logStr = String.format("Process dubbo request [%s] in [%sms]", getMethod(invocation), timeCost);
        }

        log.info(logStr);
        return result;
    }

    private String getAddress(Invoker invoker) {
        return invoker.getUrl().getHost() + ":" + invoker.getUrl().getPort();
    }

    private String getMethod(Invocation invocation) {
        return invocation.getInvoker() == null || invocation.getInvoker().getInterface() == null ?
                invocation.getAttachment("path") :
                invocation.getInvoker().getInterface().getName() + "." + invocation.getMethodName();
    }
}
