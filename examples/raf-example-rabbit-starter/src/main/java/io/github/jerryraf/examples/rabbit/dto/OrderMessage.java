package io.github.jerryraf.examples.rabbit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Order message payload.
 *
 * @author Jerry
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Order ID */
    private Long orderId;

    /** User ID */
    private Long userId;

    /** Order amount */
    private BigDecimal amount;

    /** Order status: CREATED / PAID / CANCELLED */
    private String status;
}
