package com.raf.framework.core.common.exception.base;

import com.raf.framework.core.common.result.IResponseEnum;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;

/**
 * 异常基础类-禁止外部调用
 */
@Getter
public abstract class BaseException extends RuntimeException {
    protected int code = -1;
    protected String msg;

    /**
     * 保留原始枚举引用，方便在if (ex.getResponseEnum() == GatewayErrorEnum.TOKEN_INVALID)
     * 必须加 transient 防止 RPC 序列化失败
     * 必须加 @JsonIgnore 防止 JSON 序列化冗余
     */
    @JsonIgnore
    private transient IResponseEnum responseEnum;

    public BaseException(IResponseEnum responseEnum) {
        super(responseEnum.getMsg());
        this.responseEnum = responseEnum;
        this.code = responseEnum.getCode();
        this.msg = responseEnum.getMsg();
    }

    // 覆盖：使用枚举的code，但使用自定义 msg
    public BaseException(IResponseEnum responseEnum, String msg) {
        super(msg);
        this.responseEnum = responseEnum;
        this.code = responseEnum.getCode();
        this.msg = msg;
    }

    public BaseException(int code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }

    //使用枚举，保留异常
    public BaseException(IResponseEnum responseEnum, Throwable cause) {
        super(responseEnum.getMsg(), cause);
        this.responseEnum = responseEnum;
        this.code = responseEnum.getCode();
        this.msg = responseEnum.getMsg();
    }

    //不使用枚举，保留异常
    public BaseException(int code, String msg, Throwable cause) {
        super(msg, cause);
        this.code = code;
        this.msg = msg;
    }
}