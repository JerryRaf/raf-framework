package com.raf.framework.autoconfigure.spring.valid;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * @author Jerry
 * @date 2019/01/01
 */
public class LongPositiveValidator implements ConstraintValidator<LongPositive, Long> {

    @Override
    public boolean isValid(Long value, ConstraintValidatorContext context) {
        if (null == value) {
            return false;
        }
        return value > 0;
    }
}