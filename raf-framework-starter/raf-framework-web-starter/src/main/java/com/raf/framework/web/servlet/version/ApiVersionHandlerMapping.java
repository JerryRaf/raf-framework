package com.raf.framework.web.servlet.version;

import java.lang.reflect.Method;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.condition.RequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * API version handler mapping.
 * Resolves {@code {version}} placeholders in {@link RequestMapping} path using
 * the value from {@link ApiVersion}, without relying on reflection to mutate
 * annotation internals.
 *
 * @author Jerry
 * @since 2019-01-01
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
        RequestMappingInfo methodInfo = createRequestMappingInfo(method, null);
        if (methodInfo == null) {
            return null;
        }

        ApiVersion apiVersion = AnnotatedElementUtils.findMergedAnnotation(method, ApiVersion.class);
        if (apiVersion == null) {
            apiVersion = AnnotatedElementUtils.findMergedAnnotation(handlerType, ApiVersion.class);
        }

        RequestMappingInfo typeInfo = createRequestMappingInfo(handlerType, apiVersion);
        if (typeInfo != null) {
            methodInfo = typeInfo.combine(methodInfo);
        }
        return methodInfo;
    }

    private RequestMappingInfo createRequestMappingInfo(Object element, ApiVersion apiVersion) {
        RequestMapping requestMapping = (element instanceof Class<?>)
                ? AnnotatedElementUtils.findMergedAnnotation((Class<?>) element, RequestMapping.class)
                : AnnotatedElementUtils.findMergedAnnotation((Method) element, RequestMapping.class);

        if (requestMapping == null) {
            return null;
        }

        RequestCondition<?> condition = (element instanceof Class<?>)
                ? getCustomTypeCondition((Class<?>) element)
                : getCustomMethodCondition((Method) element);

        if (apiVersion != null) {
            String versionSegment = "v" + apiVersion.value();
            String[] resolvedPaths = resolvePaths(requestMapping.path(), versionSegment);
            String[] resolvedValues = resolvePaths(requestMapping.value(), versionSegment);

            // Build a new RequestMappingInfo with resolved paths instead of mutating annotation internals
            RequestMappingInfo.BuilderConfiguration options = new RequestMappingInfo.BuilderConfiguration();
            options.setPatternParser(getPatternParser());

            return RequestMappingInfo
                    .paths(resolvedPaths.length > 0 ? resolvedPaths : resolvedValues)
                    .methods(requestMapping.method())
                    .params(requestMapping.params())
                    .headers(requestMapping.headers())
                    .consumes(requestMapping.consumes())
                    .produces(requestMapping.produces())
                    .mappingName(requestMapping.name())
                    .customCondition(condition)
                    .options(options)
                    .build();
        }

        return createRequestMappingInfo(requestMapping, condition);
    }

    private String[] resolvePaths(String[] paths, String versionSegment) {
        String[] resolved = new String[paths.length];
        for (int i = 0; i < paths.length; i++) {
            resolved[i] = paths[i].replace("{version}", versionSegment);
        }
        return resolved;
    }
}
