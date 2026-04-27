package com.raf.framework.dubbo;

import com.raf.framework.core.common.exception.BusinessException;
import com.raf.framework.core.common.result.RafResponseEnum;
import com.raf.framework.core.spring.bean.SpringContext;
import com.raf.framework.dubbo.core.DubboFilterOrders;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.groups.Default;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

/**
 * Dubbo provider parameter validation filter.
 *
 * @author Jerry
 * @since 2019-01-01
 */
@Slf4j
@Activate(group = CommonConstants.PROVIDER, order = DubboFilterOrders.VALID_FILTER)
public class ValidFilter implements Filter {

    // Validator lifecycle is static; filter is singleton, cache is process-wide.
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();
    private static final Map<MethodSignature, CachedMethod> METHOD_CACHE = new ConcurrentHashMap<>();

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        try {
            CachedMethod cachedMethod = getTargetMethod(invoker, invocation);
            if (cachedMethod != null && cachedMethod.isRequiresValidation()) {
                validateParameters(invoker, invocation, cachedMethod.getMethod());
            }
            return invoker.invoke(invocation);
        } catch (ValidationException ex) {
            BusinessException businessException = buildErrorResult(ex);
            return AsyncRpcResult.newDefaultAsyncResult(null, businessException, invocation);
        }
    }

    /**
     * Resolve and cache target method metadata.
     */
    private CachedMethod getTargetMethod(Invoker<?> invoker, Invocation invocation) {
        MethodSignature signature = new MethodSignature(
                invoker.getInterface(),
                invocation.getMethodName(),
                invocation.getParameterTypes());

        return METHOD_CACHE.computeIfAbsent(signature, key -> {
            try {
                Method method = invoker.getInterface()
                        .getMethod(invocation.getMethodName(), invocation.getParameterTypes());
                boolean requiresValidation = hasValidAnnotation(method);
                return new CachedMethod(method, requiresValidation);
            } catch (NoSuchMethodException e) {
                log.warn("Method not found: {}", signature);
                return null;
            }
        });
    }

    /**
     * Execute bean validation on Dubbo method parameters.
     */
    private void validateParameters(Invoker<?> invoker, Invocation invocation, Method method) {
        Object bean = SpringContext.getBean(invoker.getInterface());
        Set<ConstraintViolation<Object>> violations = VALIDATOR.forExecutables()
                .validateParameters(bean, method, invocation.getArguments(), Default.class);
        if (!violations.isEmpty()) {
            throw new ValidationException("Parameter validation failed", violations);
        }
    }

    /**
     * Build business exception from validation violations.
     */
    private BusinessException buildErrorResult(ValidationException ex) {
        String errorMsg = ex.getViolations().stream().map(v -> {
            String field = v.getPropertyPath().toString();
            int lastDot = field.lastIndexOf('.');
            if (lastDot != -1) {
                field = field.substring(lastDot + 1);
            }
            return String.format("%s: %s", field, v.getMessage());
        }).collect(Collectors.joining("; "));

        log.warn("Parameter validation failed: {}", errorMsg);
        return new BusinessException(RafResponseEnum.PARAM_ERROR, errorMsg);
    }

    /**
     * Method signature cache key.
     * Arrays are compared by content, not by reference.
     */
    private static final class MethodSignature {
        private final Class<?> interfaceClass;
        private final String methodName;
        private final Class<?>[] parameterTypes;

        MethodSignature(Class<?> interfaceClass, String methodName, Class<?>[] parameterTypes) {
            this.interfaceClass = interfaceClass;
            this.methodName = methodName;
            this.parameterTypes = parameterTypes;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof MethodSignature)) {
                return false;
            }
            MethodSignature that = (MethodSignature) o;
            return Objects.equals(interfaceClass, that.interfaceClass)
                    && Objects.equals(methodName, that.methodName)
                    && Arrays.equals(parameterTypes, that.parameterTypes);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(interfaceClass, methodName);
            result = 31 * result + Arrays.hashCode(parameterTypes);
            return result;
        }

        @Override
        public String toString() {
            return interfaceClass.getName() + "#" + methodName + Arrays.toString(parameterTypes);
        }
    }

    /**
     * Cached method metadata with validation flag.
     */
    @AllArgsConstructor
    private static final class CachedMethod {
        @Getter
        private final Method method;
        @Getter
        private final boolean requiresValidation;
    }

    /**
     * Internal validation exception carrying violations.
     */
    @Getter
    public static class ValidationException extends RuntimeException {
        private final transient Set<ConstraintViolation<Object>> violations;

        public ValidationException(String message, Set<ConstraintViolation<Object>> violations) {
            super(message);
            this.violations = violations;
        }
    }

    /**
     * Whether method parameters contain {@link Valid} annotation.
     */
    private boolean hasValidAnnotation(Method method) {
        for (Annotation[] annotations : method.getParameterAnnotations()) {
            for (Annotation annotation : annotations) {
                if (annotation.annotationType().equals(Valid.class)) {
                    return true;
                }
            }
        }
        return false;
    }
}
