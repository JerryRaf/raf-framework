package io.github.jerryraf.examples.tx.api;

import java.math.BigDecimal;

/**
 * Payment service facade for distributed transaction.
 *
 * @author RAF Framework Team
 * @since 2026-04-28
 */
public interface PaymentFacade {

    /**
     * Create a payment record.
     *
     * @param orderId order id
     * @param amount  payment amount
     * @return payment id
     */
    Long createPayment(Long orderId, BigDecimal amount);
}
