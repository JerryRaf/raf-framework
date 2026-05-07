package io.github.jerryraf.examples.redis.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StockDeductReq {
    @NotNull
    private Long productId;
    @Min(1)
    private int quantity;
}
