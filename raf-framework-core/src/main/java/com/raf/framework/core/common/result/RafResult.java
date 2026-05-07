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

    private int code;
    private String msg;
    private T data;

    /** 全链路追踪 ID，对接 SkyWalking / ELK 日志系统 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String traceId;

    private RafResult() {
    }

    @JsonIgnore
    public boolean isSuccess() {
        return RafResponseEnum.SUCCESS.getCode() == this.code;
    }

    public static <T> RafResult<T> success() {
        return success(null);
    }

    public static <T> RafResult<T> success(T t) {
        RafResult<T> r = new RafResult<>();
        r.code = RafResponseEnum.SUCCESS.getCode();
        r.msg = RafResponseEnum.SUCCESS.getMsg();
        r.data = t;
        r.traceId = ContextHolder.getTraceId();
        return r;
    }

    public static <T> RafResult<T> fail() {
        return fail(RafResponseEnum.SERVER_ERROR);
    }

    public static <T> RafResult<T> fail(IResponseEnum iResponseEnum) {
        return fail(iResponseEnum, null);
    }

    public static <T> RafResult<T> fail(IResponseEnum iResponseEnum, T t) {
        RafResult<T> r = new RafResult<>();
        r.code = iResponseEnum.getCode();
        r.msg = iResponseEnum.getMsg();
        r.data = t;
        r.traceId = ContextHolder.getTraceId();
        return r;
    }

    /**
     * 从 BaseException 构造失败响应，保留异常中的 code 和 msg
     *
     * @param ex 框架异常基类
     */
    public static <T> RafResult<T> fail(BaseException ex) {
        RafResult<T> r = new RafResult<>();
        r.code = ex.getCode();
        r.msg = ex.getMsg();
        r.traceId = ContextHolder.getTraceId();
        return r;
    }
}
