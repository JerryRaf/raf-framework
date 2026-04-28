package io.github.jerryraf.examples.tx.product.service;

import io.github.jerryraf.examples.tx.api.ProductFacade;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * Product facade implementation for distributed transaction participant.
 *
 * @author RAF Framework Team
 * @since 2026-04-28
 */
@Slf4j
@DubboService
public class ProductFacadeImpl implements ProductFacade {

    @Override
    public boolean deductStock(Long productId, Integer quantity) {
        log.info("Deducting stock: productId={}, quantity={}", productId, quantity);
        if (productId == null || quantity == null || quantity <= 0) {
            return false;
        }
        // Simulate stock check: productId > 100 means out of stock
        if (productId > 100) {
            log.warn("Product {} is out of stock", productId);
            return false;
        }
        log.info("Stock deducted successfully: productId={}, quantity={}", productId, quantity);
        return true;
    }
}
