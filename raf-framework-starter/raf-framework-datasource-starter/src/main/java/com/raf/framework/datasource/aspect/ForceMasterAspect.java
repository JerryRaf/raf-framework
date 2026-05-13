package com.raf.framework.datasource.aspect;

import com.raf.framework.datasource.DataSourceContextHolder;
import com.raf.framework.datasource.annotation.ForceMaster;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;

/**
 * {@link ForceMaster} 注解 AOP 切面。
 *
 * <p>{@code @Order(0)} 确保在 {@link com.raf.framework.datasource.DsAspect}（Order=1）之前执行，
 * 优先设置主库上下文，防止被覆盖。
 *
 * @author Jerry
 */
@Slf4j
@Aspect
@Order(0)
public class ForceMasterAspect {

    @Around("@annotation(forceMaster) || @within(forceMaster)")
    public Object around(ProceedingJoinPoint point, ForceMaster forceMaster) throws Throwable {
        log.debug("ForceMaster: forcing master datasource for {}", point.getSignature());
        DataSourceContextHolder.setDB(DataSourceContextHolder.DEFAULT_DS);
        try {
            return point.proceed();
        } finally {
            DataSourceContextHolder.clearDB();
        }
    }
}
