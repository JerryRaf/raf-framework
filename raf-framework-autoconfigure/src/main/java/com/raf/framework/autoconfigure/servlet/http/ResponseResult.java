package com.raf.framework.autoconfigure.servlet.http;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author Jerry
 * @date 2020/05/07
 */
@Retention(RUNTIME)
@Target({TYPE, ElementType.METHOD})
@Documented
public @interface ResponseResult {
}