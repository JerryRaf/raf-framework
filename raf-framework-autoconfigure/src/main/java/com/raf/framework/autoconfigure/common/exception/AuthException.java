package com.raf.framework.autoconfigure.common.exception;

import com.raf.framework.autoconfigure.common.result.RafResponseEnum;
import lombok.Getter;

/**
 * 组件异常
 *
 * @author Jerry
 * @date 2019/01/01
 */
@Getter
public class AuthException extends RuntimeException {
    private static final long serialVersionUID=-1;
    private int code = RafResponseEnum.UNAUTHORIZED.getCode();
    private String msg;

    public AuthException() {
        super(RafResponseEnum.UNAUTHORIZED.getMsg());
        this.msg = RafResponseEnum.UNAUTHORIZED.getMsg();
    }

    public AuthException(int code, String msg) {
        super(msg);
        this.msg = msg;
        this.code = code;
    }

    public AuthException(String msg, Throwable e) {
        super(msg, e);
        this.msg = msg;
    }

    public AuthException(int code, String msg, Throwable e) {
        super(msg, e);
        this.msg = msg;
        this.code = code;
    }
}
