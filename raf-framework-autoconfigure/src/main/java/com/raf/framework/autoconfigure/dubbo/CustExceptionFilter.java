package com.raf.framework.autoconfigure.dubbo;

import com.raf.framework.autoconfigure.common.exception.BusinessException;
import com.raf.framework.autoconfigure.common.result.RafResponseEnum;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.*;
import org.apache.dubbo.rpc.service.GenericService;

import java.lang.reflect.Method;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_FILTER_VALIDATION_EXCEPTION;


@Activate(group = CommonConstants.PROVIDER)
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

                // directly throw if it's checked exception
                if (!(exception instanceof RuntimeException) && (exception instanceof Exception)) {
                    return;
                }
                // directly throw if the exception appears in the signature
                try {
                    Method method = invoker.getInterface().getMethod(invocation.getMethodName(), invocation.getParameterTypes());
                    Class<?>[] exceptionClasses = method.getExceptionTypes();
                    for (Class<?> exceptionClass : exceptionClasses) {
                        if (exception.getClass().equals(exceptionClass)) {
                            return;
                        }
                    }
                } catch (NoSuchMethodException e) {
                    return;
                }

                //自定义异常直接返回
                if (exception instanceof BusinessException) {
                    return;
                }

                // for the exception not found in method's signature, print ERROR message in server's log.
                logger.error(CONFIG_FILTER_VALIDATION_EXCEPTION, "", "", "Got unchecked and undeclared exception which called by " + RpcContext.getServiceContext().getRemoteHost() + ". service: " + invoker.getInterface().getName() + ", method: " + invocation.getMethodName() + ", exception: " + exception.getClass().getName() + ": " + exception.getMessage(), exception);

                appResponse.setException(new BusinessException(RafResponseEnum.SERVER_ERROR));
            } catch (Throwable e) {
                logger.warn(CONFIG_FILTER_VALIDATION_EXCEPTION, "", "", "Fail to ExceptionFilter when called by " + RpcContext.getServiceContext().getRemoteHost() + ". service: " + invoker.getInterface().getName() + ", method: " + invocation.getMethodName() + ", exception: " + e.getClass().getName() + ": " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void onError(Throwable e, Invoker<?> invoker, Invocation invocation) {
        logger.error(CONFIG_FILTER_VALIDATION_EXCEPTION, "", "", "Got unchecked and undeclared exception which called by " + RpcContext.getServiceContext().getRemoteHost() + ". service: " + invoker.getInterface().getName() + ", method: " + invocation.getMethodName() + ", exception: " + e.getClass().getName() + ": " + e.getMessage(), e);
    }

}