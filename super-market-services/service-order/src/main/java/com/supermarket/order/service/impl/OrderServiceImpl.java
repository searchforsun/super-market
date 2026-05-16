package com.supermarket.order.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supermarket.common.core.exception.BizException;
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
import java.util.List;

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
                throw new BizException(400, "库存不足: skuId=" + item.getSkuId());
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
        if (order == null) throw new BizException(404, "订单不存在");
        return order;
    }

    @Override
    public Order getById(Long orderId) {
        Order order = super.getById(orderId);
        if (order == null) throw new BizException(404, "订单不存在");
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
        if (order.getOrderStatus() != 1) throw new BizException(400, "仅待付款订单可取消");
        order.setOrderStatus(5);
        updateById(order);
        rollbackInventory(orderNo);
    }

    @Override
    @Transactional
    public void paySuccess(String orderNo, String payNo) {
        Order order = getByOrderNo(orderNo);
        if (order.getOrderStatus() != 1) throw new BizException(400, "订单状态不正确");
        order.setOrderStatus(2);
        order.setPayNo(payNo);
        order.setPaidAt(LocalDateTime.now());
        updateById(order);
    }

    @Override
    @Transactional
    public void ship(String orderNo) {
        Order order = getByOrderNo(orderNo);
        if (order.getOrderStatus() != 2) throw new BizException(400, "仅待发货订单可发货");
        order.setOrderStatus(3);
        order.setShippedAt(LocalDateTime.now());
        updateById(order);
    }

    @Override
    @Transactional
    public void confirmReceive(String orderNo) {
        Order order = getByOrderNo(orderNo);
        if (order.getOrderStatus() != 3) throw new BizException(400, "仅待收货订单可确认");
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
