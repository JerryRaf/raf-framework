package com.raf.framework.autoconfigure.servlet.version;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.condition.RequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.*;
import java.util.Map;

/**
 * @author Jerry
 * @date 2019/01/01
 */
public class ApiVersionHandlerMapping extends RequestMappingHandlerMapping {

    @Override
    protected RequestCondition<ApiVersionCondition> getCustomTypeCondition(Class<?> handlerType) {
        ApiVersion apiVersion = AnnotationUtils.findAnnotation(handlerType, ApiVersion.class);
        return createCondition(apiVersion);
    }

    @Override
    protected RequestCondition<ApiVersionCondition> getCustomMethodCondition(Method method) {
        ApiVersion apiVersion = AnnotationUtils.findAnnotation(method, ApiVersion.class);
        return createCondition(apiVersion);
    }

    private RequestCondition<ApiVersionCondition> createCondition(ApiVersion apiVersion) {
        return apiVersion == null ? null : new ApiVersionCondition(apiVersion.value());
    }

    @Override
    protected RequestMappingInfo getMappingForMethod(Method method, @Nullable Class<?> handlerType) {
        RequestMappingInfo info = this.createRequestMappingInfo(method, null);
        if (info != null) {
            ApiVersion apiVersion = AnnotatedElementUtils.findMergedAnnotation(method, ApiVersion.class);
            RequestMappingInfo typeInfo = this.createRequestMappingInfo(handlerType, apiVersion);
            if (typeInfo != null) {
                info = typeInfo.combine(info);
            }
        }
        return info;
    }

    @SuppressWarnings("unchecked")
    private RequestMappingInfo createRequestMappingInfo(AnnotatedElement element, ApiVersion apiVersion) {
        RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(element, RequestMapping.class);
        RequestCondition<?> condition = element instanceof Class ? this.getCustomTypeCondition((Class) element) : this.getCustomMethodCondition((Method) element);
        if (element instanceof Class && null != apiVersion && null != requestMapping) {
            try {
                // 动态修改RequestMapping注解的属性
                InvocationHandler invocationHandler = Proxy.getInvocationHandler(requestMapping);
                Field field = invocationHandler.getClass().getDeclaredField("valueCache");
                // SynthesizedAnnotationInvocationHandler的valueCache是私有变量，需要打开权限
                field.setAccessible(true);
                Map map = (Map) field.get(invocationHandler);
                String[] paths = new String[requestMapping.path().length];
                for (int i = 0; i < requestMapping.path().length; i++) {
                    paths[i] = requestMapping.path()[i].replace("{version}", "v".concat(String.valueOf(apiVersion.value())));
                }
                map.put("path", paths);
                String[] values = new String[requestMapping.value().length];
                for (int i = 0; i < requestMapping.value().length; i++) {
                    values[i] = requestMapping.value()[i].replace("{version}", "v".concat(String.valueOf(apiVersion.value())));
                }
                map.put("value", values);
                // 上面改了value和path是因为注解里@AliasFor，两者互为，不晓得其它地方有没有用到，所以都改了，以免其它问题
            } catch (Exception ex) {
                logger.error("api版本异常",ex);
            }
        }
        return requestMapping != null ? this.createRequestMappingInfo(requestMapping, condition) : null;
    }
}