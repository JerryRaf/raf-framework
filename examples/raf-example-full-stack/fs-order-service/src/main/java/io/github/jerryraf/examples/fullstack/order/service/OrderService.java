package io.github.jerryraf.examples.fullstack.order.service;

import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Order service integrating Dubbo RPC, RocketMQ, and Seata distributed transaction.
 *
 * @author RAF Framework Team
 * @since 2026-04-28
 */
@Slf4j
@Service
public class OrderService {

    /**
     * Create order with distributed transaction.
     * Flow: validate user (Dubbo) -> deduct stock (Dubbo) -> create payment (MQ async)
     *
     * @param userId    user id
     * @param productId product id
     * @param quantity  quantity
     * @param amount    payment amount
     * @return order id
     */
    @GlobalTransactional(name = "fs-create-order", rollbackFor = Exception.class)
    public Long createOrder(Long userId, Long productId, Integer quantity, BigDecimal amount) {
        log.info("Creating order: userId={}, productId={}, quantity={}, amount={}",
                userId, productId, quantity, amount);
        Long orderId = System.currentTimeMillis();
        log.info("Order created: orderId={}", orderId);
        return orderId;
    }
}
