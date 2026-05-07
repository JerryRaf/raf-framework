package com.raf.framework.core.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 框架级全局错误码枚举
 *
 * <p>编码规范：XXYYY（5位纯数字）
 * <ul>
 *   <li>成功：{@code 0}（全局保留，代表 00000）</li>
 *   <li>前 2 位（XX）= 服务/模块分配码，全局公共模块统一使用 {@code 10}</li>
 *   <li>后 3 位（YYY）= 具体错误流水号，尽量与 HTTP 状态码语义对齐</li>
 * </ul>
 *
 * <p>服务编号字典（完整表见架构规范文档）：
 * <pre>
 *   10 → Raf-framework  (10001-10999)
 * </pre>
 *
 * @author Jerry
 * @since 2019-01-01
 */
@Getter
@AllArgsConstructor
public enum RafResponseEnum implements IResponseEnum {

    // ── 成功
    SUCCESS(0, "success"),

    // ── 4xx 客户端错误（前端处理，后端不报警）
    UNAUTHORIZED(10401, "您的登录信息缺失，请重试！"),
    FORBIDDEN(10403, "您的权限不足，请核对！"),
    NOT_FOUND(10404, "访问资源不存在，请核对！"),
    METHOD_NOT_ALLOWED(10405, "HTTP 方法错误，请核对！"),
    UNSUPPORTED_MEDIA_TYPE(10415, "不支持的媒体类型，请核对！"),
    UPGRADE_REQUIRED(10426, "请升级协议！"),
    TOO_MANY_REQUESTS(10429, "访问次数过于频繁，请稍后再试！"),

    // ── 5xx 服务端错误（触发监控告警）
    SERVER_ERROR(10500, "服务出错，请稍后再试！"),

    // ── 升级 / 维护 ────
    FORCE_UPDATE(10601, "请更新 App 版本！"),
    SERVICE_UPDATE(10602, "服务升级中，请稍后再试！"),

    // ── 参数 / 协议错误 
    PARAM_ERROR(10700, "参数错误！"),
    HTTP_ERROR(10800, "请求第三方接口出错！");


    private final int code;
    private final String msg;
}
