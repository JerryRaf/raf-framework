package com.raf.framework.autoconfigure.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 框架错误码枚举
 * httpStatus:200-500
 * 框架级别：501-1000
 * 业务异常：10000以上
 *
 * @author Jerry
 * @date 2019/01/01
 */
@Getter
@AllArgsConstructor
public enum RafResponseEnum implements IResponseEnum {
    /**
     * 系统通用返回枚举
     */
    SUCCESS(200, "成功！"),
    UNAUTHORIZED(401, "您的登陆信息缺失，请重试！"),
    FORBIDDEN(403, "您的权限不足，请核对！"),
    NOT_FOUND(404, "访问资源不存在，请核对！"),
    METHOD_NOT_ALLOWED(405, "http方法错误，请核对！"),
    UNSUPPORTED_MEDIA_TYPE(415, "不支持的媒体类型，请核对！"),
    UPGRADE_REQUIRED(426, "请升级协议！"),
    TOO_MANY_REQUESTS(429, "访问次数过于频繁，请稍后再试！"),

    SERVER_ERROR(500, "服务出错，请稍后再试！"),
    FEIGN_ERROR(500, "调用超时，请稍后再试！"),

    SIGN_ERROR_600(600, "签名错误，请核对！"),
    FORCE_UPDATE_603(603, "请更新app版本！"),
    SERVICE_UPDATE_605(605, "服务升级中，请稍后再试！"),

    PARAM_ERROR_700(700, "参数错误！"),
    HTTP_ERROR_800(800, "请求第三方接口出错！");

    private final int code;
    private final String msg;
}
