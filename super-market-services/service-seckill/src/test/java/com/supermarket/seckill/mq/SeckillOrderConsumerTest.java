package com.supermarket.seckill.mq;

import com.supermarket.common.dubbo.api.order.OrderDubboService;
import com.supermarket.common.dubbo.api.order.dto.CreateOrderRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class SeckillOrderConsumerTest {

    private SeckillOrderConsumer consumer;
    private OrderDubboService orderDubboService;

    @BeforeEach
    void setUp() throws Exception {
        orderDubboService = mock(OrderDubboService.class);
        consumer = new SeckillOrderConsumer();
        Field field = SeckillOrderConsumer.class.getDeclaredField("orderDubboService");
        field.setAccessible(true);
        field.set(consumer, orderDubboService);
    }

    @Test
    void shouldCreateOrderWhenValidMessage() {
        String json = "{\"userId\":1,\"skuId\":100,\"price\":99.99,\"quantity\":2}";

        consumer.onMessage(json);

        ArgumentCaptor<CreateOrderRequest> captor = ArgumentCaptor.forClass(CreateOrderRequest.class);
        verify(orderDubboService).placeOrder(captor.capture());
        CreateOrderRequest request = captor.getValue();
        assertEquals(1L, request.getUserId());
        assertEquals(1, request.getItems().size());
        assertEquals(100L, request.getItems().get(0).getSkuId());
        assertEquals(new BigDecimal("99.99"), request.getItems().get(0).getSkuPrice());
        assertEquals(2, request.getItems().get(0).getQuantity());
    }

    @Test
    void shouldHandleExceptionWhenInvalidJson() {
        consumer.onMessage("not-a-valid-json");

        verify(orderDubboService, never()).placeOrder(any());
    }

    @Test
    void shouldHandleExceptionWhenMessageIsMalformed() {
        consumer.onMessage("{\"userId\":\"not-a-number\"}");

        verify(orderDubboService, never()).placeOrder(any());
    }
}
