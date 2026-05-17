package com.supermarket.payment.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supermarket.common.core.result.R;
import com.supermarket.common.dubbo.api.order.OrderDubboService;
import com.supermarket.payment.entity.Payment;
import com.supermarket.payment.entity.PaymentRefund;
import com.supermarket.payment.service.PaymentService;
import jakarta.annotation.PostConstruct;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import javax.sql.DataSource;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(
    classes = {PaymentIntegrationTest.TestConfig.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"spring.cloud.bootstrap.enabled=false"}
)
@ActiveProfiles("integration-test")
class PaymentIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private DataSource dataSource;

    @MockBean
    private OrderDubboService orderDubboService;

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate = new JdbcTemplate(dataSource);

        // OrderDubboService.updateStatus is void, make it a no-op
        doNothing().when(orderDubboService).updateStatus(anyString(), any(Integer.class));
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM payment_idempotent");
        jdbcTemplate.execute("DELETE FROM payment_refunds");
        jdbcTemplate.execute("DELETE FROM payments");
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

        @Autowired
        private PaymentService paymentService;

        @Autowired
        private OrderDubboService orderDubboService;

        @PostConstruct
        void injectDubboMock() {
            // PaymentServiceImpl uses @DubboReference for orderDubboService,
            // which is not processed when Dubbo auto-config is excluded.
            // Inject the @MockBean into the raw target object during context init,
            // before any HTTP requests can be made.
            PaymentService target = AopTestUtils.getUltimateTargetObject(paymentService);
            ReflectionTestUtils.setField(target, "orderDubboService", orderDubboService);
        }
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private HttpEntity<MultiValueMap<String, String>> formEntity(MultiValueMap<String, String> params) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return new HttpEntity<>(params, headers);
    }

    private ResponseEntity<R<Payment>> createPayment(String orderNo, Long userId, BigDecimal amount) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("orderNo", orderNo);
        params.add("userId", String.valueOf(userId));
        params.add("amount", amount.toPlainString());
        params.add("payMethod", "1");

        return restTemplate.exchange(
            "/api/payment/pay",
            HttpMethod.POST,
            formEntity(params),
            new ParameterizedTypeReference<R<Payment>>() {}
        );
    }

    private ResponseEntity<R<Payment>> queryPayment(String payNo) {
        return restTemplate.exchange(
            "/api/payment/{payNo}",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<R<Payment>>() {},
            payNo
        );
    }

    // ========================================================================
    // Test 1: Complete Payment Lifecycle
    // ========================================================================

    @Test
    @DisplayName("Pay lifecycle: create -> query -> callback -> query paid -> refund -> query refunded")
    void shouldCompletePaymentLifecycle() throws Exception {
        String orderNo = "ORD-IT-PAY-001";
        Long userId = 1L;
        BigDecimal amount = new BigDecimal("198.00");

        // Step 1: Create payment
        ResponseEntity<R<Payment>> createResp = createPayment(orderNo, userId, amount);
        assertThat(createResp.getBody().isSuccess()).isTrue();
        assertThat(createResp.getBody().getData().getPayNo()).isNotBlank();
        assertThat(createResp.getBody().getData().getOrderNo()).isEqualTo(orderNo);
        assertThat(createResp.getBody().getData().getPayStatus()).isEqualTo(1);
        assertThat(createResp.getBody().getData().getAmount()).isEqualByComparingTo(new BigDecimal("198.00"));

        String payNo = createResp.getBody().getData().getPayNo();

        // Step 2: Query payment by payNo
        ResponseEntity<R<Payment>> queryResp = queryPayment(payNo);
        assertThat(queryResp.getBody().isSuccess()).isTrue();
        assertThat(queryResp.getBody().getData().getPayNo()).isEqualTo(payNo);
        assertThat(queryResp.getBody().getData().getPayStatus()).isEqualTo(1);

        // Step 3: Mock payment callback
        String requestId = "REQ-" + System.currentTimeMillis();
        MultiValueMap<String, String> callbackParams = new LinkedMultiValueMap<>();
        callbackParams.add("requestId", requestId);
        callbackParams.add("payNo", payNo);

        ResponseEntity<String> callbackRawResp = restTemplate.exchange(
            "/api/payment/callback/mock",
            HttpMethod.POST,
            formEntity(callbackParams),
            String.class
        );
        // Parse the response manually to debug callback issues
        String respBody = callbackRawResp.getBody();
        JsonNode root = objectMapper.readTree(respBody);
        assertThat(root.get("code").asInt())
            .as("Callback failed: body=%s", respBody)
            .isEqualTo(0);
        assertThat(root.get("data").asText()).isEqualTo("success");

        // Verify OrderDubboService.updateStatus was called for payment (status=2)
        verify(orderDubboService, times(1)).updateStatus(orderNo, 2);

        // Step 4: Query payment after callback -> payStatus should be 2 (PAID)
        queryResp = queryPayment(payNo);
        assertThat(queryResp.getBody().getData().getPayStatus()).isEqualTo(2);
        assertThat(queryResp.getBody().getData().getThirdPayNo()).isNotBlank();
        assertThat(queryResp.getBody().getData().getPaidAt()).isNotNull();

        // Step 5: Refund
        MultiValueMap<String, String> refundParams = new LinkedMultiValueMap<>();
        refundParams.add("orderNo", orderNo);
        refundParams.add("refundAmount", "198.00");
        refundParams.add("reason", "Product quality issue");

        ResponseEntity<R<PaymentRefund>> refundResp = restTemplate.exchange(
            "/api/payment/refund",
            HttpMethod.POST,
            formEntity(refundParams),
            new ParameterizedTypeReference<R<PaymentRefund>>() {}
        );
        assertThat(refundResp.getBody().isSuccess()).isTrue();
        assertThat(refundResp.getBody().getData().getRefundNo()).isNotBlank();
        assertThat(refundResp.getBody().getData().getOrderNo()).isEqualTo(orderNo);
        assertThat(refundResp.getBody().getData().getRefundAmount()).isEqualByComparingTo(new BigDecimal("198.00"));
        assertThat(refundResp.getBody().getData().getRefundStatus()).isEqualTo(2);

        // Verify OrderDubboService.updateStatus was called for refund cancellation (status=6)
        verify(orderDubboService, times(1)).updateStatus(orderNo, 6);

        // Step 6: Query payment after refund -> payStatus should be 4 (REFUNDED)
        queryResp = queryPayment(payNo);
        assertThat(queryResp.getBody().getData().getPayStatus()).isEqualTo(4);
    }

    // ========================================================================
    // Test 2: Duplicate Payment
    // ========================================================================

    @Test
    @DisplayName("Duplicate payment: create payment -> try duplicate -> expect 400")
    void shouldRejectDuplicatePayment() {
        String orderNo = "ORD-IT-DUP-001";
        Long userId = 2L;
        BigDecimal amount = new BigDecimal("99.00");

        // Step 1: Create payment -> success
        ResponseEntity<R<Payment>> createResp = createPayment(orderNo, userId, amount);
        assertThat(createResp.getBody().isSuccess()).isTrue();

        // Step 2: Try to create duplicate payment for same order -> expect PAYMENT_DUPLICATE (30004)
        ResponseEntity<R<Payment>> dupResp = createPayment(orderNo, userId, amount);
        assertThat(dupResp.getBody().getCode()).isEqualTo(30004);
    }

    // ========================================================================
    // Test 3: Query Non-Existent Payment
    // ========================================================================

    @Test
    @DisplayName("Not found: query non-existent payNo -> expect 404")
    void shouldReturn404ForNonExistentPayment() {
        ResponseEntity<R<Payment>> queryResp = restTemplate.exchange(
            "/api/payment/{payNo}",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<R<Payment>>() {},
            "PAY-NOT-EXIST"
        );
        assertThat(queryResp.getBody().getCode()).isEqualTo(30005);
    }

    // ========================================================================
    // Test 4: Refund Unpaid Payment
    // ========================================================================

    @Test
    @DisplayName("Refund unpaid: create payment -> try refund without callback -> expect 400")
    void shouldRejectRefundForUnpaidPayment() {
        String orderNo = "ORD-IT-REFUND-ERR-001";
        Long userId = 4L;
        BigDecimal amount = new BigDecimal("75.00");

        // Step 1: Create payment (status=1, unpaid)
        ResponseEntity<R<Payment>> createResp = createPayment(orderNo, userId, amount);
        assertThat(createResp.getBody().isSuccess()).isTrue();

        // Step 2: Try to refund unpaid payment -> expect 400
        MultiValueMap<String, String> refundParams = new LinkedMultiValueMap<>();
        refundParams.add("orderNo", orderNo);
        refundParams.add("refundAmount", "75.00");
        refundParams.add("reason", "Test refund on unpaid");

        ResponseEntity<R<PaymentRefund>> refundResp = restTemplate.exchange(
            "/api/payment/refund",
            HttpMethod.POST,
            formEntity(refundParams),
            new ParameterizedTypeReference<R<PaymentRefund>>() {}
        );
        assertThat(refundResp.getBody().getCode()).isEqualTo(30010);
    }
}
