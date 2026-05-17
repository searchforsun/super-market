package com.supermarket.shop.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supermarket.shop.ShopApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * REAL HTTP integration tests for Shop domain.
 * Uses TestRestTemplate (RANDOM_PORT) + real MySQL.
 * No middleware mocking needed — ShopService only uses the database.
 */
@SpringBootTest(classes = ShopApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
@DisplayName("Shop HTTP Integration Test")
class ShopIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Apply merchant -> audit -> get shop -> get by merchant -> page -> duplicate apply (expect 400)")
    void shouldCompleteShopFlow() {
        long uniqueId = System.currentTimeMillis();
        long userId = 100000 + uniqueId % 100000;

        // ===============================================================
        // 1. Apply merchant registration
        // ===============================================================
        String merchantJson = String.format("""
                {
                    "userId": %d,
                    "companyName": "测试科技股份有限公司-%d",
                    "businessLicense": "91440101MA5XXXXXX",
                    "legalPerson": "张三",
                    "idCard": "110101199001011234",
                    "contactPhone": "13800138000"
                }
                """, userId, uniqueId);

        ResponseEntity<Map> applyResp = restTemplate.postForEntity(
                "/api/shop/merchant/apply",
                createJsonEntity(merchantJson),
                Map.class);
        assertThat(applyResp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(applyResp.getBody().get("code"))/*FIX:code now 0*/ .isEqualTo(0);

        Long merchantId = extractLong(applyResp.getBody(), "data.id");
        assertThat(merchantId).isNotNull();

        // ===============================================================
        // 2. Audit merchant — approved (auditStatus=1)
        // ===============================================================
        ResponseEntity<Map> auditResp = restTemplate.exchange(
                "/api/shop/merchant/{id}/audit?auditStatus={status}",
                HttpMethod.PUT, null, Map.class, merchantId, 1);
        assertThat(auditResp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(auditResp.getBody().get("code"))/*FIX:code now 0*/ .isEqualTo(0);

        // ===============================================================
        // 3. Get shop by merchant ID (verifies shop was auto-created)
        // ===============================================================
        ResponseEntity<Map> shopByMerchantResp = restTemplate.getForEntity(
                "/api/shop/merchant/{merchantId}", Map.class, merchantId);
        assertThat(shopByMerchantResp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(shopByMerchantResp.getBody().get("code"))/*FIX:code now 0*/ .isEqualTo(0);

        Map shopData = (Map) shopByMerchantResp.getBody().get("data");
        assertThat(shopData).isNotNull();
        assertThat(shopData.get("merchantId")).isEqualTo(merchantId);
        Long shopId = ((Number) shopData.get("id")).longValue();
        assertThat(shopId).isPositive();
        String shopName = (String) shopData.get("shopName");
        assertThat(shopName).contains("旗舰店");

        // ===============================================================
        // 4. Get shop by ID directly
        // ===============================================================
        ResponseEntity<Map> getShopResp = restTemplate.getForEntity(
                "/api/shop/{id}", Map.class, shopId);
        assertThat(getShopResp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(getShopResp.getBody().get("code"))/*FIX:code now 0*/ .isEqualTo(0);
        assertThat(((Map) getShopResp.getBody().get("data")).get("shopName")).isEqualTo(shopName);

        // ===============================================================
        // 5. Page merchants — verify our merchant appears
        // ===============================================================
        ResponseEntity<Map> pageResp = restTemplate.getForEntity(
                "/api/shop/merchant/page?page={page}&size={size}",
                Map.class, 1, 10);
        assertThat(pageResp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(pageResp.getBody().get("code"))/*FIX:code now 0*/ .isEqualTo(0);

        Map pageData = (Map) pageResp.getBody().get("data");
        assertThat(pageData).isNotNull();
        // Our merchant should be in the list (most recent first)
        assertThat(((java.util.List) pageData.get("records"))).isNotEmpty();

        // ===============================================================
        // 6. (Negative) Duplicate apply for same userId should fail
        // ===============================================================
        ResponseEntity<Map> dupResp = restTemplate.postForEntity(
                "/api/shop/merchant/apply",
                createJsonEntity(merchantJson),
                Map.class);
        assertThat(dupResp.getBody().get("code")).isEqualTo(50003);
        String message = (String) dupResp.getBody().get("message");
        assertThat(message).contains("已提交入驻申请");
    }

    // ---- helpers ----

    private HttpEntity<String> createJsonEntity(String json) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(json, headers);
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
