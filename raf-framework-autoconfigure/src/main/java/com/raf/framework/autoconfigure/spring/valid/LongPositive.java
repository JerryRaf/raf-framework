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
@Constraint(validatedBy = LongPositiveValidator.class)
public @interface LongPositive {
    String message() default "请输入合法id！";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
