package io.github.jerryraf.examples.web.common;

import com.raf.framework.autoconfigure.common.result.IResponseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * User module error codes.
 * Encoding: service(2) + module(2) + sequence(3)
 * User service: 20, user module: 01
 *
 * @author Jerry
 * @since 2026-04-17
 */
@Getter
@AllArgsConstructor
public enum UserErrorCode implements IResponseEnum {

    USER_NOT_FOUND(20001001, "User not found"),
    USER_ALREADY_EXISTS(20001002, "User already exists"),
    USER_DISABLED(20001003, "User is disabled"),
    INVALID_PASSWORD(20001004, "Invalid password"),
    PASSWORD_TOO_WEAK(20001005, "Password strength is insufficient");

    private final int code;
    private final String msg;
}
