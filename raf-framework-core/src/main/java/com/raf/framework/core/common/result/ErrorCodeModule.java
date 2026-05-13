package com.raf.framework.core.common.result;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 错误码模块标注注解。
 *
 * <p>标注在实现 {@link IResponseEnum} 的枚举类上，声明该模块的错误码前缀。
 * 框架启动时会扫描所有 {@link IResponseEnum} 实现，检测 code 冲突并 fail-fast。
 *
 * <p>使用示例：
 * <pre>{@code
 * @ErrorCodeModule(prefix = 101, description = "订单模块")
 * public enum OrderErrorEnum implements IResponseEnum {
 *     ORDER_NOT_FOUND(101001, "订单不存在"),
 *     ORDER_ALREADY_PAID(101002, "订单已支付");
 *
 *     private final int code;
 *     private final String msg;
 *     // ...
 * }
 * }</pre>
 *
 * <p>注意：本注解是可选的，不加注解的枚举仍然正常工作（向后兼容）。
 * 冲突检测对所有 {@link IResponseEnum} 实现类生效，与是否标注本注解无关。
 *
 * @author Jerry
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface ErrorCodeModule {

    /**
     * 模块前缀，用于文档说明，不影响实际 code 值。
     * <p>建议各模块分配不同前缀，如：101=订单，102=用户，103=支付。
     */
    int prefix();

    /**
     * 模块描述。
     */
    String description() default "";
}
