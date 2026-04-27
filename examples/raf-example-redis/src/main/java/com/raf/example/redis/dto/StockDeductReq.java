package com.raf.example.redis.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Stock deduction request DTO.
 *
 * @author Jerry
 * @since 2026-04-17
 */
@Data
public class StockDeductReq {

    @NotNull(message = "Product ID must not be null")
    private Long productId;

    @NotNull(message = "Quantity must not be null")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
}
