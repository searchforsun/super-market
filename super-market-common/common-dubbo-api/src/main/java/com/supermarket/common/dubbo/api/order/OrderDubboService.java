package com.supermarket.common.dubbo.api.order;

import com.supermarket.common.dubbo.api.order.dto.CreateOrderRequest;
import com.supermarket.common.dubbo.api.order.dto.OrderDTO;

public interface OrderDubboService {
    OrderDTO createOrder(CreateOrderRequest request);
    OrderDTO getByOrderNo(String orderNo);
    void updateStatus(String orderNo, Integer toStatus);
}
