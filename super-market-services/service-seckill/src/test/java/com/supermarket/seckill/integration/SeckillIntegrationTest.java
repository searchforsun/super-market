package com.supermarket.seckill.integration;

import com.supermarket.seckill.SeckillApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * REAL HTTP integration tests for Seckill domain.
 * Uses TestRestTemplate (RANDOM_PORT) + real MySQL + real Redis (via Redisson + StringRedisTemplate).
 * Uses real RocketMQTemplate (optional, may be null if not configured).
 * No middleware mocking needed -- all infrastructure beans are real.
 */
@SpringBootTest(classes = SeckillApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
@DisplayName("Seckill HTTP Integration Test")
class SeckillIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("Create session -> create product -> preheat -> list sessions -> list products -> " +
            "execute (success) -> execute (success) -> execute (stock exhausted)")
    void shouldCompleteSeckillFlow() {
        // ===============================================================
        // 1. Create a seckill session
        // ===============================================================
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> sessionReq = new HashMap<>();
        sessionReq.put("name", "618秒杀场次-" + System.currentTimeMillis());
        sessionReq.put("startTime", now.toString());
        sessionReq.put("endTime", now.plusHours(2).toString());

        ResponseEntity<Map> createSessionResp = restTemplate.postForEntity(
                "/api/seckill/admin/session",
                createJsonEntity(sessionReq),
                Map.class);
        assertThat(createSessionResp.getBody().get("code")).isEqualTo(0);
        Long sessionId = extractLong(createSessionResp.getBody(), "data.id");
        assertThat(sessionId).isNotNull();

        // ===============================================================
        // 2. Create a seckill product in that session (stock=2, no limitPerUser)
        // ===============================================================
        long spuId = 10000 + System.currentTimeMillis() % 10000;
        Map<String, Object> productReq = new HashMap<>();
        productReq.put("productId", spuId);
        productReq.put("seckillPrice", 99.99);
        productReq.put("stock", 2);

        ResponseEntity<Map> createProductResp = restTemplate.exchange(
                "/api/seckill/admin/product?sessionId={sessionId}",
                HttpMethod.POST,
                createJsonEntity(productReq),
                Map.class,
                sessionId);
        assertThat(createProductResp.getBody().get("code")).isEqualTo(0);
        Map prodData = (Map) createProductResp.getBody().get("data");
        assertThat(prodData.get("sessionId")).isEqualTo(sessionId);
        assertThat(prodData.get("status")).isEqualTo(0); // initially inactive
        Long productId = ((Number) prodData.get("id")).longValue();
        assertThat(productId).isNotNull();

        // ===============================================================
        // 3. Preheat the product (sets Redis cache + updates status to 1)
        // ===============================================================
        ResponseEntity<Map> preheatResp = restTemplate.exchange(
                "/api/seckill/admin/preheat/{id}",
                HttpMethod.POST, null, Map.class, productId);
        assertThat(preheatResp.getBody().get("code")).isEqualTo(0);

        // ===============================================================
        // 4. List available seckill sessions
        // ===============================================================
        ResponseEntity<Map> sessionsResp = restTemplate.getForEntity(
                "/api/seckill/sessions", Map.class);
        assertThat(sessionsResp.getBody().get("code")).isEqualTo(0);
        List<Map> sessions = (List<Map>) sessionsResp.getBody().get("data");
        assertThat(sessions).isNotEmpty();
        // Our session should be in the list
        boolean found = sessions.stream()
                .anyMatch(s -> Objects.equals(s.get("id"), sessionId));
        assertThat(found).isTrue();

        // ===============================================================
        // 5. List products for the session
        // ===============================================================
        ResponseEntity<Map> productsResp = restTemplate.getForEntity(
                "/api/seckill/products?sessionId={sessionId}",
                Map.class, sessionId);
        assertThat(productsResp.getBody().get("code")).isEqualTo(0);
        List<Map> products = (List<Map>) productsResp.getBody().get("data");
        assertThat(products).isNotEmpty();
        assertThat(products.get(0).get("sessionId")).isEqualTo(sessionId);

        // ===============================================================
        // 6. Execute seckill -- first success (stock 2 -> 1)
        // ===============================================================
        Long userId = 10001L;
        ResponseEntity<Map> execSuccessResp = restTemplate.exchange(
                "/api/seckill/execute?userId={userId}&seckillProductId={productId}&quantity={qty}",
                HttpMethod.POST, null, Map.class, userId, productId, 1);
        assertThat(execSuccessResp.getBody().get("code")).isEqualTo(0);
        Map successData = (Map) execSuccessResp.getBody().get("data");
        assertThat(successData.get("success")).isEqualTo(Boolean.TRUE);
        assertThat(successData.get("message")).isNotNull();
        assertThat(successData.get("requestId")).isNotNull();

        // ===============================================================
        // 7. Execute seckill -- second success (stock 1 -> 0)
        // ===============================================================
        Long userId2 = 10002L;
        ResponseEntity<Map> execSuccess2Resp = restTemplate.exchange(
                "/api/seckill/execute?userId={userId}&seckillProductId={productId}&quantity={qty}",
                HttpMethod.POST, null, Map.class, userId2, productId, 1);
        assertThat(execSuccess2Resp.getBody().get("code")).isEqualTo(0);
        Map success2Data = (Map) execSuccess2Resp.getBody().get("data");
        assertThat(success2Data.get("success")).isEqualTo(Boolean.TRUE);

        // ===============================================================
        // 8. Execute seckill -- stock exhausted (stock 0)
        // ===============================================================
        Long userId3 = 10003L;
        ResponseEntity<Map> execEmptyResp = restTemplate.exchange(
                "/api/seckill/execute?userId={userId}&seckillProductId={productId}&quantity={qty}",
                HttpMethod.POST, null, Map.class, userId3, productId, 1);
        assertThat(execEmptyResp.getBody().get("code")).isEqualTo(40010);
        assertThat(execEmptyResp.getBody().get("message")).isNotNull();
    }

    // ---- helpers ----

    private HttpEntity<?> createJsonEntity(Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private Long extractLong(Map body, String path) {
        String[] parts = path.split("\\.");
        Object node = body;
        for (String part : parts) {
            if (node instanceof Map) {
                node = ((Map<?, ?>) node).get(part);
            } else {
                return null;
            }
        }
        return node instanceof Number ? ((Number) node).longValue() : null;
    }
}
