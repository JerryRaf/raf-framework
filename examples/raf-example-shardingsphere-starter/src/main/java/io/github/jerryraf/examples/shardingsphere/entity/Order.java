package io.github.jerryraf.examples.shardingsphere.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 订单实体（分库分表演示）
 * 分库键：user_id % 2 → ds0 / ds1
 * 分表键：order_id % 2 → t_order_0 / t_order_1
 */
@Data
@TableName("t_order")
public class Order {

    @TableId(type = IdType.ASSIGN_ID)
    private Long orderId;

    private Long userId;

    private String status;

    private BigDecimal totalAmount;

    private LocalDateTime createTime;
}
