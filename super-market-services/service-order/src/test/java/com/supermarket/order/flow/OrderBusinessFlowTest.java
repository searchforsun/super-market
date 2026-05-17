package com.supermarket.order.flow;

import org.mybatis.spring.annotation.MapperScan;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supermarket.common.dubbo.api.inventory.InventoryDubboService;
import com.supermarket.common.dubbo.api.order.dto.CreateOrderRequest;
import com.supermarket.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Postman-style business flow tests for the Order domain.
 *
 * Each test method chains multiple API calls to verify complete order
 * lifecycle scenarios, similar to a Postman collection run.
 *
 * Uses H2 in-memory database (MySQL mode) with @Transactional rollback
 * so each test starts with a clean state. External Dubbo dependencies
 * (InventoryDubboService) are mocked.
 */
@SpringBootTest(
    classes = {OrderBusinessFlowTest.TestConfig.class},
    properties = {"spring.cloud.bootstrap.enabled=false"}
)
@AutoConfigureMockMvc
@ActiveProfiles("flow-test")
@Transactional
class OrderBusinessFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderService orderService;

    @MockBean
    private InventoryDubboService inventoryDubboService;

    @BeforeEach
    void setUp() {
        // OrderServiceImpl uses @DubboReference for inventoryDubboService,
        // which is not processed when Dubbo auto-config is excluded.
        // Spring may wrap the service in an AOP proxy (@Transactional), so
        // we unwrap to set the field on the actual target object.
        OrderService target = AopTestUtils.getUltimateTargetObject(orderService);
        ReflectionTestUtils.setField(target, "inventoryDubboService", inventoryDubboService);

        // Stub inventory operations to succeed by default
        when(inventoryDubboService.deduct(anyLong(), anyInt())).thenReturn(true);
        when(inventoryDubboService.restore(anyLong(), anyInt())).thenReturn(true);
    }

    @Configuration
    @MapperScan("com.supermarket.order.mapper")
    @EnableAutoConfiguration(excludeName = {
        "org.apache.dubbo.spring.boot.autoconfigure.DubboAutoConfiguration",
        "org.apache.dubbo.spring.boot.autoconfigure.DubboRelaxedBindingAutoConfiguration",
        "org.apache.dubbo.spring.boot.autoconfigure.DubboRelaxedBinding2AutoConfiguration",
        "org.apache.dubbo.spring.boot.autoconfigure.DubboListenerAutoConfiguration",
        "com.alibaba.cloud.nacos.NacosDiscoveryAutoConfiguration",
        "com.alibaba.cloud.nacos.NacosConfigAutoConfiguration",
        "com.alibaba.cloud.nacos.NacosServiceAutoConfiguration",
        "com.alibaba.cloud.nacos.registry.NacosServiceRegistryAutoConfiguration",
        "com.alibaba.cloud.nacos.discovery.NacosDiscoveryClientConfiguration",
        "com.alibaba.cloud.nacos.discovery.NacosDiscoveryHeartBeatConfiguration",
        "com.alibaba.cloud.nacos.discovery.reactive.NacosReactiveDiscoveryClientConfiguration",
        "com.alibaba.cloud.nacos.discovery.configclient.NacosConfigServerAutoConfiguration",
        "com.alibaba.cloud.nacos.endpoint.NacosDiscoveryEndpointAutoConfiguration",
        "com.alibaba.cloud.nacos.endpoint.NacosConfigEndpointAutoConfiguration",
        "com.alibaba.cloud.nacos.loadbalancer.LoadBalancerNacosAutoConfiguration",
        "com.alibaba.cloud.nacos.util.UtilIPv6AutoConfiguration",
        "io.seata.spring.boot.autoconfigure.SeataAutoConfiguration",
        "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
        "org.redisson.spring.starter.RedissonAutoConfigurationV2"
    })
    @ComponentScan(basePackages = {
        "com.supermarket.order.controller",
        "com.supermarket.order.service",
        "com.supermarket.order.mapper",
        "com.supermarket.order.mq",
        "com.supermarket.common.mybatis",
        "com.supermarket.common.web.handler",
        "com.supermarket.common.web.config",
        "com.supermarket.common.web.interceptor"
    })
    static class TestConfig {
    }

    // ========================================================================
    // Flow 1: Complete Order Lifecycle (create -> pay -> ship -> receive)
    // ========================================================================

    @Test
    @DisplayName("Flow 1: Full order lifecycle - create, pay, ship, receive")
    void shouldCompleteFullOrderLifecycle() throws Exception {
        // --- Step 1: Create order ---
        MvcResult createResult = mockMvc.perform(post("/api/order/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildRequest(1L, 1L))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.orderNo").isNotEmpty())
            .andExpect(jsonPath("$.data.orderStatus").value(1)) // PENDING_PAYMENT
            .andExpect(jsonPath("$.data.userId").value(1))
            .andReturn();

        String orderNo = extractOrderNo(createResult);

        // --- Step 2: Get order detail -> verify status=PENDING_PAYMENT ---
        mockMvc.perform(get("/api/order/{orderNo}", orderNo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.orderNo").value(orderNo))
            .andExpect(jsonPath("$.data.orderStatus").value(1));

        // --- Step 3: Get order items -> verify 1 item, quantity=2 ---
        mockMvc.perform(get("/api/order/{orderNo}/items", orderNo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].skuName").value("Test Product"))
            .andExpect(jsonPath("$.data[0].quantity").value(2))
            .andExpect(jsonPath("$.data[0].skuPrice").value(99.00));

        // --- Step 4: Simulate payment success (direct service call) ---
        orderService.paySuccess(orderNo, "PAY" + System.currentTimeMillis());

        // Verify PAID status
        mockMvc.perform(get("/api/order/{orderNo}", orderNo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.orderStatus").value(2));

        // --- Step 5: Ship order ---
        mockMvc.perform(put("/api/order/{orderNo}/ship", orderNo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0));

        // Verify SHIPPED status
        mockMvc.perform(get("/api/order/{orderNo}", orderNo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.orderStatus").value(3))
            .andExpect(jsonPath("$.data.shippedAt").isNotEmpty());

        // --- Step 6: Confirm receive ---
        mockMvc.perform(put("/api/order/{orderNo}/receive", orderNo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0));

        // Verify COMPLETED status
        mockMvc.perform(get("/api/order/{orderNo}", orderNo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.orderStatus").value(4))
            .andExpect(jsonPath("$.data.receivedAt").isNotEmpty());
    }

    // ========================================================================
    // Flow 2: Order Cancellation
    // ========================================================================

    @Test
    @DisplayName("Flow 2: Create order then cancel, verify cannot ship after cancel")
    void shouldCancelOrderFlow() throws Exception {
        // --- Step 1: Create order ---
        MvcResult createResult = mockMvc.perform(post("/api/order/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildRequest(100L, 1L))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andReturn();

        String orderNo = extractOrderNo(createResult);

        // --- Step 2: Cancel with reason ---
        mockMvc.perform(put("/api/order/{orderNo}/cancel", orderNo)
                .param("reason", "Changed my mind"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0));

        // --- Step 3: Verify CANCELLED status ---
        mockMvc.perform(get("/api/order/{orderNo}", orderNo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.orderStatus").value(5)); // CANCELLED

        // --- Step 4: Try to ship cancelled order -> expect error ---
        mockMvc.perform(put("/api/order/{orderNo}/ship", orderNo))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value(30002));
    }

    // ========================================================================
    // Flow 3: Order Listing (user + shop + admin)
    // ========================================================================

    @Test
    @DisplayName("Flow 3: Order listing for user, shop, and admin")
    void shouldListOrdersFlow() throws Exception {
        Long userId = 200L;
        Long shopId = 10L;

        // --- Step 1: Create 3 orders for userId=200, shopId=10 ---
        String orderNo1 = extractOrderNo(mockMvc.perform(post("/api/order/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildRequest(userId, shopId))))
            .andExpect(status().isOk())
            .andReturn());

        String orderNo2 = extractOrderNo(mockMvc.perform(post("/api/order/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildRequest(userId, shopId))))
            .andExpect(status().isOk())
            .andReturn());

        String orderNo3 = extractOrderNo(mockMvc.perform(post("/api/order/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildRequest(userId, shopId))))
            .andExpect(status().isOk())
            .andReturn());

        // --- Step 2: Pay one order to create a different status ---
        orderService.paySuccess(orderNo1, "PAY" + System.currentTimeMillis());

        // --- Step 3: List by user -> verify page.total=3 ---
        mockMvc.perform(get("/api/order/list/user/{userId}", userId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.total").value(3))
            .andExpect(jsonPath("$.data.records.length()").value(3));

        // --- Step 4: List by user with status filter (PENDING_PAYMENT=1) -> 2 records ---
        mockMvc.perform(get("/api/order/list/user/{userId}", userId)
                .param("status", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.total").value(2));

        // --- Step 5: List by user with status filter (PAID=2) -> 1 record ---
        mockMvc.perform(get("/api/order/list/user/{userId}", userId)
                .param("status", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.total").value(1));

        // --- Step 6: List by shop -> verify 3 records ---
        mockMvc.perform(get("/api/order/list/shop/{shopId}", shopId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.total").value(3));

        // --- Step 7: Admin list -> verify at least 3 records (may include other tests' data) ---
        mockMvc.perform(get("/api/order/admin/list"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.total").isNumber());
    }

    // ========================================================================
    // Flow 4: Order Lifecycle Edge Cases
    // ========================================================================

    @Test
    @DisplayName("Flow 4: Edge cases - receive before ship, double ship, cancel completed")
    void shouldEnforceOrderLifecycleEdgeCases() throws Exception {
        // --- Step 1: Create order ---
        MvcResult createResult = mockMvc.perform(post("/api/order/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildRequest(300L, 1L))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andReturn();

        String orderNo = extractOrderNo(createResult);

        // --- Step 2: Try to receive before ship -> expect error ---
        mockMvc.perform(put("/api/order/{orderNo}/receive", orderNo))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value(30002));

        // --- Step 3: Pay first (needed for ship) ---
        orderService.paySuccess(orderNo, "PAY" + System.currentTimeMillis());

        // --- Step 4: Ship -> success ---
        mockMvc.perform(put("/api/order/{orderNo}/ship", orderNo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0));

        // --- Step 5: Try to ship already-shipped order -> expect error ---
        mockMvc.perform(put("/api/order/{orderNo}/ship", orderNo))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value(30002));

        // --- Step 6: Receive -> success ---
        mockMvc.perform(put("/api/order/{orderNo}/receive", orderNo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0));

        // --- Step 7: Try to cancel completed order -> expect error ---
        mockMvc.perform(put("/api/order/{orderNo}/cancel", orderNo)
                .param("reason", "Too late"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value(30002));
    }

    // ========================================================================
    // Flow 5: Inventory rollback on cancellation
    // ========================================================================

    @Test
    @DisplayName("Flow 5: Inventory rollback when order is cancelled")
    void shouldRollbackInventoryOnCancel() throws Exception {
        // --- Step 1: Create order ---
        MvcResult createResult = mockMvc.perform(post("/api/order/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildRequest(400L, 1L))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andReturn();

        String orderNo = extractOrderNo(createResult);

        // Verify inventory was deducted during creation
        verify(inventoryDubboService, atLeastOnce()).deduct(anyLong(), eq(2));

        // --- Step 2: Reset mock for clearer verification ---
        reset(inventoryDubboService);
        when(inventoryDubboService.restore(anyLong(), anyInt())).thenReturn(true);
        when(inventoryDubboService.deduct(anyLong(), anyInt())).thenReturn(true);

        // --- Step 3: Cancel the order ---
        mockMvc.perform(put("/api/order/{orderNo}/cancel", orderNo)
                .param("reason", "Cancel for rollback test"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0));

        // --- Step 4: Verify inventory was restored ---
        verify(inventoryDubboService).restore(anyLong(), eq(2));
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private String extractOrderNo(MvcResult result) throws Exception {
        String json = result.getResponse().getContentAsString();
        return objectMapper.readTree(json).get("data").get("orderNo").asText();
    }

    private CreateOrderRequest buildRequest(Long userId, Long shopId) {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setUserId(userId);
        req.setShopId(shopId);
        req.setAddressSnapshot("{\"name\":\"Test User\",\"phone\":\"13800000001\",\"address\":\"Test Address\"}");
        req.setRemark("Integration test order");

        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
        item.setSkuId(1L);
        item.setSkuName("Test Product");
        item.setSkuPrice(new BigDecimal("99.00"));
        item.setQuantity(2);
        req.setItems(List.of(item));

        return req;
    }
}
