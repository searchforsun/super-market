package com.supermarket.cart.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real HTTP integration test for CartController.
 * Tests cart add, list, update, select, count, and remove via TestRestTemplate against real Redis.
 * <p>
 * Note: Cart service uses Redis only (no MySQL). @Transactional does not apply.
 * Unique userId per test prevents test interference.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "dubbo.application.name=test",
        "dubbo.registry.address=N/A",
        "dubbo.protocol.name=tri",
        "dubbo.protocol.port=-1"
})
@ActiveProfiles("test")
@DisplayName("Cart Integration Test")
class CartIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private final Long userId = -1L * System.nanoTime(); // Unique negative userId per test run

    @AfterEach
    void cleanUp() {
        // Clean cart items after each test
        restTemplate.exchange(
                "/api/cart/item/{skuId}?userId={userId}",
                HttpMethod.DELETE, null, String.class, 0L, userId);
        restTemplate.exchange(
                "/api/cart/item/{skuId}?userId={userId}",
                HttpMethod.DELETE, null, String.class, 1L, userId);
        restTemplate.exchange(
                "/api/cart/item/{skuId}?userId={userId}",
                HttpMethod.DELETE, null, String.class, 2L, userId);
    }

    @Test
    @DisplayName("Should complete full cart lifecycle: add -> list -> update -> select -> count -> remove -> verify empty")
    void shouldManageCartLifecycle() throws Exception {
        Long skuId1 = 2001L;
        Long skuId2 = 2002L;

        // ============================================================
        // Step 1: Add first item to cart
        // ============================================================
        Map<String, Object> item1 = new HashMap<>();
        item1.put("skuId", skuId1);
        item1.put("spuId", 1001L);
        item1.put("spuName", "iPhone 15 Pro Max");
        item1.put("skuSpec", "256GB 原色钛金属");
        item1.put("skuImage", "https://example.com/iphone.jpg");
        item1.put("price", 9999.00);
        item1.put("quantity", 1);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity1 = new HttpEntity<>(item1, headers);

        ResponseEntity<String> addResp1 = restTemplate.exchange(
                "/api/cart/add?userId={userId}",
                HttpMethod.POST, entity1, String.class, userId);
        assertThat(addResp1.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode addRoot1 = objectMapper.readTree(addResp1.getBody());
        assertThat(addRoot1.get("code").asInt())/*FIX:code now 0*/ .isEqualTo(0);

        // ============================================================
        // Step 2: Add second item to cart
        // ============================================================
        Map<String, Object> item2 = new HashMap<>();
        item2.put("skuId", skuId2);
        item2.put("spuId", 1001L);
        item2.put("spuName", "iPhone 15 Pro Max");
        item2.put("skuSpec", "256GB 蓝色钛金属");
        item2.put("skuImage", "https://example.com/iphone-blue.jpg");
        item2.put("price", 9999.00);
        item2.put("quantity", 2);

        HttpEntity<Map<String, Object>> entity2 = new HttpEntity<>(item2, headers);
        ResponseEntity<String> addResp2 = restTemplate.exchange(
                "/api/cart/add?userId={userId}",
                HttpMethod.POST, entity2, String.class, userId);
        assertThat(addResp2.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode addRoot2 = objectMapper.readTree(addResp2.getBody());
        assertThat(addRoot2.get("code").asInt())/*FIX:code now 0*/ .isEqualTo(0);

        // ============================================================
        // Step 3: Add same SKU again (should increment quantity)
        // ============================================================
        Map<String, Object> item1Again = new HashMap<>();
        item1Again.put("skuId", skuId1);
        item1Again.put("spuId", 1001L);
        item1Again.put("spuName", "iPhone 15 Pro Max");
        item1Again.put("skuSpec", "256GB 原色钛金属");
        item1Again.put("price", 9999.00);
        item1Again.put("quantity", 3);

        HttpEntity<Map<String, Object>> entity1Again = new HttpEntity<>(item1Again, headers);
        ResponseEntity<String> addResp1Again = restTemplate.exchange(
                "/api/cart/add?userId={userId}",
                HttpMethod.POST, entity1Again, String.class, userId);
        assertThat(addResp1Again.getStatusCode()).isEqualTo(HttpStatus.OK);

        // ============================================================
        // Step 4: List cart items
        // ============================================================
        ResponseEntity<String> listResp = restTemplate.getForEntity(
                "/api/cart/list?userId={userId}", String.class, userId);
        assertThat(listResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode listRoot = objectMapper.readTree(listResp.getBody());
        assertThat(listRoot.get("code").asInt())/*FIX:code now 0*/ .isEqualTo(0);
        assertThat(listRoot.get("data").size()).isEqualTo(2);

        // Verify first item quantity was incremented (1 + 3 = 4)
        for (int i = 0; i < listRoot.get("data").size(); i++) {
            JsonNode item = listRoot.get("data").get(i);
            if (item.get("skuId").asLong() == skuId1) {
                assertThat(item.get("quantity").asInt()).isEqualTo(4);
                assertThat(item.get("selected").asBoolean()).isTrue();
            }
        }

        // ============================================================
        // Step 5: Update quantity of first item
        // ============================================================
        ResponseEntity<String> updateResp = restTemplate.exchange(
                "/api/cart/item/{skuId}?userId={userId}&quantity={quantity}",
                HttpMethod.PUT, null, String.class, skuId1, userId, 2);
        assertThat(updateResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode updateRoot = objectMapper.readTree(updateResp.getBody());
        assertThat(updateRoot.get("code").asInt())/*FIX:code now 0*/ .isEqualTo(0);

        // Verify updated quantity
        ResponseEntity<String> verifyUpdateResp = restTemplate.getForEntity(
                "/api/cart/list?userId={userId}", String.class, userId);
        JsonNode verifyUpdate = objectMapper.readTree(verifyUpdateResp.getBody());
        for (int i = 0; i < verifyUpdate.get("data").size(); i++) {
            JsonNode item = verifyUpdate.get("data").get(i);
            if (item.get("skuId").asLong() == skuId1) {
                assertThat(item.get("quantity").asInt()).isEqualTo(2);
            }
        }

        // ============================================================
        // Step 6: Unselect first item
        // ============================================================
        ResponseEntity<String> selectResp = restTemplate.exchange(
                "/api/cart/item/{skuId}/select?userId={userId}&selected={selected}",
                HttpMethod.PUT, null, String.class, skuId1, userId, false);
        assertThat(selectResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode selectRoot = objectMapper.readTree(selectResp.getBody());
        assertThat(selectRoot.get("code").asInt())/*FIX:code now 0*/ .isEqualTo(0);

        // Verify unselected
        ResponseEntity<String> verifySelectResp = restTemplate.getForEntity(
                "/api/cart/list?userId={userId}", String.class, userId);
        JsonNode verifySelect = objectMapper.readTree(verifySelectResp.getBody());
        for (int i = 0; i < verifySelect.get("data").size(); i++) {
            JsonNode item = verifySelect.get("data").get(i);
            if (item.get("skuId").asLong() == skuId1) {
                assertThat(item.get("selected").asBoolean()).isFalse();
            }
        }

        // ============================================================
        // Step 7: Select all items
        // ============================================================
        ResponseEntity<String> selectAllResp = restTemplate.exchange(
                "/api/cart/select-all?userId={userId}&selected={selected}",
                HttpMethod.PUT, null, String.class, userId, true);
        assertThat(selectAllResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode selectAllRoot = objectMapper.readTree(selectAllResp.getBody());
        assertThat(selectAllRoot.get("code").asInt())/*FIX:code now 0*/ .isEqualTo(0);

        // Verify all selected
        ResponseEntity<String> verifyAllResp = restTemplate.getForEntity(
                "/api/cart/list?userId={userId}", String.class, userId);
        JsonNode verifyAll = objectMapper.readTree(verifyAllResp.getBody());
        for (int i = 0; i < verifyAll.get("data").size(); i++) {
            assertThat(verifyAll.get("data").get(i).get("selected").asBoolean()).isTrue();
        }

        // ============================================================
        // Step 8: Get cart count
        // ============================================================
        ResponseEntity<String> countResp = restTemplate.getForEntity(
                "/api/cart/count?userId={userId}", String.class, userId);
        assertThat(countResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode countRoot = objectMapper.readTree(countResp.getBody());
        assertThat(countRoot.get("code").asInt())/*FIX:code now 0*/ .isEqualTo(0);
        assertThat(countRoot.get("data").asInt()).isEqualTo(2);

        // ============================================================
        // Step 9: Remove first item
        // ============================================================
        ResponseEntity<String> removeResp = restTemplate.exchange(
                "/api/cart/item/{skuId}?userId={userId}",
                HttpMethod.DELETE, null, String.class, skuId1, userId);
        assertThat(removeResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode removeRoot = objectMapper.readTree(removeResp.getBody());
        assertThat(removeRoot.get("code").asInt())/*FIX:code now 0*/ .isEqualTo(0);

        // ============================================================
        // Step 10: Verify cart has 1 item remaining
        // ============================================================
        ResponseEntity<String> afterRemoveResp = restTemplate.getForEntity(
                "/api/cart/list?userId={userId}", String.class, userId);
        JsonNode afterRemove = objectMapper.readTree(afterRemoveResp.getBody());
        assertThat(afterRemove.get("code").asInt())/*FIX:code now 0*/ .isEqualTo(0);
        assertThat(afterRemove.get("data").size()).isEqualTo(1);
        assertThat(afterRemove.get("data").get(0).get("skuId").asLong()).isEqualTo(skuId2);

        // ============================================================
        // Step 11: Remove remaining item and verify empty
        // ============================================================
        restTemplate.exchange("/api/cart/item/{skuId}?userId={userId}",
                HttpMethod.DELETE, null, String.class, skuId2, userId);

        ResponseEntity<String> emptyResp = restTemplate.getForEntity(
                "/api/cart/list?userId={userId}", String.class, userId);
        JsonNode emptyRoot = objectMapper.readTree(emptyResp.getBody());
        assertThat(emptyRoot.get("code").asInt())/*FIX:code now 0*/ .isEqualTo(0);
        assertThat(emptyRoot.get("data").size()).isEqualTo(0);

        // Count should be 0
        ResponseEntity<String> finalCountResp = restTemplate.getForEntity(
                "/api/cart/count?userId={userId}", String.class, userId);
        JsonNode finalCount = objectMapper.readTree(finalCountResp.getBody());
        assertThat(finalCount.get("data").asInt()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should handle empty cart gracefully")
    void shouldHandleEmptyCart() throws Exception {
        // List empty cart
        ResponseEntity<String> listResp = restTemplate.getForEntity(
                "/api/cart/list?userId={userId}", String.class, -999L);
        assertThat(listResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode listRoot = objectMapper.readTree(listResp.getBody());
        assertThat(listRoot.get("code").asInt())/*FIX:code now 0*/ .isEqualTo(0);
        assertThat(listRoot.get("data").size()).isEqualTo(0);

        // Count empty cart
        ResponseEntity<String> countResp = restTemplate.getForEntity(
                "/api/cart/count?userId={userId}", String.class, -999L);
        assertThat(countResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode countRoot = objectMapper.readTree(countResp.getBody());
        assertThat(countRoot.get("code").asInt())/*FIX:code now 0*/ .isEqualTo(0);
        assertThat(countRoot.get("data").asInt()).isEqualTo(0);
    }
}
