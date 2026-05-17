package com.supermarket.order.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderServiceTest {

    private OrderServiceImpl orderService;
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
        orderService = new OrderServiceImpl(orderItemMapper);
        ReflectionTestUtils.setField(orderService, "baseMapper", orderMapper);
        ReflectionTestUtils.setField(orderService, "inventoryDubboService", inventoryDubboService);
        ReflectionTestUtils.setField(orderService, "orderEventProducer", orderEventProducer);
    }

    // -- Order creation tests --

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

    // -- Order query tests --

    @Test
    void shouldGetByOrderNo() {
        Order mockOrder = new Order();
        mockOrder.setOrderNo("ORD001");
        mockOrder.setOrderStatus(1);

        // getOne with throwEx=true calls selectList internally
        when(orderMapper.selectOne(any(), anyBoolean())).thenReturn(mockOrder);
        assertThat(orderService.getByOrderNo("ORD001").getOrderNo()).isEqualTo("ORD001");
    }

    @Test
    void shouldThrowWhenOrderNoNotFound() {
        // getOne with throwEx=true calls selectList - return empty list = null result
        when(orderMapper.selectOne(any(), anyBoolean())).thenReturn(null);

        assertThatThrownBy(() -> orderService.getByOrderNo("ORD_NOT_EXIST"))
            .isInstanceOf(BizException.class)
            .hasMessageContaining("订单不存在");
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

    @Test
    void shouldGetById() {
        Order order = new Order();
        order.setId(1L);
        order.setOrderNo("ORD001");
        order.setOrderStatus(1);
        when(orderMapper.selectById(1L)).thenReturn(order);

        Order result = orderService.getById(1L);
        assertThat(result.getOrderNo()).isEqualTo("ORD001");
    }

    @Test
    void shouldThrowWhenGetByIdNotFound() {
        when(orderMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> orderService.getById(999L))
            .isInstanceOf(BizException.class)
            .hasMessageContaining("订单不存在");
    }

    // -- Order lifecycle tests (status transitions) --

    @Test
    void shouldCancelOrder() {
        Order order = createOrderWithStatus(1);
        when(orderMapper.selectOne(any(), anyBoolean())).thenReturn(order);
        when(orderItemMapper.selectByOrderNo("ORD001")).thenReturn(List.of());
        when(orderMapper.updateById(any())).thenReturn(1);

        orderService.cancelOrder("ORD001", "测试取消");

        verify(orderMapper).updateById(argThat(o -> o.getOrderStatus() == 5));
    }

    @Test
    void shouldThrowWhenCancelNonPendingOrder() {
        when(orderMapper.selectOne(any(), anyBoolean())).thenReturn(createOrderWithStatus(2));

        assertThatThrownBy(() -> orderService.cancelOrder("ORD001", "test"))
            .isInstanceOf(BizException.class)
            .hasMessageContaining("仅待付款订单可取消");
    }

    @Test
    void shouldRollbackInventoryOnCancel() {
        Order order = createOrderWithStatus(1);
        when(orderMapper.selectOne(any(), anyBoolean())).thenReturn(order);

        OrderItem item = new OrderItem();
        item.setSkuId(1001L);
        item.setQuantity(2);
        when(orderItemMapper.selectByOrderNo("ORD001")).thenReturn(List.of(item));
        when(inventoryDubboService.restore(anyLong(), anyInt())).thenReturn(true);
        when(orderMapper.updateById(any())).thenReturn(1);

        orderService.cancelOrder("ORD001", "取消");

        verify(inventoryDubboService).restore(1001L, 2);
    }

    @Test
    void shouldNotThrowWhenRollbackInventoryFails() {
        Order order = createOrderWithStatus(1);
        when(orderMapper.selectOne(any(), anyBoolean())).thenReturn(order);

        OrderItem item = new OrderItem();
        item.setSkuId(1001L);
        item.setQuantity(2);
        when(orderItemMapper.selectByOrderNo("ORD001")).thenReturn(List.of(item));
        when(inventoryDubboService.restore(anyLong(), anyInt())).thenThrow(new RuntimeException("连接失败"));
        when(orderMapper.updateById(any())).thenReturn(1);

        // Should not throw - exception is caught in rollbackInventory
        orderService.cancelOrder("ORD001", "取消");

        verify(orderMapper).updateById(argThat(o -> o.getOrderStatus() == 5));
    }

    @Test
    void shouldPaySuccess() {
        when(orderMapper.selectOne(any(), anyBoolean())).thenReturn(createOrderWithStatus(1));
        when(orderMapper.updateById(any())).thenReturn(1);

        orderService.paySuccess("ORD001", "PAY001");

        verify(orderMapper).updateById(argThat(o -> o.getOrderStatus() == 2));
    }

    @Test
    void shouldThrowWhenPaySuccessWrongStatus() {
        when(orderMapper.selectOne(any(), anyBoolean())).thenReturn(createOrderWithStatus(3));

        assertThatThrownBy(() -> orderService.paySuccess("ORD001", "PAY002"))
            .isInstanceOf(BizException.class)
            .hasMessageContaining("订单状态不正确");
    }

    @Test
    void shouldShip() {
        when(orderMapper.selectOne(any(), anyBoolean())).thenReturn(createOrderWithStatus(2));
        when(orderMapper.updateById(any())).thenReturn(1);

        orderService.ship("ORD001");

        verify(orderMapper).updateById(argThat(o -> o.getOrderStatus() == 3));
    }

    @Test
    void shouldThrowWhenShipWrongStatus() {
        when(orderMapper.selectOne(any(), anyBoolean())).thenReturn(createOrderWithStatus(1));

        assertThatThrownBy(() -> orderService.ship("ORD001"))
            .isInstanceOf(BizException.class)
            .hasMessageContaining("仅待发货订单可发货");
    }

    @Test
    void shouldConfirmReceive() {
        when(orderMapper.selectOne(any(), anyBoolean())).thenReturn(createOrderWithStatus(3));
        when(orderMapper.updateById(any())).thenReturn(1);

        orderService.confirmReceive("ORD001");

        verify(orderMapper).updateById(argThat(o -> o.getOrderStatus() == 4));
    }

    @Test
    void shouldThrowWhenConfirmReceiveWrongStatus() {
        when(orderMapper.selectOne(any(), anyBoolean())).thenReturn(createOrderWithStatus(1));

        assertThatThrownBy(() -> orderService.confirmReceive("ORD001"))
            .isInstanceOf(BizException.class)
            .hasMessageContaining("仅待收货订单可确认");
    }

    // -- Paginated list tests --

    @Test
    void shouldListByUser() {
        Page<Order> mockPage = new Page<>(1, 10);
        when(orderMapper.selectPage(any(), any())).thenReturn(mockPage);

        Page<Order> result = orderService.listByUser(1L, null, 1, 10);
        assertThat(result).isNotNull();
    }

    @Test
    void shouldListByUserWithStatusFilter() {
        Page<Order> mockPage = new Page<>(1, 10);
        when(orderMapper.selectPage(any(), any())).thenReturn(mockPage);

        Page<Order> result = orderService.listByUser(1L, 1, 1, 10);
        assertThat(result).isNotNull();
    }

    @Test
    void shouldListByShop() {
        Page<Order> mockPage = new Page<>(1, 10);
        when(orderMapper.selectPage(any(), any())).thenReturn(mockPage);

        Page<Order> result = orderService.listByShop(1L, null, 1, 10);
        assertThat(result).isNotNull();
    }

    @Test
    void shouldListByShopWithStatusFilter() {
        Page<Order> mockPage = new Page<>(1, 10);
        when(orderMapper.selectPage(any(), any())).thenReturn(mockPage);

        Page<Order> result = orderService.listByShop(1L, 2, 1, 10);
        assertThat(result).isNotNull();
    }

    @Test
    void shouldListAdmin() {
        Page<Order> mockPage = new Page<>(1, 10);
        when(orderMapper.selectPage(any(), any())).thenReturn(mockPage);

        Page<Order> result = orderService.listAdmin(1, 10);
        assertThat(result).isNotNull();
    }

    // -- Dubbo updateStatus tests --

    @Test
    void shouldUpdateStatusToPaid() {
        when(orderMapper.selectOne(any(), anyBoolean())).thenReturn(createOrderWithStatus(1));
        when(orderMapper.updateById(any())).thenReturn(1);

        orderService.updateStatus("ORD001", 2);

        verify(orderMapper).updateById(argThat(o -> o.getOrderStatus() == 2));
    }

    @Test
    void shouldUpdateStatusToCancelled() {
        when(orderMapper.selectOne(any(), anyBoolean())).thenReturn(createOrderWithStatus(1));
        when(orderItemMapper.selectByOrderNo("ORD001")).thenReturn(List.of());
        when(orderMapper.updateById(any())).thenReturn(1);

        orderService.updateStatus("ORD001", 5);

        verify(orderMapper).updateById(argThat(o -> o.getOrderStatus() == 5));
    }

    @Test
    void shouldUpdateStatusToRefundCancelled() {
        when(orderMapper.selectOne(any(), anyBoolean())).thenReturn(createOrderWithStatus(1));
        when(orderItemMapper.selectByOrderNo("ORD001")).thenReturn(List.of());
        when(orderMapper.updateById(any())).thenReturn(1);

        orderService.updateStatus("ORD001", 6);

        verify(orderMapper).updateById(argThat(o -> o.getOrderStatus() == 5));
    }

    @Test
    void shouldHandleUnknownStatusGracefully() {
        // Should not throw, just log a warning
        orderService.updateStatus("ORD001", 99);

        verify(orderMapper, never()).updateById(any());
    }

    // -- Dubbo placeOrder test --

    @Test
    void shouldPlaceOrderViaDubbo() {
        when(orderMapper.insert(any())).thenReturn(1);
        when(orderItemMapper.insert(any())).thenReturn(1);
        when(inventoryDubboService.deduct(anyLong(), anyInt())).thenReturn(true);

        orderService.placeOrder(buildRequest());

        verify(orderMapper, atLeastOnce()).insert(any());
    }

    // -- Helper methods --

    private Order createOrderWithStatus(int status) {
        Order order = new Order();
        order.setOrderNo("ORD001");
        order.setOrderStatus(status);
        return order;
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
}
