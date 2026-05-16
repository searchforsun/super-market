package com.supermarket.common.dubbo.api.order;

import com.supermarket.common.dubbo.api.order.dto.CreateOrderRequest;

public interface OrderDubboService {

    void updateStatus(String orderNo, Integer toStatus);

    void placeOrder(CreateOrderRequest request);
}
