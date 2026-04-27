package com.raf.framework.core.spring.condition;

import java.util.Map;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * @author Jerry
 * @date 2019/01/01
 */
public class ValuesPropertyCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Map<String, Object> annotationAttributes =
                metadata.getAnnotationAttributes(ConditionalOnPropertyForValues.class.getName());
        String propertyName = (String) annotationAttributes.get("value");
        String[] values = (String[]) annotationAttributes.get("havingValues");
        String propertyValue = context.getEnvironment().getProperty(propertyName);
        if (propertyValue == null) {
            return false;
        }
        for (String havingValue : values) {
            if (havingValue.equalsIgnoreCase(propertyValue)) {
                return true;
            }
        }
        return false;
    }
}