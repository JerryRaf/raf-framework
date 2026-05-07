package io.github.jerryraf.examples.rabbit.controller;

import com.raf.framework.core.common.result.RafResult;
import io.github.jerryraf.examples.rabbit.dto.OrderMessage;
import io.github.jerryraf.examples.rabbit.producer.OrderDelayProducer;
import io.github.jerryraf.examples.rabbit.producer.OrderNotifyProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * REST interface to trigger RabbitMQ message sending for demo purposes.
 *
 * @author Jerry
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderNotifyProducer notifyProducer;
    private final OrderDelayProducer delayProducer;

    @PostMapping("/{orderId}/notify")
    public RafResult<Void> sendNotify(@PathVariable Long orderId) {
        OrderMessage order = OrderMessage.builder()
                .orderId(orderId)
                .userId(1001L)
                .amount(new BigDecimal("99.00"))
                .status("PAID")
                .build();
        notifyProducer.send(order);
        return RafResult.success();
    }

    @PostMapping("/{orderId}/timeout")
    public RafResult<Void> sendTimeout(@PathVariable Long orderId,
                                       @RequestParam(defaultValue = "10") int delaySeconds) {
        OrderMessage order = OrderMessage.builder()
                .orderId(orderId)
                .userId(1001L)
                .amount(new BigDecimal("99.00"))
                .status("CREATED")
                .build();
        delayProducer.sendTimeout(order, delaySeconds);
        return RafResult.success();
    }
}
