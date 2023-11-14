package com.raf.framework.autoconfigure.servlet.version;

import org.springframework.web.bind.annotation.Mapping;

import java.lang.annotation.*;

/**
 * @author Jerry
 * @date 2019/01/01
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Mapping
public @interface ApiVersion {

    /**
     * version
     */
    int value();
}