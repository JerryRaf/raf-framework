package com.raf.framework.core.common.exception;

import com.raf.framework.core.common.exception.base.BaseException;
import com.raf.framework.core.common.result.IResponseEnum;
import com.raf.framework.core.common.result.RafResponseEnum;

/**
 * 基础设施异常
 *
 * <p>用于表示基础设施层面的不可预期异常，例如：Redis 连接失败、Dubbo 超时、ES 不可用等。
 * 与 {@link BusinessException} 不同，此类异常需要保留完整堆栈信息用于排查问题。
 *
 * <p>错误码必须通过 {@link IResponseEnum} 枚举传入，禁止直接传入 int code，
 * 以确保全局错误码可统一管理和追踪。
 *
 * @author Jerry
 * @since 2019-01-01
 */
public class InfrastructureException extends BaseException {

    /**
     * 推荐：使用枚举 + 原始异常构造，保留完整上下文
     */
    public InfrastructureException(IResponseEnum responseEnum, Throwable cause) {
        super(responseEnum, cause);
    }

    /**
     * 使用枚举构造（无原始异常）
     */
    public InfrastructureException(IResponseEnum responseEnum) {
        super(responseEnum);
    }

    /**
     * 使用枚举 code，自定义 msg，保留原始异常
     * 适用于需要补充详细错误上下文的场景
     */
    public InfrastructureException(IResponseEnum responseEnum, String msg, Throwable cause) {
        super(responseEnum.getCode(), msg, cause);
    }

    /**
     * 使用枚举 code，自定义 msg（无原始异常）
     */
    public InfrastructureException(IResponseEnum responseEnum, String msg) {
        super(responseEnum.getCode(), msg);
    }

    /**
     * 使用自定义 msg，默认 SERVER_ERROR(10500) 错误码，保留原始异常
     */
    public InfrastructureException(String msg, Throwable cause) {
        super(RafResponseEnum.SERVER_ERROR.getCode(), msg, cause);
    }

    /**
     * 使用自定义 msg，默认 SERVER_ERROR(10500) 错误码
     */
    public InfrastructureException(String msg) {
        super(RafResponseEnum.SERVER_ERROR.getCode(), msg);
    }
}