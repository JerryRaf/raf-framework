package com.raf.framework.autoconfigure.spring.condition;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Map;

/**
 * @author Jerry
 * @date 2019/01/01
 */
public class ValuesPropertyCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Map<String, Object> annotationAttributes = metadata.getAnnotationAttributes(ConditionalOnPropertyForValues.class.getName());
        String propertyName = (String) annotationAttributes.get("value");
        String[] values = (String[]) annotationAttributes.get("havingValues");
        String propertyValue = context.getEnvironment().getProperty(propertyName);
        for (String havingValue : values) {
            if (propertyValue.equalsIgnoreCase(havingValue)) {
                return true;
            }
        }
        return false;
    }
}