package com.supermarket.product.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real HTTP integration test for ProductController.
 * Tests the full SPU/SKU lifecycle via TestRestTemplate against real MySQL.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "dubbo.application.name=test",
        "dubbo.registry.address=N/A",
        "dubbo.protocol.name=tri",
        "dubbo.protocol.port=-1"
})
@ActiveProfiles("test")
@Transactional
@DisplayName("Product Integration Test")
class ProductIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should complete full product lifecycle: create -> get -> audit -> shelf -> list -> search -> update -> verify")
    void shouldCompleteProductLifecycle() throws Exception {
        long timestamp = System.currentTimeMillis();
        Long shopId = 1L;
        Long categoryId = 4L; // "苹果手机" category from seed data

        // ============================================================
        // Step 1: Create SPU with SKUs
        // ============================================================
        Map<String, Object> createReq = new HashMap<>();
        createReq.put("shopId", shopId);
        createReq.put("categoryId", categoryId);
        createReq.put("name", "TestPhone_Integration_" + timestamp);
        createReq.put("subtitle", "Test subtitle " + timestamp);
        createReq.put("mainImage", "https://example.com/test.jpg");
        createReq.put("images", Arrays.asList("https://example.com/img1.jpg", "https://example.com/img2.jpg"));
        createReq.put("description", "<p>Test product description</p>");

        // Create SKUs
        Map<String, Object> sku1 = new HashMap<>();
        sku1.put("specName", "Standard Edition " + timestamp);
        sku1.put("specCode", "STD-" + timestamp);
        sku1.put("price", 999.99);
        sku1.put("marketPrice", 1299.99);
        sku1.put("costPrice", 699.99);
        sku1.put("weight", 500);

        Map<String, Object> sku2 = new HashMap<>();
        sku2.put("specName", "Pro Edition " + timestamp);
        sku2.put("specCode", "PRO-" + timestamp);
        sku2.put("price", 1999.99);
        sku2.put("marketPrice", 2499.99);
        sku2.put("costPrice", 1299.99);
        sku2.put("weight", 550);

        createReq.put("skus", Arrays.asList(sku1, sku2));

        ResponseEntity<String> createResp = restTemplate.postForEntity(
                "/api/product/spu", createReq, String.class);
        assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode createRoot = objectMapper.readTree(createResp.getBody());
        assertThat(createRoot.get("code").asInt())/*FIX:code now 0*/ .isEqualTo(0);
        Long spuId = createRoot.get("data").get("id").asLong();
        assertThat(spuId).isPositive();
        assertThat(createRoot.get("data").get("name").asText())
                .isEqualTo("TestPhone_Integration_" + timestamp);
        assertThat(createRoot.get("data").get("auditStatus").asInt()).isEqualTo(0);
        assertThat(createRoot.get("data").get("shelfStatus").asInt()).isEqualTo(0);

        // ============================================================
        // Step 2: Get SPU by ID
        // ============================================================
        ResponseEntity<String> getSpuResp = restTemplate.getForEntity(
                "/api/product/spu/{spuId}", String.class, spuId);
        assertThat(getSpuResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode getSpuRoot = objectMapper.readTree(getSpuResp.getBody());
        assertThat(getSpuRoot.get("code").asInt())/*FIX:code now 0*/ .isEqualTo(0);
        assertThat(getSpuRoot.get("data").get("name").asText())
                .isEqualTo("TestPhone_Integration_" + timestamp);
        assertThat(getSpuRoot.get("data").get("shopId").asLong()).isEqualTo(shopId);
        assertThat(getSpuRoot.get("data").get("categoryId").asLong()).isEqualTo(categoryId);

        // ============================================================
        // Step 3: Get SKU list
        // ============================================================
        ResponseEntity<String> skusResp = restTemplate.getForEntity(
                "/api/product/spu/{spuId}/skus", String.class, spuId);
        assertThat(skusResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode skusRoot = objectMapper.readTree(skusResp.getBody());
        assertThat(skusRoot.get("code").asInt())/*FIX:code now 0*/ .isEqualTo(0);
        assertThat(skusRoot.get("data").size()).isEqualTo(2);
        Long skuId1 = skusRoot.get("data").get(0).get("id").asLong();
        Long skuId2 = skusRoot.get("data").get(1).get("id").asLong();
        assertThat(skuId1).isPositive();
        assertThat(skuId2).isPositive();
        assertThat(skusRoot.get("data").get(0).get("spuId").asLong()).isEqualTo(spuId);

        // ============================================================
        // Step 4: Audit product (approve, auditStatus=1)
        // ============================================================
        ResponseEntity<String> auditResp = restTemplate.exchange(
                "/api/product/spu/{spuId}/audit?auditStatus=1&reason={reason}",
                HttpMethod.PUT, null, String.class, spuId, "Product approved for testing");
        assertThat(auditResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode auditRoot = objectMapper.readTree(auditResp.getBody());
        assertThat(auditRoot.get("code").asInt())/*FIX:code now 0*/ .isEqualTo(0);

        // Verify audit status changed to 1 (approved)
        ResponseEntity<String> verifyAuditResp = restTemplate.getForEntity(
                "/api/product/spu/{spuId}", String.class, spuId);
        JsonNode verifyAudit = objectMapper.readTree(verifyAuditResp.getBody());
        // Auditing with status=1 also sets shelfStatus=1 in the service
        assertThat(verifyAudit.get("data").get("auditStatus").asInt()).isEqualTo(1);
        assertThat(verifyAudit.get("data").get("shelfStatus").asInt()).isEqualTo(1);

        // ============================================================
        // Step 5: Change shelf status (take offline)
        // ============================================================
        ResponseEntity<String> shelfOffResp = restTemplate.exchange(
                "/api/product/spu/{spuId}/shelf?shelfStatus=0",
                HttpMethod.PUT, null, String.class, spuId);
        assertThat(shelfOffResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode shelfOffRoot = objectMapper.readTree(shelfOffResp.getBody());
        assertThat(shelfOffRoot.get("code").asInt())/*FIX:code now 0*/ .isEqualTo(0);

        // Verify shelf status changed to 0
        ResponseEntity<String> verifyShelfResp = restTemplate.getForEntity(
                "/api/product/spu/{spuId}", String.class, spuId);
        JsonNode verifyShelf = objectMapper.readTree(verifyShelfResp.getBody());
        assertThat(verifyShelf.get("data").get("shelfStatus").asInt()).isEqualTo(0);

        // Put back online for subsequent tests
        restTemplate.exchange("/api/product/spu/{spuId}/shelf?shelfStatus=1",
                HttpMethod.PUT, null, String.class, spuId);

        // ============================================================
        // Step 6: List by shop
        // ============================================================
        ResponseEntity<String> listShopResp = restTemplate.getForEntity(
                "/api/product/list/shop/{shopId}?page={page}&size={size}",
                String.class, shopId, 1, 20);
        assertThat(listShopResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode listShopRoot = objectMapper.readTree(listShopResp.getBody());
        assertThat(listShopRoot.get("code").asInt())/*FIX:code now 0*/ .isEqualTo(0);
        assertThat(listShopRoot.get("data").get("records").isArray()).isTrue();
        assertThat(listShopRoot.get("data").get("total").asLong()).isPositive();

        // ============================================================
        // Step 7: List by category (requires auditStatus=1 AND shelfStatus=1)
        // ============================================================
        ResponseEntity<String> listCatResp = restTemplate.getForEntity(
                "/api/product/list/category/{categoryId}?page={page}&size={size}",
                String.class, categoryId, 1, 20);
        assertThat(listCatResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode listCatRoot = objectMapper.readTree(listCatResp.getBody());
        assertThat(listCatRoot.get("code").asInt())/*FIX:code now 0*/ .isEqualTo(0);
        // Should include our newly created product (since it's audited and on shelf)

        // ============================================================
        // Step 8: Search by keyword
        // ============================================================
        String searchKeyword = "TestPhone_Integration_" + timestamp;
        ResponseEntity<String> searchResp = restTemplate.getForEntity(
                "/api/product/search?keyword={keyword}&page={page}&size={size}",
                String.class, searchKeyword, 1, 20);
        assertThat(searchResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode searchRoot = objectMapper.readTree(searchResp.getBody());
        assertThat(searchRoot.get("code").asInt())/*FIX:code now 0*/ .isEqualTo(0);
        assertThat(searchRoot.get("data").get("total").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(searchRoot.get("data").get("records").get(0).get("name").asText())
                .contains("TestPhone_Integration");

        // ============================================================
        // Step 9: Admin list with audit filter
        // ============================================================
        ResponseEntity<String> adminListResp = restTemplate.getForEntity(
                "/api/product/list?auditStatus={auditStatus}&page={page}&size={size}",
                String.class, 1, 1, 20);
        assertThat(adminListResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode adminListRoot = objectMapper.readTree(adminListResp.getBody());
        assertThat(adminListRoot.get("code").asInt())/*FIX:code now 0*/ .isEqualTo(0);
        assertThat(adminListRoot.get("data").get("records").isArray()).isTrue();

        // ============================================================
        // Step 10: Update SPU
        // ============================================================
        Map<String, Object> updateReq = new HashMap<>();
        updateReq.put("name", "TestPhone_Updated_" + timestamp);
        updateReq.put("subtitle", "Updated subtitle " + timestamp);
        updateReq.put("description", "<p>Updated description</p>");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> updateEntity = new HttpEntity<>(updateReq, headers);

        ResponseEntity<String> updateResp = restTemplate.exchange(
                "/api/product/spu/{spuId}", HttpMethod.PUT, updateEntity, String.class, spuId);
        assertThat(updateResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode updateRoot = objectMapper.readTree(updateResp.getBody());
        assertThat(updateRoot.get("code").asInt())/*FIX:code now 0*/ .isEqualTo(0);

        // ============================================================
        // Step 11: Verify update persisted
        // ============================================================
        ResponseEntity<String> verifyUpdateResp = restTemplate.getForEntity(
                "/api/product/spu/{spuId}", String.class, spuId);
        assertThat(verifyUpdateResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode verifyUpdate = objectMapper.readTree(verifyUpdateResp.getBody());
        assertThat(verifyUpdate.get("data").get("name").asText())
                .isEqualTo("TestPhone_Updated_" + timestamp);
        assertThat(verifyUpdate.get("data").get("subtitle").asText())
                .isEqualTo("Updated subtitle " + timestamp);

        // ============================================================
        // Step 12: Update SKU price
        // ============================================================
        Map<String, Object> updateSkuReq = new HashMap<>();
        updateSkuReq.put("price", 899.99);
        updateSkuReq.put("status", 1);

        HttpEntity<Map<String, Object>> updateSkuEntity = new HttpEntity<>(updateSkuReq, headers);

        ResponseEntity<String> updateSkuResp = restTemplate.exchange(
                "/api/product/sku/{skuId}", HttpMethod.PUT, updateSkuEntity, String.class, skuId1);
        assertThat(updateSkuResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode updateSkuRoot = objectMapper.readTree(updateSkuResp.getBody());
        assertThat(updateSkuRoot.get("code").asInt())/*FIX:code now 0*/ .isEqualTo(0);

        // Verify SKU update
        ResponseEntity<String> verifySkuResp = restTemplate.getForEntity(
                "/api/product/spu/{spuId}/skus", String.class, spuId);
        JsonNode verifySku = objectMapper.readTree(verifySkuResp.getBody());
        JsonNode firstSku = verifySku.get("data").get(0);
        assertThat(firstSku.get("price").decimalValue())
                .isEqualByComparingTo(new BigDecimal("899.99"));
    }

    @Test
    @DisplayName("Should return 404 for non-existent SPU")
    void shouldReturn404ForNonExistentSpu() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/product/spu/{spuId}", String.class, -99999L);
        // GlobalExceptionHandler returns HTTP 200 with biz code in body
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode root = objectMapper.readTree(response.getBody());
        assertThat(root.get("code").asInt()).isEqualTo(20001);
        assertThat(root.get("message").asText()).contains("商品不存在");
    }

    @Test
    @DisplayName("Should reject creating product with missing required fields")
    void shouldRejectCreateProductWithMissingFields() throws Exception {
        Map<String, Object> invalidReq = new HashMap<>();
        // Missing shopId, categoryId, name (all @NotNull/@NotBlank)
        invalidReq.put("name", ""); // @NotBlank but empty

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/product/spu", invalidReq, String.class);
        // Validation errors return HTTP 400 with PARAM_ERROR (90002)
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        JsonNode root = objectMapper.readTree(response.getBody());
        assertThat(root.get("code").asInt()).isEqualTo(90002);
    }

    @Test
    @DisplayName("Should return empty search for non-existent keyword")
    void shouldReturnEmptySearchForNonExistentKeyword() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/product/search?keyword={keyword}", String.class,
                "ZZZZ_NONEXISTENT_PRODUCT_" + System.currentTimeMillis());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode root = new ObjectMapper().readTree(response.getBody());
        assertThat(root.get("code").asInt())/*FIX:code now 0*/ .isEqualTo(0);
        assertThat(root.get("data").get("total").asLong()).isEqualTo(0);
        assertThat(root.get("data").get("records").size()).isEqualTo(0);
    }
}
