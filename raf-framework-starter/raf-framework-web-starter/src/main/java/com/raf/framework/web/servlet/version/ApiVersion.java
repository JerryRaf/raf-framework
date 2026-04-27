package com.raf.framework.web.servlet.version;

import java.lang.annotation.*;
import org.springframework.web.bind.annotation.Mapping;

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