package io.github.jerryraf.examples.kafka.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 日志事件请求
 */
@Data
public class LogEventReq {

    @NotBlank(message = "服务名不能为空")
    private String serviceName;

    @NotNull(message = "级别不能为空")
    private String level;

    @NotBlank(message = "消息不能为空")
    private String message;

    private String traceId;
}
