package com.supermarket.order.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supermarket.common.core.result.R;
import com.supermarket.common.dubbo.api.inventory.InventoryDubboService;
import com.supermarket.common.dubbo.api.order.dto.CreateOrderRequest;
import com.supermarket.order.entity.Order;
import com.supermarket.order.entity.OrderItem;
import com.supermarket.order.service.OrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * REAL HTTP integration tests for the Order domain.
 *
 * Uses TestRestTemplate (real HTTP) + real MySQL (via the "integration-test" profile).
 * NO MockMvc, NO H2, NO @MockBean for services (except external Dubbo dependencies).
 * Cleans up test data after each test via @AfterEach truncation instead of @Transactional
 * because TestRestTemplate calls run in separate HTTP threads/transactions.
 */
@SpringBootTest(
    classes = {OrderIntegrationTest.TestConfig.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"spring.cloud.bootstrap.enabled=false"}
)
@ActiveProfiles("integration-test")
class OrderIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderService orderService;

    @Autowired
    private DataSource dataSource;

    @MockBean
    private InventoryDubboService inventoryDubboService;

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate = new JdbcTemplate(dataSource);

        // OrderServiceImpl has @DubboReference for inventoryDubboService,
        // which is not processed when Dubbo auto-config is excluded.
        OrderService target = AopTestUtils.getUltimateTargetObject(orderService);
        ReflectionTestUtils.setField(target, "inventoryDubboService", inventoryDubboService);

        when(inventoryDubboService.deduct(anyLong(), anyInt())).thenReturn(true);
        when(inventoryDubboService.restore(anyLong(), anyInt())).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        // Clean all test data from tables to leave no traces
        jdbcTemplate.execute("DELETE FROM order_items");
        jdbcTemplate.execute("DELETE FROM orders");
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
    // Helper Methods
    // ========================================================================

    private CreateOrderRequest buildCreateRequest(Long userId, Long shopId, int itemCount) {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setUserId(userId);
        req.setShopId(shopId);
        req.setAddressSnapshot("{\"name\":\"Test User\",\"phone\":\"13800000001\",\"address\":\"Test Address\"}");
        req.setRemark("Integration test order");

        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
        item.setSkuId(100L);
        item.setSkuName("Test Product 0");
        item.setSkuPrice(new BigDecimal("99.00"));
        item.setQuantity(2);
        req.setItems(new java.util.ArrayList<>(List.of(item)));

        return req;
    }

    private CreateOrderRequest buildCreateRequest(Long userId, Long shopId) {
        return buildCreateRequest(userId, shopId, 1);
    }

    // ========================================================================
    // Test 1: Complete Order Lifecycle
    // ========================================================================

    @Test
    @DisplayName("Complete lifecycle: create -> detail -> items -> pay -> ship -> receive -> COMPLETED")
    void shouldCompleteOrderLifecycle() {
        // Step 1: Create order via HTTP POST
        ResponseEntity<R<Order>> createResp = restTemplate.exchange(
            "/api/order/create",
            HttpMethod.POST,
            new HttpEntity<>(buildCreateRequest(1L, 1L)),
            new ParameterizedTypeReference<R<Order>>() {}
        );
        assertThat(createResp.getBody().isSuccess()).isTrue();
        assertThat(createResp.getBody().getData().getOrderNo()).isNotBlank();
        assertThat(createResp.getBody().getData().getOrderStatus()).isEqualTo(1);
        assertThat(createResp.getBody().getData().getUserId()).isEqualTo(1);
        String orderNo = createResp.getBody().getData().getOrderNo();

        // Step 2: Get order detail via HTTP GET
        ResponseEntity<R<Order>> detailResp = restTemplate.exchange(
            "/api/order/{orderNo}",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<R<Order>>() {},
            orderNo
        );
        assertThat(detailResp.getBody().isSuccess()).isTrue();
        assertThat(detailResp.getBody().getData().getOrderNo()).isEqualTo(orderNo);
        assertThat(detailResp.getBody().getData().getOrderStatus()).isEqualTo(1);

        // Step 3: Get order items via HTTP GET
        ResponseEntity<R<List<OrderItem>>> itemsResp = restTemplate.exchange(
            "/api/order/{orderNo}/items",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<R<List<OrderItem>>>() {},
            orderNo
        );
        assertThat(itemsResp.getBody().isSuccess()).isTrue();
        assertThat(itemsResp.getBody().getData()).hasSize(1);
        assertThat(itemsResp.getBody().getData().get(0).getSkuName()).isEqualTo("Test Product 0");
        assertThat(itemsResp.getBody().getData().get(0).getQuantity()).isEqualTo(2);

        // Step 4: Simulate payment success (direct service call — no public HTTP endpoint)
        orderService.paySuccess(orderNo, "PAY" + System.currentTimeMillis());

        // Verify PAID status via service directly (same JVM, committed data)
        Order paidOrder = orderService.getByOrderNo(orderNo);
        assertThat(paidOrder.getOrderStatus()).isEqualTo(2);

        // Also verify via HTTP
        detailResp = restTemplate.exchange(
            "/api/order/{orderNo}",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<R<Order>>() {},
            orderNo
        );
        assertThat(detailResp.getBody().getData().getOrderStatus()).isEqualTo(2);

        // Step 5: Ship order via HTTP PUT
        ResponseEntity<R<Void>> shipResp = restTemplate.exchange(
            "/api/order/{orderNo}/ship",
            HttpMethod.PUT,
            null,
            new ParameterizedTypeReference<R<Void>>() {},
            orderNo
        );
        assertThat(shipResp.getBody().isSuccess()).isTrue();

        // Verify SHIPPED status
        detailResp = restTemplate.exchange(
            "/api/order/{orderNo}",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<R<Order>>() {},
            orderNo
        );
        assertThat(detailResp.getBody().getData().getOrderStatus()).isEqualTo(3);
        assertThat(detailResp.getBody().getData().getShippedAt()).isNotNull();

        // Step 6: Confirm receive via HTTP PUT
        ResponseEntity<R<Void>> receiveResp = restTemplate.exchange(
            "/api/order/{orderNo}/receive",
            HttpMethod.PUT,
            null,
            new ParameterizedTypeReference<R<Void>>() {},
            orderNo
        );
        assertThat(receiveResp.getBody().isSuccess()).isTrue();

        // Verify COMPLETED status
        detailResp = restTemplate.exchange(
            "/api/order/{orderNo}",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<R<Order>>() {},
            orderNo
        );
        assertThat(detailResp.getBody().getData().getOrderStatus()).isEqualTo(4);
        assertThat(detailResp.getBody().getData().getReceivedAt()).isNotNull();
    }

    // ========================================================================
    // Test 2: Cancel Flow
    // ========================================================================

    @Test
    @DisplayName("Cancel flow: create -> cancel -> verify CANCELLED -> ship rejected")
    void shouldCancelOrderFlow() {
        // Step 1: Create order
        ResponseEntity<R<Order>> createResp = restTemplate.exchange(
            "/api/order/create",
            HttpMethod.POST,
            new HttpEntity<>(buildCreateRequest(100L, 1L)),
            new ParameterizedTypeReference<R<Order>>() {}
        );
        assertThat(createResp.getBody().isSuccess()).isTrue();
        String orderNo = createResp.getBody().getData().getOrderNo();

        // Step 2: Cancel with reason
        ResponseEntity<R<Void>> cancelResp = restTemplate.exchange(
            "/api/order/{orderNo}/cancel?reason={reason}",
            HttpMethod.PUT,
            null,
            new ParameterizedTypeReference<R<Void>>() {},
            orderNo,
            "Changed my mind"
        );
        assertThat(cancelResp.getBody().isSuccess()).isTrue();

        // Step 3: Verify CANCELLED status via HTTP
        ResponseEntity<R<Order>> detailResp = restTemplate.exchange(
            "/api/order/{orderNo}",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<R<Order>>() {},
            orderNo
        );
        assertThat(detailResp.getBody().getData().getOrderStatus()).isEqualTo(5);

        // Step 4: Try to ship cancelled order -> expect ORDER_STATUS_ERROR (30002)
        ResponseEntity<R<Void>> shipResp = restTemplate.exchange(
            "/api/order/{orderNo}/ship",
            HttpMethod.PUT,
            null,
            new ParameterizedTypeReference<R<Void>>() {},
            orderNo
        );
        assertThat(shipResp.getBody().getCode()).isEqualTo(30002);
    }

    // ========================================================================
    // Test 3: Order Listing Flows
    // ========================================================================

    @Test
    @DisplayName("List flow: create 3 orders -> listByUser -> filter -> listByShop -> adminList")
    void shouldListOrdersFlow() throws Exception {
        Long userId = 200L;
        Long shopId = 10L;

        // Step 1: Create 3 orders
        ResponseEntity<R<Order>> o1 = restTemplate.exchange(
            "/api/order/create", HttpMethod.POST,
            new HttpEntity<>(buildCreateRequest(userId, shopId)),
            new ParameterizedTypeReference<R<Order>>() {}
        );
        String orderNo1 = o1.getBody().getData().getOrderNo();

        ResponseEntity<R<Order>> o2 = restTemplate.exchange(
            "/api/order/create", HttpMethod.POST,
            new HttpEntity<>(buildCreateRequest(userId, shopId)),
            new ParameterizedTypeReference<R<Order>>() {}
        );

        ResponseEntity<R<Order>> o3 = restTemplate.exchange(
            "/api/order/create", HttpMethod.POST,
            new HttpEntity<>(buildCreateRequest(userId, shopId)),
            new ParameterizedTypeReference<R<Order>>() {}
        );

        // Step 2: Pay one order to create a different status
        orderService.paySuccess(orderNo1, "PAY" + System.currentTimeMillis());

        // Step 3: ListByUser -> verify 3 total records
        ResponseEntity<String> userListResp = restTemplate.exchange(
            "/api/order/list/user/{userId}",
            HttpMethod.GET, null, String.class, userId
        );
        JsonNode userListData = objectMapper.readTree(userListResp.getBody()).get("data");
        assertThat(userListData.get("total").asInt()).isEqualTo(3);
        assertThat(userListData.get("records").size()).isEqualTo(3);

        // Step 4: ListByUser with status filter PAID(=2) -> 1 record
        ResponseEntity<String> paidListResp = restTemplate.exchange(
            "/api/order/list/user/{userId}?status={status}",
            HttpMethod.GET, null, String.class, userId, 2
        );
        JsonNode paidListData = objectMapper.readTree(paidListResp.getBody()).get("data");
        assertThat(paidListData.get("total").asInt()).isEqualTo(1);

        // Step 5: ListByUser with status filter PENDING_PAYMENT(=1) -> 2 records
        ResponseEntity<String> pendingListResp = restTemplate.exchange(
            "/api/order/list/user/{userId}?status={status}",
            HttpMethod.GET, null, String.class, userId, 1
        );
        JsonNode pendingListData = objectMapper.readTree(pendingListResp.getBody()).get("data");
        assertThat(pendingListData.get("total").asInt()).isEqualTo(2);

        // Step 6: ListByShop -> verify 3 records
        ResponseEntity<String> shopListResp = restTemplate.exchange(
            "/api/order/list/shop/{shopId}",
            HttpMethod.GET, null, String.class, shopId
        );
        JsonNode shopListData = objectMapper.readTree(shopListResp.getBody()).get("data");
        assertThat(shopListData.get("total").asInt()).isEqualTo(3);

        // Step 7: AdminList -> verify at least 3 records
        ResponseEntity<String> adminListResp = restTemplate.exchange(
            "/api/order/admin/list",
            HttpMethod.GET, null, String.class
        );
        JsonNode adminListData = objectMapper.readTree(adminListResp.getBody()).get("data");
        assertThat(adminListData.get("total").asInt()).isGreaterThanOrEqualTo(3);
    }

    // ========================================================================
    // Test 4: Edge Cases
    // ========================================================================

    @Test
    @DisplayName("Edge cases: receive before ship -> 400, double ship -> 400, cancel completed -> 400")
    void shouldEnforceOrderLifecycleEdgeCases() {
        // Step 1: Create order
        ResponseEntity<R<Order>> createResp = restTemplate.exchange(
            "/api/order/create", HttpMethod.POST,
            new HttpEntity<>(buildCreateRequest(300L, 1L)),
            new ParameterizedTypeReference<R<Order>>() {}
        );
        assertThat(createResp.getBody().isSuccess()).isTrue();
        String orderNo = createResp.getBody().getData().getOrderNo();

        // Step 2: Try to receive before ship -> expect ORDER_STATUS_ERROR (30002)
        ResponseEntity<R<Void>> earlyReceiveResp = restTemplate.exchange(
            "/api/order/{orderNo}/receive",
            HttpMethod.PUT, null,
            new ParameterizedTypeReference<R<Void>>() {}, orderNo
        );
        assertThat(earlyReceiveResp.getBody().getCode()).isEqualTo(30002);

        // Step 3: Pay (needed for ship)
        orderService.paySuccess(orderNo, "PAY" + System.currentTimeMillis());

        // Step 4: Ship -> success
        ResponseEntity<R<Void>> shipResp = restTemplate.exchange(
            "/api/order/{orderNo}/ship",
            HttpMethod.PUT, null,
            new ParameterizedTypeReference<R<Void>>() {}, orderNo
        );
        assertThat(shipResp.getBody().isSuccess()).isTrue();

        // Step 5: Try to ship again -> expect ORDER_STATUS_ERROR (30002)
        ResponseEntity<R<Void>> doubleShipResp = restTemplate.exchange(
            "/api/order/{orderNo}/ship",
            HttpMethod.PUT, null,
            new ParameterizedTypeReference<R<Void>>() {}, orderNo
        );
        assertThat(doubleShipResp.getBody().getCode()).isEqualTo(30002);

        // Step 6: Receive -> success
        ResponseEntity<R<Void>> receiveResp = restTemplate.exchange(
            "/api/order/{orderNo}/receive",
            HttpMethod.PUT, null,
            new ParameterizedTypeReference<R<Void>>() {}, orderNo
        );
        assertThat(receiveResp.getBody().isSuccess()).isTrue();

        // Step 7: Try to cancel completed order -> expect ORDER_STATUS_ERROR (30002)
        ResponseEntity<R<Void>> cancelResp = restTemplate.exchange(
            "/api/order/{orderNo}/cancel?reason={reason}",
            HttpMethod.PUT, null,
            new ParameterizedTypeReference<R<Void>>() {}, orderNo, "Too late"
        );
        assertThat(cancelResp.getBody().getCode()).isEqualTo(30002);
    }

    // ========================================================================
    // Test 5: Order Items
    // ========================================================================

    @Test
    @DisplayName("Order items: create order with 2 items -> verify items endpoint returns both")
    void shouldReturnMultipleOrderItems() {
        // Step 1: Create order with 2 items
        CreateOrderRequest twoItemReq = new CreateOrderRequest();
        twoItemReq.setUserId(400L);
        twoItemReq.setShopId(1L);
        twoItemReq.setAddressSnapshot("{\"name\":\"Test User\",\"phone\":\"13800000001\",\"address\":\"Test Address\"}");
        twoItemReq.setRemark("Two items test");
        CreateOrderRequest.OrderItemRequest item0 = new CreateOrderRequest.OrderItemRequest();
        item0.setSkuId(100L); item0.setSkuName("Test Product 0"); item0.setSkuPrice(new BigDecimal("99.00")); item0.setQuantity(2);
        CreateOrderRequest.OrderItemRequest item1 = new CreateOrderRequest.OrderItemRequest();
        item1.setSkuId(101L); item1.setSkuName("Test Product 1"); item1.setSkuPrice(new BigDecimal("49.50")); item1.setQuantity(3);
        twoItemReq.setItems(List.of(item0, item1));

        ResponseEntity<R<Order>> createResp = restTemplate.exchange(
            "/api/order/create", HttpMethod.POST,
            new HttpEntity<>(twoItemReq),
            new ParameterizedTypeReference<R<Order>>() {}
        );
        assertThat(createResp.getBody().isSuccess()).isTrue();
        String orderNo = createResp.getBody().getData().getOrderNo();

        // Step 2: Get items -> verify 2 items with correct names
        ResponseEntity<R<List<OrderItem>>> itemsResp = restTemplate.exchange(
            "/api/order/{orderNo}/items",
            HttpMethod.GET, null,
            new ParameterizedTypeReference<R<List<OrderItem>>>() {}, orderNo
        );
        assertThat(itemsResp.getBody().isSuccess()).isTrue();
        List<OrderItem> items = itemsResp.getBody().getData();
        assertThat(items).hasSize(2);
        assertThat(items.get(0).getSkuName()).isEqualTo("Test Product 0");
        assertThat(items.get(0).getSkuPrice()).isEqualByComparingTo(new BigDecimal("99.00"));
        assertThat(items.get(0).getQuantity()).isEqualTo(2);
        assertThat(items.get(1).getSkuName()).isEqualTo("Test Product 1");
        assertThat(items.get(1).getSkuPrice()).isEqualByComparingTo(new BigDecimal("49.50"));
        assertThat(items.get(1).getQuantity()).isEqualTo(3);
    }
}
