package com.raf.framework.core.common.result;

import com.raf.framework.core.common.exception.base.BaseException;
import com.raf.framework.core.trace.ContextHolder;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

/**
 * 通用返回结果包装
 *
 * <p>响应结构遵循 RESTful API 规范：HTTP 状态码表达网络层状态，body code 表达业务状态。
 * 所有响应均携带 traceId 以支持全链路追踪。
 *
 * <p>设计约束：
 * <ul>
 *   <li>仅通过静态工厂方法构造，禁止外部直接 new 或调用 setter</li>
 *   <li>错误码必须通过 {@link IResponseEnum} 枚举传入，禁止直接传入 int code</li>
 * </ul>
 *
 * @author Jerry
 * @since 2019-01-01
 */
@Getter
public class RafResult<T> implements Serializable {
    private static final long serialVersionUID = -1;

    private final int code;
    private final String msg;
    private final T data;

    /** 全链路追踪 ID，对接 SkyWalking / ELK 日志系统 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String traceId;

    private RafResult(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
        this.traceId = ContextHolder.getTraceId();
    }

    @JsonIgnore
    public boolean isSuccess() {
        return RafResponseEnum.SUCCESS.getCode() == this.code;
    }

    public static <T> RafResult<T> success() {
        return success(null);
    }

    public static <T> RafResult<T> success(T t) {
        return new RafResult<>(RafResponseEnum.SUCCESS.getCode(), RafResponseEnum.SUCCESS.getMsg(), t);
    }

    public static <T> RafResult<T> fail() {
        return fail(RafResponseEnum.SERVER_ERROR);
    }

    public static <T> RafResult<T> fail(IResponseEnum iResponseEnum) {
        return fail(iResponseEnum, null);
    }

    public static <T> RafResult<T> fail(IResponseEnum iResponseEnum, T t) {
        return new RafResult<>(iResponseEnum.getCode(), iResponseEnum.getMsg(), t);
    }

    /**
     * 从 BaseException 构造失败响应，保留异常中的 code 和 msg
     *
     * @param ex 框架异常基类
     */
    public static <T> RafResult<T> fail(BaseException ex) {
        return new RafResult<>(ex.getCode(), ex.getMsg(), null);
    }
}
