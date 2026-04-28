package io.github.jerryraf.examples.tx.order.service;

import io.github.jerryraf.examples.tx.api.PaymentFacade;
import io.github.jerryraf.examples.tx.api.ProductFacade;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Order service with distributed transaction support via Seata AT mode.
 *
 * @author RAF Framework Team
 * @since 2026-04-28
 */
@Slf4j
@Service
public class OrderService {

    @DubboReference
    private ProductFacade productFacade;

    @DubboReference
    private PaymentFacade paymentFacade;

    /**
     * Create order with distributed transaction.
     * Rolls back all services if any step fails.
     *
     * @param productId product id
     * @param quantity  quantity to purchase
     * @param amount    payment amount
     * @return payment id
     * @throws RuntimeException if stock is insufficient or payment fails
     */
    @GlobalTransactional(name = "create-order", rollbackFor = Exception.class)
    public Long createOrder(Long productId, Integer quantity, BigDecimal amount) {
        log.info("Creating order: productId={}, quantity={}, amount={}", productId, quantity, amount);

        boolean deducted = productFacade.deductStock(productId, quantity);
        if (!deducted) {
            // In production, use BusinessException from raf-framework-web-starter
            throw new RuntimeException("Insufficient stock for product: " + productId);
        }

        Long orderId = System.currentTimeMillis();
        Long paymentId = paymentFacade.createPayment(orderId, amount);
        log.info("Order created: orderId={}, paymentId={}", orderId, paymentId);
        return paymentId;
    }
}
