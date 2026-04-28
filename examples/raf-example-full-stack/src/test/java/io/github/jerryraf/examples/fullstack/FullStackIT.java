package io.github.jerryraf.examples.fullstack;

import io.github.jerryraf.examples.fullstack.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Full-stack integration test covering the complete order flow.
 *
 * @author RAF Framework Team
 * @since 2026-04-28
 */
@SpringBootTest(classes = io.github.jerryraf.examples.fullstack.order.OrderServiceApplication.class)
class FullStackIT {

    @Autowired
    private OrderService orderService;

    @Test
    void createOrder_shouldReturnOrderId() {
        Long orderId = orderService.createOrder(1L, 1L, 2, new BigDecimal("199.00"));
        assertNotNull(orderId);
    }
}
