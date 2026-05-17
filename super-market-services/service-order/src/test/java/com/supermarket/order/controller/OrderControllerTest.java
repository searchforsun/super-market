package com.supermarket.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supermarket.common.dubbo.api.order.dto.CreateOrderRequest;
import com.supermarket.order.entity.Order;
import com.supermarket.order.entity.OrderItem;
import com.supermarket.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Uses @SpringBootTest with a minimal TestConfig instead of @WebMvcTest to avoid
 * loading the main application class which has @EnableDubbo. The @EnableDubbo
 * annotation triggers Dubbo service export and @DubboReference resolution that
 * cannot be disabled through auto-configuration exclusions alone.
 * <p>
 * The inner TestConfig uses @EnableAutoConfiguration to restore web-related
 * auto-configurations (Jackson, MVC) that are needed for MockMvc to work,
 * while explicitly excluding infrastructure auto-configurations (Dubbo, Nacos,
 * DataSource, Redis, Seata, MyBatis-Plus).
 */
@SpringBootTest(classes = {OrderController.class, OrderControllerTest.TestConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @Configuration
    @EnableAutoConfiguration
    @ComponentScan(basePackageClasses = OrderController.class)
    static class TestConfig {
    }

    @Test
    void shouldCreateOrderWhenValidRequest() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setUserId(1L);
        request.setShopId(10L);
        request.setAddressId(100L);
        request.setRemark("Please deliver fast");

        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
        item.setSkuId(1000L);
        item.setSkuName("Test SKU");
        item.setSkuPrice(new BigDecimal("99.99"));
        item.setQuantity(2);
        request.setItems(List.of(item));

        Order order = new Order();
        order.setOrderNo("ORD202405160001");
        order.setUserId(1L);
        order.setActualAmount(new BigDecimal("199.98"));

        when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(order);

        mockMvc.perform(post("/api/order/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.orderNo").value("ORD202405160001"))
                .andExpect(jsonPath("$.data.userId").value(1));
    }

    @Test
    void shouldReturnOrderWhenGetDetail() throws Exception {
        Order order = new Order();
        order.setOrderNo("ORD202405160001");
        order.setUserId(1L);
        order.setOrderStatus(1);

        when(orderService.getByOrderNo("ORD202405160001")).thenReturn(order);

        mockMvc.perform(get("/api/order/ORD202405160001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.orderNo").value("ORD202405160001"))
                .andExpect(jsonPath("$.data.orderStatus").value(1));
    }

    @Test
    void shouldReturnPageWhenListByShop() throws Exception {
        Order order = new Order();
        order.setOrderNo("ORD001");
        order.setShopId(10L);
        Page<Order> page = new Page<>(1, 10);
        page.setRecords(List.of(order));
        page.setTotal(1);

        when(orderService.listByShop(eq(10L), any(), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/api/order/list/shop/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records[0].orderNo").value("ORD001"));
    }

    @Test
    void shouldReturnPageWhenListByUser() throws Exception {
        Order order = new Order();
        order.setOrderNo("ORD002");
        order.setUserId(1L);
        Page<Order> page = new Page<>(1, 10);
        page.setRecords(List.of(order));
        page.setTotal(1);

        when(orderService.listByUser(eq(1L), any(), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/api/order/list/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records[0].orderNo").value("ORD002"));
    }

    @Test
    void shouldReturnItemsWhenGetOrderItems() throws Exception {
        OrderItem item = new OrderItem();
        item.setId(1L);
        item.setOrderNo("ORD001");
        item.setSkuName("Test Item");
        item.setQuantity(2);

        when(orderService.getOrderItems("ORD001")).thenReturn(List.of(item));

        mockMvc.perform(get("/api/order/ORD001/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].skuName").value("Test Item"))
                .andExpect(jsonPath("$.data[0].quantity").value(2));
    }

    @Test
    void shouldCancelOrderWhenValidReason() throws Exception {
        doNothing().when(orderService).cancelOrder("ORD001", "Changed my mind");

        mockMvc.perform(put("/api/order/ORD001/cancel")
                        .param("reason", "Changed my mind"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"));
    }

    @Test
    void shouldShipOrderWhenExists() throws Exception {
        doNothing().when(orderService).ship("ORD001");

        mockMvc.perform(put("/api/order/ORD001/ship"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"));
    }

    @Test
    void shouldReceiveOrderWhenExists() throws Exception {
        doNothing().when(orderService).confirmReceive("ORD001");

        mockMvc.perform(put("/api/order/ORD001/receive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"));
    }

    @Test
    void shouldReturnPageWhenAdminList() throws Exception {
        Order order = new Order();
        order.setOrderNo("ORD003");
        Page<Order> page = new Page<>(1, 10);
        page.setRecords(List.of(order));
        page.setTotal(1);

        when(orderService.listAdmin(anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/api/order/admin/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records[0].orderNo").value("ORD003"));
    }
}
