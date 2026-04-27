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
@Conditional(ValuesPropertyCondition.class)
public @interface ConditionalOnPropertyForValues {

    String value();

    String prefix() default "";

    String[] name() default {};

    String havingValue() default "";

    boolean matchIfMissing() default false;

    boolean relaxedNames() default true;

    String[] havingValues() default {};
}