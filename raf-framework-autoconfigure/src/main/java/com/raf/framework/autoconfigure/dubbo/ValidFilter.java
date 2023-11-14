package com.raf.framework.autoconfigure.dubbo;

import com.google.common.collect.Maps;
import com.raf.framework.autoconfigure.common.RafConstant;
import com.raf.framework.autoconfigure.common.result.RafResponseEnum;
import com.raf.framework.autoconfigure.common.result.RafResult;
import com.raf.framework.autoconfigure.spring.bean.SpringContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.executable.ExecutableValidator;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

/**
 * dubbo rpc参数校验
 *
 * @author Jerry
 * @date 2019/01/01 12:00
 */
@Slf4j
@Activate(group = {CommonConstants.PROVIDER}, order = -100)
public class ValidFilter implements Filter {

    private static ExecutableValidator executableValidator = Validation.buildDefaultValidatorFactory().getValidator().forExecutables();

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        Method method = getMethod(invoker, invocation);
        if (method == null) {
            return invoker.invoke(invocation);
        }

        Class<?> inf = invoker.getInterface();
        Object object = SpringContext.getBean(inf);
        Object[] paramList = invocation.getArguments();
        Set<ConstraintViolation<Object>> constraintViolations = executableValidator.validateParameters(object, method, paramList);
        if (constraintViolations.isEmpty()) {
            return invoker.invoke(invocation);
        }

        RafResult<?> response = getValidationResult(constraintViolations);
        return AsyncRpcResult.newDefaultAsyncResult(response, null, invocation);
    }

    /**
     * 获取校验方法
     */
    private static Method getMethod(Invoker<?> invoker, Invocation invocation) {
        Method[] methods = invoker.getInterface().getDeclaredMethods();
        for (Method m : methods) {
            boolean needCheck = m.getName().equals(invocation.getMethodName()) && invocation.getArguments().length == m.getParameterCount();
            if (needCheck) {
                if (matchMethod(invocation.getParameterTypes(), m.getParameterTypes())) {
                    return m;
                }
            }
        }
        return null;
    }

    /**
     * 获取匹配的方法
     */
    private static boolean matchMethod(Class[] invokerMethodParamClassList, Class[] matchMethodParamClassList) {
        for (int i = 0; i < invokerMethodParamClassList.length; i++) {
            if (!invokerMethodParamClassList[i].equals(matchMethodParamClassList[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * 校验结果转换返回对象
     */
    private static <T> RafResult<?> getValidationResult(Set<ConstraintViolation<T>> set) {
        Map<String, String> errorMsg = Maps.newHashMap();
        for (ConstraintViolation<T> violation : set) {
            String propertyPath=violation.getPropertyPath().toString();
            errorMsg.put(propertyPath.substring(propertyPath.lastIndexOf(RafConstant.DOT)+1), violation.getMessage());
        }
        return RafResult.fail(RafResponseEnum.PARAM_ERROR_700, errorMsg);
    }

}