package com.raf.framework.dubbo;

import com.raf.framework.core.common.exception.BusinessException;
import com.raf.framework.core.common.result.RafResponseEnum;
import com.raf.framework.dubbo.core.DubboFilterOrders;

import org.apache.dubbo.common.constants.CommonConstants;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_FILTER_VALIDATION_EXCEPTION;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.*;
import org.apache.dubbo.rpc.service.GenericService;
import org.apache.dubbo.rpc.support.RpcUtils;

/**
 * Dubbo provider global exception filter.
 * <p>
 * Exception handling strategy:
 * - checked exception: pass through as-is
 * - BusinessException: pass through (consumer can handle business errors)
 * - RpcException: pass through (Dubbo framework exception)
 * - other RuntimeException: mask stack trace, wrap as SERVER_ERROR
 * </p>
 *
 * @author Jerry
 * @since 2019-01-01
 */
@Activate(group = CommonConstants.PROVIDER, order = DubboFilterOrders.CUST_EXCEPTION_FILTER)
public class CustExceptionFilter implements Filter, Filter.Listener {

    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(CustExceptionFilter.class);

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        return invoker.invoke(invocation);
    }

    @Override
    public void onResponse(Result appResponse, Invoker<?> invoker, Invocation invocation) {
        if (appResponse.hasException() && GenericService.class != invoker.getInterface()) {
            try {
                Throwable exception = appResponse.getException();

                // checked exception: pass through
                if (!(exception instanceof RuntimeException) && (exception instanceof Exception)) {
                    return;
                }

                // BusinessException: pass through (consumer can handle)
                if (exception instanceof BusinessException) {
                    return;
                }

                // RpcException: pass through (Dubbo framework exception)
                if (exception instanceof RpcException) {
                    return;
                }

                // Unexpected exception: log ERROR, mask stack, wrap as SERVER_ERROR
                logger.error(
                        CONFIG_FILTER_VALIDATION_EXCEPTION,
                        "",
                        "",
                        "Got unchecked and undeclared exception which called by "
                                + RpcContext.getServiceContext().getRemoteHost() + ". service: "
                                + invoker.getInterface().getName() + ", method: " + RpcUtils.getMethodName(invocation)
                                + ", exception: "
                                + exception.getClass().getName() + ": " + exception.getMessage(),
                        exception);

                appResponse.setException(new BusinessException(RafResponseEnum.SERVER_ERROR));
            } catch (Throwable e) {
                logger.warn(
                        CONFIG_FILTER_VALIDATION_EXCEPTION,
                        "",
                        "",
                        "Fail to ExceptionFilter when called by "
                                + RpcContext.getServiceContext().getRemoteHost() + ". service: "
                                + invoker.getInterface().getName() + ", method: " + RpcUtils.getMethodName(invocation)
                                + ", exception: "
                                + e.getClass().getName() + ": " + e.getMessage(),
                        e);
            }
        }
    }

    @Override
    public void onError(Throwable e, Invoker<?> invoker, Invocation invocation) {
        logger.error(
                CONFIG_FILTER_VALIDATION_EXCEPTION,
                "",
                "",
                "Got unchecked and undeclared exception which called by "
                        + RpcContext.getServiceContext().getRemoteHost() + ". service: "
                        + invoker.getInterface().getName() + ", method: " + RpcUtils.getMethodName(invocation)
                        + ", exception: "
                        + e.getClass().getName() + ": " + e.getMessage(),
                e);
    }
}
