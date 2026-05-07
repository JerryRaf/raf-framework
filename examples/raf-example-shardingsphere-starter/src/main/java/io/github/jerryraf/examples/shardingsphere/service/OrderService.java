package io.github.jerryraf.examples.shardingsphere.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.raf.framework.autoconfigure.common.exception.BusinessException;
import io.github.jerryraf.examples.shardingsphere.entity.Order;
import io.github.jerryraf.examples.shardingsphere.mapper.OrderMapper;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订单服务（演示 ShardingSphere 分库分表路由）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderMapper orderMapper;

    /**
     * 创建订单
     * ShardingSphere 根据 user_id 路由到 ds0/ds1，根据 order_id 路由到 t_order_0/t_order_1
     */
    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(Order order) {
        order.setCreateTime(LocalDateTime.now());
        order.setStatus("PENDING");
        orderMapper.insert(order);
        log.info("订单创建成功，orderId={}, userId={}, 路由到 ds{}.t_order_{}",
            order.getOrderId(), order.getUserId(),
            order.getUserId() % 2, order.getOrderId() % 2);
        return order;
    }

    /**
     * 按订单ID查询（ShardingSphere 自动路由）
     */
    public Order getById(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(10001, "订单不存在");
        }
        return order;
    }

    /**
     * 按用户ID查询订单列表（广播到所有分库，ShardingSphere 合并结果）
     */
    public List<Order> getByUserId(Long userId) {
        return orderMapper.selectByUserId(userId);
    }

    /**
     * 分页查询（跨分库分表，ShardingSphere 自动归并）
     */
    public Page<Order> pageOrders(int pageNum, int pageSize, String status) {
        Page<Order> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
            .eq(status != null, Order::getStatus, status)
            .orderByDesc(Order::getCreateTime);
        return orderMapper.selectPage(page, wrapper);
    }
}
