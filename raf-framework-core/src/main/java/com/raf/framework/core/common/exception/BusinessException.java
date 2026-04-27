package com.raf.framework.core.common.exception;

import com.raf.framework.core.common.exception.base.BaseException;
import com.raf.framework.core.common.result.IResponseEnum;

/**
 * 业务异常类
 * <p>
 * 用于表示业务逻辑层面的异常情况，例如：参数校验失败、业务规则不满足等
 * 不应该用于系统级错误（如数据库连接失败、第三方服务不可用等）
 * </p>
 * <p>
 * 性能优化：重写 fillInStackTrace() 返回 this，避免填充堆栈信息
 * 因为业务异常通常是预期的，不需要完整的堆栈跟踪，这样可以提升性能
 * </p>
 */
public class BusinessException extends BaseException {
    public BusinessException(IResponseEnum responseEnum) {
        super(responseEnum);
    }

    public BusinessException(IResponseEnum responseEnum, String msg) {
        super(responseEnum, msg);
    }

    /**
     * 性能优化：不填充堆栈信息
     * 业务异常是预期的异常，不需要堆栈跟踪，返回 this 可显著提升性能
     */
    @Override
    public Throwable fillInStackTrace() {
        return this;
    }
}