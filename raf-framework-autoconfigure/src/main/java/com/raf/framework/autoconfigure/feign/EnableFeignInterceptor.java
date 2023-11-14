package com.raf.framework.autoconfigure.feign;

import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Jerry
 * @date 2019/01/01
 * Feign调用传递用户信息
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({FeignInterceptorConfig.class})
public @interface EnableFeignInterceptor {

}
