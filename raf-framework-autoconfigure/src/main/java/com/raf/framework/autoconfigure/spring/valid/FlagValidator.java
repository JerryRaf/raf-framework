package com.raf.framework.autoconfigure.spring.valid;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * @author Jerry
 * @date 2019/01/01
 */
public class FlagValidator implements ConstraintValidator<Flag, Object> {

    private String values;

    @Override
    public void initialize(Flag flag) {
        this.values = flag.values();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext constraintValidatorContext) {
        if (null == value) {
            return false;
        }

        String[] valueArray = values.split(",");
        boolean flag = false;
        for (String aValueArray : valueArray) {
            if (aValueArray.equals(value.toString())) {
                flag = true;
                break;
            }
        }
        return flag;
    }
}