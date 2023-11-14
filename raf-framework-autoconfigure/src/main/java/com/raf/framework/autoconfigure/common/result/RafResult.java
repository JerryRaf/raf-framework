package com.raf.framework.autoconfigure.common.result;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;

/**
 * 通用返回结果包装
 *
 * @author Jerry
 * @date 2019/01/01
 */
@Data
public class RafResult<T> implements Serializable {
    private static final long serialVersionUID = -1;

    private int code;
    private String msg;
    private T data;

    @JsonIgnore
    public boolean isSuccess() {
        return RafResponseEnum.SUCCESS.getCode() == this.code;
    }

    public static <T> RafResult<T> success() {
        return success(null);
    }

    public static <T> RafResult<T> success(T t) {
        RafResult<T> r = new RafResult<>();
        r.setCode(RafResponseEnum.SUCCESS.getCode());
        r.setMsg(RafResponseEnum.SUCCESS.getMsg());
        r.setData(t);
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
        r.setCode(iResponseEnum.getCode());
        r.setMsg(iResponseEnum.getMsg());
        r.setData(t);
        return r;
    }

}
