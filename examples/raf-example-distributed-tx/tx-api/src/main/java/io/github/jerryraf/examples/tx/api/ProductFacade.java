package io.github.jerryraf.examples.tx.api;

/**
 * Product service facade for distributed transaction.
 *
 * @author RAF Framework Team
 * @since 2026-04-28
 */
public interface ProductFacade {

    /**
     * Deduct product stock.
     *
     * @param productId product id
     * @param quantity  quantity to deduct
     * @return true if deduction succeeded, false if insufficient stock
     */
    boolean deductStock(Long productId, Integer quantity);
}
