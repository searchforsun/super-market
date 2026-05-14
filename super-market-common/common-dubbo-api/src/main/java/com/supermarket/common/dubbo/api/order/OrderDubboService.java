package com.supermarket.common.dubbo.api.order;

public interface OrderDubboService {

    void updateStatus(String orderNo, Integer toStatus);
}
