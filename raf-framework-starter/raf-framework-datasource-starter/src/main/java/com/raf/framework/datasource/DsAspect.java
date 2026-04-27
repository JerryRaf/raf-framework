package com.raf.framework.datasource;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;

/**
 * AOP aspect for datasource switching based on @DsSelector annotation
 *
 * @author Jerry
 * @since 2026-04-20
 */
@Slf4j
@Aspect
@Order(1)
public class DsAspect {

    /**
     * Switch datasource based on @DsSelector annotation value
     *
     * @param point      the proceeding join point
     * @param dsSelector the DsSelector annotation
     * @return method result
     * @throws Throwable if method throws
     */
    @Around("@annotation(dsSelector)")
    public Object around(ProceedingJoinPoint point, DsSelector dsSelector) throws Throwable {
        String dsKey = dsSelector.value();
        log.debug("Switching datasource to: {}", dsKey);
        DataSourceContextHolder.setDB(dsKey);
        try {
            return point.proceed();
        } finally {
            DataSourceContextHolder.clearDB();
            log.debug("Cleared datasource context");
        }
    }
}
