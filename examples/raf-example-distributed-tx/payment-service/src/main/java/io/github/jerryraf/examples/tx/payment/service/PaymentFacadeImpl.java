package io.github.jerryraf.examples.tx.payment.service;

import io.github.jerryraf.examples.tx.api.PaymentFacade;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Payment facade implementation for distributed transaction participant.
 *
 * @author RAF Framework Team
 * @since 2026-04-28
 */
@Slf4j
@DubboService
public class PaymentFacadeImpl implements PaymentFacade {

    private static final AtomicLong PAYMENT_ID_SEQ = new AtomicLong(1000L);

    @Override
    public Long createPayment(Long orderId, BigDecimal amount) {
        log.info("Creating payment: orderId={}, amount={}", orderId, amount);
        if (orderId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Invalid payment parameters");
        }
        Long paymentId = PAYMENT_ID_SEQ.incrementAndGet();
        log.info("Payment created: paymentId={}", paymentId);
        return paymentId;
    }
}
