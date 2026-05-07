package io.github.jerryraf.examples.shardingsphere.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.raf.framework.autoconfigure.common.annotation.ResponseResult;
import io.github.jerryraf.examples.shardingsphere.entity.Order;
import io.github.jerryraf.examples.shardingsphere.service.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 订单控制器（ShardingSphere 分库分表演示）
 */
@RestController
@RequestMapping("/api/orders")
@ResponseResult
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public Order createOrder(@RequestBody @Valid Order order) {
        return orderService.createOrder(order);
    }

    @GetMapping("/{orderId}")
    public Order getOrder(@PathVariable Long orderId) {
        return orderService.getById(orderId);
    }

    @GetMapping("/user/{userId}")
    public List<Order> getOrdersByUser(@PathVariable Long userId) {
        return orderService.getByUserId(userId);
    }

    @GetMapping("/page")
    public Page<Order> pageOrders(
        @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize,
        @RequestParam(required = false) String status
    ) {
        return orderService.pageOrders(pageNum, pageSize, status);
    }
}
