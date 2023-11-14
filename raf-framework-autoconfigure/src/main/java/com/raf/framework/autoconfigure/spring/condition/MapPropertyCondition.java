package com.raf.framework.autoconfigure.spring.condition;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Map;

/**
 * @author Jerry
 * @date 2019/01/01
 */
public class MapPropertyCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Map<String, Object> annotationAttributes = metadata.getAnnotationAttributes(ConditionalOnMapProperty.class.getName());
        String propertyName = (String) annotationAttributes.get("prefix");

        Map target = Binder.get(context.getEnvironment())
                .bind(propertyName, Map.class)
                .orElse(null);

        return null != target;
    }
}
