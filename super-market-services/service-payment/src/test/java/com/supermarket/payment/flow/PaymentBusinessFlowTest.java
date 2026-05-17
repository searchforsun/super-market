package com.supermarket.payment.flow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supermarket.common.dubbo.api.order.OrderDubboService;
import com.supermarket.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.mybatis.spring.annotation.MapperScan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Postman-style business flow tests for the Payment domain.
 *
 * Chains payment creation, callback, query, and refund API calls to verify
 * the complete payment lifecycle. Uses H2 in-memory database with
 * @Transactional rollback between tests.
 */
@SpringBootTest(
    classes = {PaymentBusinessFlowTest.TestConfig.class},
    properties = {"spring.cloud.bootstrap.enabled=false"}
)
@AutoConfigureMockMvc
@ActiveProfiles("flow-test")
@Transactional
class PaymentBusinessFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentService paymentService;

    @MockBean
    private OrderDubboService orderDubboService;

    @BeforeEach
    void setUp() {
        // PaymentServiceImpl uses @DubboReference for orderDubboService,
        // which is not processed when Dubbo auto-config is excluded.
        // Spring may wrap the service in an AOP proxy (@Transactional), so
        // we unwrap to set the field on the actual target object.
        PaymentService target = AopTestUtils.getUltimateTargetObject(paymentService);
        ReflectionTestUtils.setField(target, "orderDubboService", orderDubboService);

        // OrderDubboService.updateStatus is void, so Mockito default is fine.
        // No explicit stubbing needed for void methods.
    }

    @Configuration
    @MapperScan("com.supermarket.payment.mapper")
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
        "com.supermarket.payment.controller",
        "com.supermarket.payment.service",
        "com.supermarket.payment.mapper",
        "com.supermarket.common.mybatis",
        "com.supermarket.common.web.handler",
        "com.supermarket.common.web.config",
        "com.supermarket.common.web.interceptor"
    })
    static class TestConfig {
    }

    // ========================================================================
    // Flow 1: Complete Payment Lifecycle (create -> callback -> query -> refund)
    // ========================================================================

    @Test
    @DisplayName("Flow 1: Full payment lifecycle - create, callback, query, refund")
    void shouldCompletePaymentLifecycle() throws Exception {
        String orderNo = "ORD-FLOW-TEST-001";
        Long userId = 1L;
        BigDecimal amount = new BigDecimal("198.00");
        Integer payMethod = 1;

        // --- Step 1: Create payment ---
        MvcResult createResult = mockMvc.perform(post("/api/payment/pay")
                .param("orderNo", orderNo)
                .param("userId", String.valueOf(userId))
                .param("amount", amount.toPlainString())
                .param("payMethod", String.valueOf(payMethod)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.payNo").isNotEmpty())
            .andExpect(jsonPath("$.data.orderNo").value(orderNo))
            .andExpect(jsonPath("$.data.payStatus").value(1))
            .andExpect(jsonPath("$.data.amount").value(198.00))
            .andReturn();

        String payNo = extractPayNo(createResult);

        // Verify the payment is stored in the database
        assertThat(paymentService.getByPayNoEntity(payNo)).isNotNull();
        assertThat(paymentService.getByPayNoEntity(payNo).getPayStatus()).isEqualTo(1);

        // --- Step 2: Query payment by payNo ---
        mockMvc.perform(get("/api/payment/{payNo}", payNo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.payNo").value(payNo))
            .andExpect(jsonPath("$.data.payStatus").value(1));

        // --- Step 3: Mock payment callback ---
        String requestId = "REQ-" + System.currentTimeMillis();
        mockMvc.perform(post("/api/payment/callback/mock")
                .param("requestId", requestId)
                .param("payNo", payNo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data").value("success"));

        // Verify the order status update was called via Dubbo
        verify(orderDubboService, times(1)).updateStatus(orderNo, 2);

        // --- Step 4: Query payment after callback -> payStatus should be 2 (PAID) ---
        mockMvc.perform(get("/api/payment/{payNo}", payNo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.payStatus").value(2))
            .andExpect(jsonPath("$.data.thirdPayNo").isNotEmpty())
            .andExpect(jsonPath("$.data.paidAt").isNotEmpty());

        // --- Step 5: Refund ---
        BigDecimal refundAmount = new BigDecimal("198.00");
        mockMvc.perform(post("/api/payment/refund")
                .param("orderNo", orderNo)
                .param("refundAmount", refundAmount.toPlainString())
                .param("reason", "Product quality issue"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.refundNo").isNotEmpty())
            .andExpect(jsonPath("$.data.orderNo").value(orderNo))
            .andExpect(jsonPath("$.data.refundAmount").value(198.00))
            .andExpect(jsonPath("$.data.refundStatus").value(2))
            .andReturn();

        // Verify the order status update was called for refund cancellation
        verify(orderDubboService, times(1)).updateStatus(orderNo, 6);

        // --- Step 6: Query payment after refund -> payStatus should be 4 (REFUNDED) ---
        mockMvc.perform(get("/api/payment/{payNo}", payNo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.payStatus").value(4));
    }

    // ========================================================================
    // Flow 2: Payment Edge Cases
    // ========================================================================

    @Test
    @DisplayName("Flow 2: Payment edge cases - duplicate payment, query not found")
    void shouldEnforcePaymentEdgeCases() throws Exception {
        String orderNo = "ORD-EDGE-001";
        Long userId = 2L;
        BigDecimal amount = new BigDecimal("99.00");

        // --- Step 1: Create payment ---
        MvcResult createResult = mockMvc.perform(post("/api/payment/pay")
                .param("orderNo", orderNo)
                .param("userId", String.valueOf(userId))
                .param("amount", amount.toPlainString())
                .param("payMethod", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andReturn();

        String payNo = extractPayNo(createResult);

        // --- Step 2: Try to create duplicate payment for same order -> expect PAYMENT_DUPLICATE ---
        mockMvc.perform(post("/api/payment/pay")
                .param("orderNo", orderNo)
                .param("userId", String.valueOf(userId))
                .param("amount", amount.toPlainString())
                .param("payMethod", "1"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value(30004));

        // --- Step 3: Query non-existent payment -> expect PAYMENT_NOT_FOUND ---
        mockMvc.perform(get("/api/payment/PAY-NOT-EXIST"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value(30005));
    }

    // ========================================================================
    // Flow 3: Payment Callback Idempotency
    // ========================================================================

    @Test
    @DisplayName("Flow 3: Repeated callback is idempotent")
    void shouldHandleIdempotentCallback() throws Exception {
        String orderNo = "ORD-IDEM-001";
        Long userId = 3L;
        BigDecimal amount = new BigDecimal("50.00");

        // --- Step 1: Create payment ---
        MvcResult createResult = mockMvc.perform(post("/api/payment/pay")
                .param("orderNo", orderNo)
                .param("userId", String.valueOf(userId))
                .param("amount", amount.toPlainString())
                .param("payMethod", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andReturn();

        String payNo = extractPayNo(createResult);

        // --- Step 2: First callback ---
        String requestId = "IDEM-REQ-" + System.currentTimeMillis();
        mockMvc.perform(post("/api/payment/callback/mock")
                .param("requestId", requestId)
                .param("payNo", payNo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value("success"));

        // Verify order status was updated once
        verify(orderDubboService, times(1)).updateStatus(orderNo, 2);

        // --- Step 3: Repeated callback with same requestId -> also success (idempotent) ---
        mockMvc.perform(post("/api/payment/callback/mock")
                .param("requestId", requestId)
                .param("payNo", payNo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value("success"));

        // Order status should still be updated only once (idempotent)
        verify(orderDubboService, times(1)).updateStatus(orderNo, 2);

        // Payment status should still be PAID, not changed again
        mockMvc.perform(get("/api/payment/{payNo}", payNo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.payStatus").value(2));
    }

    // ========================================================================
    // Flow 4: Refund Edge Cases
    // ========================================================================

    @Test
    @DisplayName("Flow 4: Refund on unpaid payment -> error")
    void shouldRejectRefundForUnpaidPayment() throws Exception {
        String orderNo = "ORD-REFUND-ERR-001";
        Long userId = 4L;
        BigDecimal amount = new BigDecimal("75.00");

        // --- Step 1: Create payment (status = 1, unpaid) ---
        MvcResult createResult = mockMvc.perform(post("/api/payment/pay")
                .param("orderNo", orderNo)
                .param("userId", String.valueOf(userId))
                .param("amount", amount.toPlainString())
                .param("payMethod", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andReturn();

        // --- Step 2: Try to refund unpaid payment -> expect REFUND_ONLY_PAID ---
        mockMvc.perform(post("/api/payment/refund")
                .param("orderNo", orderNo)
                .param("refundAmount", "75.00")
                .param("reason", "Test refund on unpaid"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value(30007));
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private String extractPayNo(MvcResult result) throws Exception {
        String json = result.getResponse().getContentAsString();
        return objectMapper.readTree(json).get("data").get("payNo").asText();
    }
}
