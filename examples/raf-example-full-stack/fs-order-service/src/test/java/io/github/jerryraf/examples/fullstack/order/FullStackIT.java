package io.github.jerryraf.examples.fullstack.order;

import io.github.jerryraf.examples.fullstack.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Full-stack integration test covering the complete order flow.
 *
 * @author RAF Framework Team
 * @since 2026-04-28
 */
@SpringBootTest
@TestPropertySource(properties = {
        "dubbo.registry.address=N/A",
        "seata.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "rocketmq.name-server=localhost:9876"
})
class FullStackIT {

    @Autowired
    private OrderService orderService;

    @Test
    void createOrder_shouldReturnOrderId() {
        Long orderId = orderService.createOrder(1L, 1L, 2, new BigDecimal("199.00"));
        assertNotNull(orderId);
    }
}
