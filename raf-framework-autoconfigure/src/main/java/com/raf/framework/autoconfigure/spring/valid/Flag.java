package com.raf.framework.autoconfigure.spring.valid;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

/**
 * @author Jerry
 * @date 2019/01/01
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Constraint(validatedBy = FlagValidator.class)
public @interface Flag {
    /**
     * 有效值多个使用','隔开,如：1,2
     */
    String values();

    String message() default "无效的值";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
