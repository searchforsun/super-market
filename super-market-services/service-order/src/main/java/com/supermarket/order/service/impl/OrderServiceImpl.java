package com.supermarket.order.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supermarket.common.core.exception.BizException;
import com.supermarket.common.core.result.ResultCode;
import com.supermarket.common.dubbo.api.inventory.InventoryDubboService;
import com.supermarket.common.dubbo.api.order.OrderDubboService;
import com.supermarket.common.dubbo.api.order.dto.CreateOrderRequest;
import com.supermarket.common.dubbo.api.order.dto.OrderDTO;
import com.supermarket.order.entity.Order;
import com.supermarket.order.entity.OrderItem;
import com.supermarket.order.mapper.OrderItemMapper;
import com.supermarket.order.mapper.OrderMapper;
import com.supermarket.order.mq.OrderEventProducer;
import com.supermarket.order.service.OrderService;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@DubboService(interfaceClass = OrderDubboService.class)
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService, OrderDubboService {

    private final OrderItemMapper orderItemMapper;

    @DubboReference(check = false)
    private InventoryDubboService inventoryDubboService;

    @Autowired(required = false)
    private OrderEventProducer orderEventProducer;

    @Override
    @GlobalTransactional(timeoutMills = 30000)
    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        String orderNo = "ORD" + IdUtil.getSnowflakeNextId();

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (var item : request.getItems()) {
            totalAmount = totalAmount.add(item.getSkuPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(request.getUserId());
        order.setShopId(request.getShopId() != null ? request.getShopId() : 1L);
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setFreightAmount(BigDecimal.ZERO);
        order.setActualAmount(totalAmount);
        order.setOrderStatus(1);
        order.setAddressSnapshot(request.getAddressSnapshot());
        order.setExpireTime(LocalDateTime.now().plusMinutes(30));
        save(order);

        for (var item : request.getItems()) {
            OrderItem oi = new OrderItem();
            oi.setOrderNo(orderNo);
            oi.setSkuId(item.getSkuId());
            oi.setSkuName(item.getSkuName());
            oi.setSkuImage(item.getSkuImage());
            oi.setSkuPrice(item.getSkuPrice());
            oi.setQuantity(item.getQuantity());
            oi.setTotalPrice(item.getSkuPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            orderItemMapper.insert(oi);
        }

        for (var item : request.getItems()) {
            boolean deducted = inventoryDubboService.deduct(item.getSkuId(), item.getQuantity());
            if (!deducted) {
                throw new BizException(ResultCode.STOCK_INSUFFICIENT, ResultCode.STOCK_INSUFFICIENT.getMessage() + ": skuId=" + item.getSkuId());
            }
        }

        orderEventProducer.sendOrderCreated(orderNo);
        orderEventProducer.sendOrderTimeoutCheck(orderNo);

        log.info("订单创建成功: orderNo={}, amount={}", orderNo, totalAmount);
        return order;
    }

    @Override
    public Order getByOrderNo(String orderNo) {
        Order order = getOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        if (order == null) throw new BizException(ResultCode.ORDER_NOT_FOUND);
        return order;
    }

    @Override
    public Order getById(Long orderId) {
        Order order = super.getById(orderId);
        if (order == null) throw new BizException(ResultCode.ORDER_NOT_FOUND);
        return order;
    }

    @Override
    public Page<Order> listByUser(Long userId, Integer status, int page, int size) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
            .eq(Order::getUserId, userId)
            .orderByDesc(Order::getCreatedAt);
        if (status != null) wrapper.eq(Order::getOrderStatus, status);
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public Page<Order> listByShop(Long shopId, Integer status, int page, int size) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
            .eq(Order::getShopId, shopId)
            .orderByDesc(Order::getCreatedAt);
        if (status != null) wrapper.eq(Order::getOrderStatus, status);
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public Page<Order> listAdmin(int page, int size) {
        return page(new Page<>(page, size),
            new LambdaQueryWrapper<Order>().orderByDesc(Order::getCreatedAt));
    }

    @Override
    @Transactional
    public void cancelOrder(String orderNo, String reason) {
        Order order = getByOrderNo(orderNo);
        int currentStatus = order.getOrderStatus();
        // 退款取消(status 6)支持从已付款(2)或已完成(4)发起，不恢复库存
        boolean isRefundCancel = "退款取消".equals(reason);
        if (isRefundCancel) {
            if (currentStatus != 2 && currentStatus != 4)
                throw new BizException(ResultCode.REFUND_CANCEL_FAILED);
        } else {
            if (currentStatus != 1)
                throw new BizException(ResultCode.ORDER_STATUS_ERROR);
            rollbackInventory(orderNo);
        }
        order.setOrderStatus(5);
        updateById(order);
    }

    @Override
    @Transactional
    public void paySuccess(String orderNo, String payNo) {
        Order order = getByOrderNo(orderNo);
        if (order.getOrderStatus() == 2)
            throw new BizException(ResultCode.ORDER_PAY_DUPLICATE);
        if (order.getOrderStatus() == 5)
            throw new BizException(ResultCode.ORDER_STATUS_ERROR);
        if (order.getOrderStatus() != 1)
            throw new BizException(ResultCode.ORDER_STATUS_ERROR);
        order.setOrderStatus(2);
        order.setPayNo(payNo);
        order.setPaidAt(LocalDateTime.now());
        updateById(order);
    }

    @Override
    @Transactional
    public void ship(String orderNo) {
        Order order = getByOrderNo(orderNo);
        if (order.getOrderStatus() != 2) throw new BizException(ResultCode.ORDER_STATUS_ERROR);
        order.setOrderStatus(3);
        order.setShippedAt(LocalDateTime.now());
        updateById(order);
    }

    @Override
    @Transactional
    public void confirmReceive(String orderNo) {
        Order order = getByOrderNo(orderNo);
        if (order.getOrderStatus() != 3) throw new BizException(ResultCode.ORDER_STATUS_ERROR);
        order.setOrderStatus(4);
        order.setReceivedAt(LocalDateTime.now());
        updateById(order);
    }

    @Override
    public List<OrderItem> getOrderItems(String orderNo) {
        return orderItemMapper.selectByOrderNo(orderNo);
    }

    // -- OrderDubboService impl (跨服务 Dubbo 调用) --

    @Override
    public void placeOrder(CreateOrderRequest request) {
        createOrder(request);
    }

    @Override
    public void updateStatus(String orderNo, Integer toStatus) {
        switch (toStatus) {
            case 2 -> paySuccess(orderNo, null);
            case 5 -> cancelOrder(orderNo, "系统取消");
            case 6 -> cancelOrder(orderNo, "退款取消");
            default -> log.warn("未知订单状态: {}", toStatus);
        }
    }

    // -- OrderDubboService statistics --

    @Override
    public List<Map<String, Object>> getDailyTrend(int days) {
        return getBaseMapper().selectDailyTrend(days);
    }

    @Override
    public List<Map<String, Object>> getOrderStatusDistribution() {
        return getBaseMapper().selectStatusDistribution();
    }

    @Override
    public List<Map<String, Object>> getCategorySales() {
        List<Map<String, Object>> list = orderItemMapper.selectCategorySales();
        double total = list.stream()
                .mapToDouble(m -> ((Number) m.getOrDefault("amount", 0)).doubleValue())
                .sum();
        for (Map<String, Object> m : list) {
            double amount = ((Number) m.getOrDefault("amount", 0)).doubleValue();
            m.put("percentage", total > 0 ? Math.round(amount / total * 10000.0) / 100.0 : 0);
        }
        return list;
    }

    @Override
    public List<Map<String, Object>> getMerchantDailyTrend(Long shopId, int days) {
        return getBaseMapper().selectMerchantDailyTrend(shopId, days);
    }

    @Override
    public List<Map<String, Object>> getMerchantOrderStatusDistribution(Long shopId) {
        return getBaseMapper().selectMerchantStatusDistribution(shopId);
    }

    @Override
    public Map<String, Object> getTodayStats() {
        Map<String, Object> stats = getBaseMapper().selectTodayStats();
        if (stats == null || stats.get("gmv") == null) {
            stats = new java.util.HashMap<>();
            stats.put("gmv", 0);
            stats.put("orderCount", 0);
        }
        return stats;
    }

    @Override
    public Map<String, Object> getMerchantTodayStats(Long shopId) {
        Map<String, Object> stats = getBaseMapper().selectMerchantTodayStats(shopId);
        if (stats == null || stats.get("gmv") == null) {
            stats = new java.util.HashMap<>();
            stats.put("gmv", 0);
            stats.put("orderCount", 0);
        }
        return stats;
    }

    private void rollbackInventory(String orderNo) {
        List<OrderItem> items = getOrderItems(orderNo);
        for (OrderItem item : items) {
            try {
                inventoryDubboService.restore(item.getSkuId(), item.getQuantity());
            } catch (Exception e) {
                log.error("回补库存失败: skuId={}", item.getSkuId(), e);
            }
        }
    }
}
