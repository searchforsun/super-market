package com.supermarket.order.service.impl;

import com.supermarket.common.core.exception.BizException;
import com.supermarket.common.dubbo.api.inventory.InventoryDubboService;
import com.supermarket.common.dubbo.api.order.dto.CreateOrderRequest;
import com.supermarket.order.entity.Order;
import com.supermarket.order.entity.OrderItem;
import com.supermarket.order.mapper.OrderItemMapper;
import com.supermarket.order.mapper.OrderMapper;
import com.supermarket.order.mq.OrderEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class OrderServiceTest {

    private TestableOrderServiceImpl orderService;
    private OrderMapper orderMapper;
    private OrderItemMapper orderItemMapper;
    private InventoryDubboService inventoryDubboService;
    private OrderEventProducer orderEventProducer;

    @BeforeEach
    void setUp() {
        orderMapper = mock(OrderMapper.class);
        orderItemMapper = mock(OrderItemMapper.class);
        inventoryDubboService = mock(InventoryDubboService.class);
        orderEventProducer = mock(OrderEventProducer.class);
        orderService = new TestableOrderServiceImpl(orderItemMapper, inventoryDubboService, orderEventProducer);
        orderService.setBaseMapper(orderMapper);
    }

    @Test
    void shouldCreateOrder() {
        when(orderMapper.insert(any())).thenReturn(1);
        when(orderItemMapper.insert(any())).thenReturn(1);
        when(inventoryDubboService.deduct(anyLong(), anyInt())).thenReturn(true);

        Order order = orderService.createOrder(buildRequest());
        assertThat(order.getOrderNo()).startsWith("ORD");
        assertThat(order.getOrderStatus()).isEqualTo(1);
        verify(inventoryDubboService, atLeastOnce()).deduct(anyLong(), anyInt());
    }

    @Test
    void shouldThrowWhenInventoryInsufficient() {
        when(orderMapper.insert(any())).thenReturn(1);
        when(orderItemMapper.insert(any())).thenReturn(1);
        when(inventoryDubboService.deduct(anyLong(), anyInt())).thenReturn(false);

        assertThatThrownBy(() -> orderService.createOrder(buildRequest()))
            .isInstanceOf(BizException.class)
            .hasMessageContaining("库存不足");
    }

    @Test
    void shouldGetByOrderNo() {
        Order mockOrder = new Order();
        mockOrder.setOrderNo("ORD001");
        mockOrder.setOrderStatus(1);

        TestableOrderServiceImpl spy = spy(orderService);
        doReturn(mockOrder).when(spy).getOne(any());
        assertThat(spy.getByOrderNo("ORD001").getOrderNo()).isEqualTo("ORD001");
    }

    @Test
    void shouldGetOrderItems() {
        OrderItem item = new OrderItem();
        item.setOrderNo("ORD001");
        item.setSkuId(1001L);
        when(orderItemMapper.selectByOrderNo("ORD001")).thenReturn(List.of(item));

        List<OrderItem> items = orderService.getOrderItems("ORD001");
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getSkuId()).isEqualTo(1001L);
    }

    private CreateOrderRequest buildRequest() {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setUserId(1L);
        req.setAddressSnapshot("{\"name\":\"张三\",\"phone\":\"13800000000\"}");
        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
        item.setSkuId(1001L);
        item.setSkuName("测试商品-红色-XL");
        item.setSkuPrice(new BigDecimal("99.00"));
        item.setQuantity(2);
        req.setItems(List.of(item));
        return req;
    }

    static class TestableOrderServiceImpl extends OrderServiceImpl {
        TestableOrderServiceImpl(OrderItemMapper orderItemMapper, InventoryDubboService inventoryDubboService,
                                  OrderEventProducer orderEventProducer) {
            super(orderItemMapper, inventoryDubboService, orderEventProducer);
        }
        void setBaseMapper(OrderMapper mapper) {
            this.baseMapper = mapper;
        }
    }
}
