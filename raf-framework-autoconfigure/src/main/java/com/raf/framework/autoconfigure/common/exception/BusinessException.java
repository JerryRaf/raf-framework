package com.raf.framework.autoconfigure.common.exception;

import com.raf.framework.autoconfigure.common.result.IResponseEnum;
import lombok.Getter;

import java.text.MessageFormat;

/**
 * 业务异常类
 *
 * @author Jerry
 * @date 2019/01/01
 */
@Getter
public class BusinessException extends RuntimeException {
    private static final long serialVersionUID = -1;

    private int code;
    private final String msg;
    private IResponseEnum responseCode;

    public BusinessException(IResponseEnum iResponseEnum) {
        super(iResponseEnum.getMsg());
        responseCode = iResponseEnum;
        this.code = iResponseEnum.getCode();
        this.msg = iResponseEnum.getMsg();
    }

    public BusinessException(String msg) {
        super(msg);
        this.msg = msg;
    }

    public BusinessException(int code, String msg) {
        super(msg);
        this.msg = msg;
        this.code = code;
    }

    public BusinessException(int code, String msg, Throwable e) {
        super(msg, e);
        this.msg = msg;
        this.code = code;
    }

    @Override
    public Throwable fillInStackTrace() {
        return this;
    }

    @Override
    public String toString() {
        return MessageFormat.format("code:[{0}],msg:[{1}]", this.getCode(), this.getMsg());
    }
}
