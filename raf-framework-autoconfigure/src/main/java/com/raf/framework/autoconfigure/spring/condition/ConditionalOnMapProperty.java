package com.raf.framework.autoconfigure.spring.condition;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.*;

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