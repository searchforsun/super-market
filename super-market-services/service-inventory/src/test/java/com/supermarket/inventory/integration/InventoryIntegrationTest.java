package com.supermarket.inventory.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * HTTP integration test for InventoryController via TestRestTemplate with H2.
 * RedissonClient is mocked since Redis middleware is not required for stock CRUD operations.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "dubbo.application.name=test",
        "dubbo.registry.address=N/A",
        "dubbo.protocol.name=tri",
        "dubbo.protocol.port=-1",
        "spring.autoconfigure.exclude=org.redisson.spring.starter.RedissonAutoConfigurationV2"
})
@ActiveProfiles("test")
@Transactional
@DisplayName("Inventory Integration Test")
class InventoryIntegrationTest {

    @MockBean
    private RedissonClient redissonClient;

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeEach
    void setUp() throws InterruptedException {
        RLock mockLock = mock(RLock.class);
        when(mockLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(mockLock.isHeldByCurrentThread()).thenReturn(true);
        when(redissonClient.getLock(anyString())).thenReturn(mockLock);
    }

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should manage stock lifecycle: init -> query -> deduct -> verify -> over-deduct -> query non-existent")
    void shouldManageStockLifecycle() throws Exception {
        long timestamp = System.currentTimeMillis();
        // Use a unique negative SKU ID to avoid collision with seed data
        // (seed data uses IDs 2001-2029, so we use a large unique number)
        Long skuId = 99000L + timestamp;

        // ============================================================
        // Step 1: Initialize stock for a new SKU
        // ============================================================
        ResponseEntity<String> initResp = restTemplate.exchange(
                "/api/inventory/init?skuId={skuId}&totalStock={total}&safetyStock={safety}",
                HttpMethod.POST, null, String.class, skuId, 100, 10);
        assertThat(initResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode initRoot = objectMapper.readTree(initResp.getBody());
        assertThat(initRoot.get("code").asInt())/*FIX:code now 0*/ .isEqualTo(0);
        assertThat(initRoot.get("data").get("skuId").asLong()).isEqualTo(skuId);
        assertThat(initRoot.get("data").get("totalStock").asInt()).isEqualTo(100);
        assertThat(initRoot.get("data").get("availableStock").asInt()).isEqualTo(100);
        assertThat(initRoot.get("data").get("lockedStock").asInt()).isEqualTo(0);
        assertThat(initRoot.get("data").get("safetyStock").asInt()).isEqualTo(10);
        assertThat(initRoot.get("data").get("version").asInt()).isEqualTo(0);

        // ============================================================
        // Step 2: Query inventory by SKU ID
        // ============================================================
        ResponseEntity<String> queryResp = restTemplate.getForEntity(
                "/api/inventory/sku/{skuId}", String.class, skuId);
        assertThat(queryResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode queryRoot = objectMapper.readTree(queryResp.getBody());
        assertThat(queryRoot.get("code").asInt())/*FIX:code now 0*/ .isEqualTo(0);
        assertThat(queryRoot.get("data").get("availableStock").asInt()).isEqualTo(100);

        // ============================================================
        // Step 3: Deduct stock (30 units)
        // ============================================================
        ResponseEntity<String> deductResp = restTemplate.exchange(
                "/api/inventory/deduct?skuId={skuId}&quantity={quantity}",
                HttpMethod.POST, null, String.class, skuId, 30);
        assertThat(deductResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode deductRoot = objectMapper.readTree(deductResp.getBody());
        assertThat(deductRoot.get("code").asInt())/*FIX:code now 0*/ .isEqualTo(0);
        assertThat(deductRoot.get("data").asBoolean()).isTrue();

        // ============================================================
        // Step 4: Verify remaining stock (100 - 30 = 70)
        // ============================================================
        ResponseEntity<String> verifyResp = restTemplate.getForEntity(
                "/api/inventory/sku/{skuId}", String.class, skuId);
        assertThat(verifyResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode verifyRoot = objectMapper.readTree(verifyResp.getBody());
        assertThat(verifyRoot.get("code").asInt())/*FIX:code now 0*/ .isEqualTo(0);
        assertThat(verifyRoot.get("data").get("totalStock").asInt()).isEqualTo(100);
        int available = verifyRoot.get("data").get("availableStock").asInt();
        int locked = verifyRoot.get("data").get("lockedStock").asInt();
        assertThat(available).isEqualTo(70);
        assertThat(locked).isEqualTo(30);
        int currentVersion = verifyRoot.get("data").get("version").asInt();
        assertThat(currentVersion).isPositive();

        // ============================================================
        // Step 5: Over-deduct (1000 units, should fail)
        // ============================================================
        ResponseEntity<String> overDeductResp = restTemplate.exchange(
                "/api/inventory/deduct?skuId={skuId}&quantity={quantity}",
                HttpMethod.POST, null, String.class, skuId, 1000);
        assertThat(overDeductResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode overDeductRoot = objectMapper.readTree(overDeductResp.getBody());
        // Should fail with STOCK_INSUFFICIENT because available = 70 < 1000
        assertThat(overDeductRoot.get("code").asInt()).isEqualTo(20008);

        // ============================================================
        // Step 6: Query non-existent SKU (should return STOCK_NOT_FOUND)
        // ============================================================
        ResponseEntity<String> notFoundResp = restTemplate.getForEntity(
                "/api/inventory/sku/{skuId}", String.class, -99999L);
        assertThat(notFoundResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode notFoundRoot = objectMapper.readTree(notFoundResp.getBody());
        assertThat(notFoundRoot.get("code").asInt()).isEqualTo(20010);

        // ============================================================
        // Step 7: Verify duplicate init fails
        // ============================================================
        ResponseEntity<String> duplicateInitResp = restTemplate.exchange(
                "/api/inventory/init?skuId={skuId}&totalStock={total}&safetyStock={safety}",
                HttpMethod.POST, null, String.class, skuId, 200, 20);
        // GlobalExceptionHandler returns HTTP 200 with STOCK_ALREADY_INIT (20009)
        assertThat(duplicateInitResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode duplicateRoot = objectMapper.readTree(duplicateInitResp.getBody());
        assertThat(duplicateRoot.get("code").asInt()).isEqualTo(20009);
        assertThat(duplicateRoot.get("message").asText()).contains("已初始化");
    }
}
