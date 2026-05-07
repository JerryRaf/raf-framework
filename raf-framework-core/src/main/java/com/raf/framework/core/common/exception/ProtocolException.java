package com.raf.framework.core.common.exception;

import com.raf.framework.core.common.exception.base.BaseException;
import com.raf.framework.core.common.result.IResponseEnum;

/**
 * 协议层安全异常
 *
 * <p>用于：验签失败、解密失败、防重放攻击、Token 无效等安全校验场景。
 * 属于预期内的异常，不填充堆栈信息以提升性能。
 *
 * @author Jerry
 * @since 2019-01-01
 */
public class ProtocolException extends BaseException {

    /**
     * 推荐：使用枚举构造，规范错误码
     */
    public ProtocolException(IResponseEnum responseEnum) {
        super(responseEnum);
    }

    /**
     * 使用枚举 code，自定义 msg（用于补充安全上下文信息）
     */
    public ProtocolException(IResponseEnum responseEnum, String msg) {
        super(responseEnum, msg);
    }

    /**
     * 包装原始异常（保留 cause，但不填充本异常堆栈）
     */
    public ProtocolException(IResponseEnum responseEnum, Throwable cause) {
        super(responseEnum, cause);
    }

    /**
     * 协议异常属于预期内异常，不填充堆栈信息以提升性能
     */
    @Override
    public Throwable fillInStackTrace() {
        return this;
    }
}