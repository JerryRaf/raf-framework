package com.raf.framework.datasource;

import java.lang.annotation.*;

/**
 * DataSource Selector Annotation - marks methods or classes to use a specific datasource
 *
 * @author Jerry
 * @since 2026-04-20
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Inherited
public @interface DsSelector {

    /**
     * Datasource key, defaults to primary datasource
     *
     * @return datasource key
     */
    String value() default DataSourceContextHolder.DEFAULT_DS;
}
