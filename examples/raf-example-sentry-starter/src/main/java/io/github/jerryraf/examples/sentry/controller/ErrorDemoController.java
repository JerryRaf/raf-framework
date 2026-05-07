package io.github.jerryraf.examples.sentry.controller;

import com.raf.framework.autoconfigure.common.annotation.ResponseResult;
import com.raf.framework.autoconfigure.common.exception.BusinessException;
import com.raf.framework.autoconfigure.common.exception.InfrastructureException;
import com.raf.framework.autoconfigure.common.exception.SystemException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * Sentry 异常上报演示接口
 * 演示不同异常类型的上报策略
 */
@RestController
@RequestMapping("/api/demo")
@ResponseResult
@Slf4j
public class ErrorDemoController {

    /**
     * 触发 BusinessException（默认不上报 Sentry）
     * code >= 10000，HTTP 200，WARN 日志
     */
    @GetMapping("/business-error")
    public String triggerBusinessError() {
        throw new BusinessException(10001, "用户不存在（业务异常，默认不上报 Sentry）");
    }

    /**
     * 触发 InfrastructureException（上报 Sentry）
     * 第三方服务/数据库错误，HTTP 500，ERROR 日志
     */
    @GetMapping("/infra-error")
    public String triggerInfraError() {
        throw new InfrastructureException("Redis 连接超时（基础设施异常，上报 Sentry）");
    }

    /**
     * 触发 SystemException（上报 Sentry，含完整堆栈）
     * 未预期错误，HTTP 500，ERROR 日志 + 完整堆栈
     */
    @GetMapping("/system-error")
    public String triggerSystemError() {
        try {
            int result = 1 / 0;
            return String.valueOf(result);
        } catch (Exception e) {
            throw new SystemException("系统内部错误（系统异常，上报 Sentry）", e);
        }
    }

    /**
     * 正常接口，验证 traceId 在 Sentry 事件中的注入
     */
    @GetMapping("/ok")
    public String ok() {
        log.info("正常请求，traceId 会自动注入到 Sentry 上下文");
        return "ok";
    }
}
