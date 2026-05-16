package com.supermarket.order.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supermarket.common.dubbo.api.order.dto.CreateOrderRequest;
import com.supermarket.order.entity.Order;
import com.supermarket.order.entity.OrderItem;

import java.util.List;

public interface OrderService {
    Order createOrder(CreateOrderRequest request);
    Order getByOrderNo(String orderNo);
    Order getById(Long orderId);
    Page<Order> listByUser(Long userId, Integer status, int page, int size);
    Page<Order> listByShop(Long shopId, Integer status, int page, int size);
    Page<Order> listAdmin(int page, int size);
    void cancelOrder(String orderNo, String reason);
    void paySuccess(String orderNo, String payNo);
    void ship(String orderNo);
    void confirmReceive(String orderNo);
    List<OrderItem> getOrderItems(String orderNo);
}
