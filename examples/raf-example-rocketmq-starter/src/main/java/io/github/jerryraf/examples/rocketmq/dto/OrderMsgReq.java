package io.github.jerryraf.examples.rocketmq.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 订单消息请求
 */
@Data
public class OrderMsgReq {

    @NotBlank(message = "订单ID不能为空")
    private String orderId;

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "金额不能为空")
    @Positive(message = "金额必须大于0")
    private Double amount;

    /** 延时级别（1-18），仅延时消息有效 */
    private int delayLevel = 3;
}
