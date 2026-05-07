package io.github.jerryraf.examples.redis.controller;

import com.raf.framework.core.common.result.RafResult;
import io.github.jerryraf.examples.redis.dto.StockDeductReq;
import io.github.jerryraf.examples.redis.service.StockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * REST interface for distributed lock demo (stock deduction).
 *
 * @author Jerry
 */
@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @PostMapping("/init/{productId}")
    public RafResult<Void> initStock(@PathVariable Long productId,
                                     @RequestParam(defaultValue = "100") int stock) {
        stockService.initStock(productId, stock);
        return RafResult.success();
    }

    @GetMapping("/{productId}")
    public RafResult<Integer> getStock(@PathVariable Long productId) {
        return RafResult.success(stockService.getStock(productId));
    }

    @PostMapping("/deduct")
    public RafResult<Void> deductStock(@Valid @RequestBody StockDeductReq req) {
        stockService.deductStock(req.getProductId(), req.getQuantity());
        return RafResult.success();
    }
}
