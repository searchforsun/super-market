package com.supermarket.order.mq;

import com.supermarket.order.entity.Order;
import com.supermarket.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class OrderTimeoutConsumerTest {

    private OrderTimeoutConsumer consumer;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = mock(OrderService.class);
        consumer = new OrderTimeoutConsumer(orderService);
    }

    @Test
    void shouldCancelOrderWhenOrderExpired() {
        String orderNo = "ORD20260516123456";
        Order order = new Order();
        order.setOrderStatus(1);
        order.setExpireTime(LocalDateTime.now().minusHours(1));
        when(orderService.getByOrderNo(orderNo)).thenReturn(order);

        consumer.onMessage(orderNo);

        verify(orderService).cancelOrder(orderNo, "支付超时自动取消");
    }

    @Test
    void shouldNotCancelOrderWhenOrderNotExpired() {
        String orderNo = "ORD20260516123457";
        Order order = new Order();
        order.setOrderStatus(1);
        order.setExpireTime(LocalDateTime.now().plusHours(1));
        when(orderService.getByOrderNo(orderNo)).thenReturn(order);

        consumer.onMessage(orderNo);

        verify(orderService, never()).cancelOrder(anyString(), anyString());
    }

    @Test
    void shouldNotCancelOrderWhenStatusIsNotPendingPayment() {
        String orderNo = "ORD20260516123458";
        Order order = new Order();
        order.setOrderStatus(2);
        order.setExpireTime(LocalDateTime.now().minusHours(1));
        when(orderService.getByOrderNo(orderNo)).thenReturn(order);

        consumer.onMessage(orderNo);

        verify(orderService, never()).cancelOrder(anyString(), anyString());
    }

    @Test
    void shouldHandleExceptionWhenGetByOrderNoFails() {
        String orderNo = "ORD_NOT_EXIST";
        when(orderService.getByOrderNo(orderNo)).thenThrow(new RuntimeException("Order not found"));

        assertDoesNotThrow(() -> consumer.onMessage(orderNo));
        verify(orderService, never()).cancelOrder(anyString(), anyString());
    }
}
