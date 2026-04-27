package com.raf.framework.core.common.exception;

import com.raf.framework.core.common.exception.base.BaseException;
import com.raf.framework.core.common.result.RafResponseEnum;

/**
 * JVM 级系统异常
 *
 * <p>仅用于 JVM 内部不可恢复错误，例如：反射调用失败、类加载异常、序列化失败等。
 * 与 {@link InfrastructureException} 的区别：
 * <ul>
 *   <li>{@code SystemException}：JVM 内部错误，与外部中间件无关</li>
 *   <li>{@code InfrastructureException}：外部中间件不可用（Redis/Dubbo/ES 等）</li>
 * </ul>
 *
 * <p>此类异常需保留完整堆栈信息用于排查问题。
 *
 * @author Jerry
 * @since 2019-01-01
 */
public class SystemException extends BaseException {

    /**
     * 包装 JVM 内部原始异常，使用默认 SERVER_ERROR 错误码
     *
     * @param cause 原始异常
     */
    public SystemException(Throwable cause) {
        super(RafResponseEnum.SERVER_ERROR.getCode(), "System Error", cause);
    }

    /**
     * 包装 JVM 内部原始异常，附带自定义描述
     *
     * @param msg   错误描述
     * @param cause 原始异常
     */
    public SystemException(String msg, Throwable cause) {
        super(RafResponseEnum.SERVER_ERROR.getCode(), msg, cause);
    }
}