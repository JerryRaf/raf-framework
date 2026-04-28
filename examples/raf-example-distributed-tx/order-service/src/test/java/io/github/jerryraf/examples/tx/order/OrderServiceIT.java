package io.github.jerryraf.examples.tx.order;

import io.github.jerryraf.examples.tx.api.PaymentFacade;
import io.github.jerryraf.examples.tx.api.ProductFacade;
import io.github.jerryraf.examples.tx.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Integration test for OrderService distributed transaction scenarios.
 *
 * @author RAF Framework Team
 * @since 2026-04-28
 */
@SpringBootTest
class OrderServiceIT {

    @Autowired
    private OrderService orderService;

    @MockBean
    private ProductFacade productFacade;

    @MockBean
    private PaymentFacade paymentFacade;

    @Test
    void createOrder_shouldSucceedWhenStockSufficient() {
        when(productFacade.deductStock(1L, 2)).thenReturn(true);
        when(paymentFacade.createPayment(any(), eq(new BigDecimal("100.00")))).thenReturn(999L);

        Long paymentId = orderService.createOrder(1L, 2, new BigDecimal("100.00"));

        assertEquals(999L, paymentId);
    }

    @Test
    void createOrder_shouldThrowWhenStockInsufficient() {
        when(productFacade.deductStock(1L, 100)).thenReturn(false);

        assertThrows(RuntimeException.class,
                () -> orderService.createOrder(1L, 100, new BigDecimal("500.00")));
    }
}
