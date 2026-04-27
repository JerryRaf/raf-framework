package com.raf.example.redis.controller;

import com.raf.example.redis.dto.StockDeductReq;
import com.raf.example.redis.service.StockService;
import com.raf.framework.autoconfigure.common.result.RafResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Stock controller - demonstrates Redis cache and distributed lock via RAF Framework.
 *
 * @author Jerry
 * @since 2026-04-17
 */
@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    /**
     * Initialize product stock (demo endpoint).
     *
     * @param productId product ID
     * @param stock     initial stock quantity
     * @return success response
     */
    @PostMapping("/init/{productId}")
    public RafResult<Void> initStock(@PathVariable Long productId,
                                     @RequestParam(defaultValue = "100") int stock) {
        stockService.initStock(productId, stock);
        return RafResult.success();
    }

    /**
     * Get current stock for a product.
     * Demonstrates cache-aside pattern with penetration protection.
     *
     * @param productId product ID
     * @return current stock quantity
     */
    @GetMapping("/{productId}")
    public RafResult<Integer> getStock(@PathVariable Long productId) {
        return RafResult.success(stockService.getStock(productId));
    }

    /**
     * Deduct stock with distributed lock.
     * Demonstrates Redisson tryLock pattern.
     *
     * @param req deduction request
     * @return success response
     */
    @PostMapping("/deduct")
    public RafResult<Void> deductStock(@Valid @RequestBody StockDeductReq req) {
        stockService.deductStock(req.getProductId(), req.getQuantity());
        return RafResult.success();
    }
}
