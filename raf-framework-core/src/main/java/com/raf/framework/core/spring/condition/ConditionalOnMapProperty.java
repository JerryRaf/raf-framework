package com.raf.framework.core.spring.condition;

import java.lang.annotation.*;
import org.springframework.context.annotation.Conditional;

/**
 * @author Jerry
 * @date 2019/01/01
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Documented
@Conditional(MapPropertyCondition.class)
public @interface ConditionalOnMapProperty {
    String prefix();
}