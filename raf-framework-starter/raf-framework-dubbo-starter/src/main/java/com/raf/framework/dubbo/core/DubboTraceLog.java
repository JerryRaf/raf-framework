package com.raf.framework.dubbo.core;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface DubboTraceLog {

    // 消费者入参日志 (默认开启)
    boolean consumerReq() default true;

    // 消费者结果日志 (默认关闭)
    boolean consumerRes() default false;

    // 提供者入参日志 (默认关闭)
    boolean providerReq() default false;

    // 提供者结果日志 (默认开启)
    boolean providerRes() default true;
}