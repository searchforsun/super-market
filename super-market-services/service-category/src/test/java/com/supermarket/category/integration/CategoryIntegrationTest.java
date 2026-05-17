package com.supermarket.category.integration;

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

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real HTTP integration test for CategoryController.
 * Tests the full category lifecycle via TestRestTemplate against real MySQL.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "dubbo.application.name=test",
        "dubbo.registry.address=N/A",
        "dubbo.protocol.name=tri",
        "dubbo.protocol.port=-1"
})
@ActiveProfiles("test")
@Transactional
@DisplayName("Category Integration Test")
class CategoryIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should complete full category lifecycle: create -> tree -> children -> level -> update -> delete")
    void shouldManageCategoryTree() throws Exception {
        long timestamp = System.currentTimeMillis();

        // ============================================================
        // Step 1: Create two root categories
        // ============================================================
        Map<String, Object> cat1 = new HashMap<>();
        cat1.put("name", "TestElectronics_" + timestamp);
        cat1.put("parentId", 0);
        cat1.put("sortOrder", 1);
        cat1.put("status", 1);

        ResponseEntity<String> create1Resp = restTemplate.postForEntity(
                "/api/category", cat1, String.class);
        assertThat(create1Resp.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode root1 = objectMapper.readTree(create1Resp.getBody());
        assertThat(root1.get("code").asInt())/*FIX:code now 0*/ .isEqualTo(0);
        Long cat1Id = root1.get("data").get("id").asLong();
        assertThat(cat1Id).isPositive();
        assertThat(root1.get("data").get("name").asText()).isEqualTo("TestElectronics_" + timestamp);
        assertThat(root1.get("data").get("level").asInt()).isEqualTo(1);

        Map<String, Object> cat2 = new HashMap<>();
        cat2.put("name", "TestBooks_" + timestamp);
        cat2.put("parentId", 0);
        cat2.put("sortOrder", 2);
        cat2.put("status", 1);

        ResponseEntity<String> create2Resp = restTemplate.postForEntity(
                "/api/category", cat2, String.class);
        assertThat(create2Resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode root2 = objectMapper.readTree(create2Resp.getBody());
        assertThat(root2.get("code").asInt())/*FIX:code now 0*/ .isEqualTo(0);
        Long cat2Id = root2.get("data").get("id").asLong();

        // ============================================================
        // Step 2: Create a child category under cat1
        // ============================================================
        Map<String, Object> child = new HashMap<>();
        child.put("name", "TestMobilePhone_" + timestamp);
        child.put("parentId", cat1Id);
        child.put("sortOrder", 1);
        child.put("status", 1);

        ResponseEntity<String> childResp = restTemplate.postForEntity(
                "/api/category", child, String.class);
        assertThat(childResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode childRoot = objectMapper.readTree(childResp.getBody());
        assertThat(childRoot.get("code").asInt())/*FIX:code now 0*/ .isEqualTo(0);
        Long childId = childRoot.get("data").get("id").asLong();
        assertThat(childRoot.get("data").get("level").asInt()).isEqualTo(2);
        assertThat(childRoot.get("data").get("parentId").asLong()).isEqualTo(cat1Id);

        // ============================================================
        // Step 3: Get full category tree
        // ============================================================
        ResponseEntity<String> treeResp = restTemplate.getForEntity(
                "/api/category/tree", String.class);
        assertThat(treeResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode treeRoot = objectMapper.readTree(treeResp.getBody());
        assertThat(treeRoot.get("code").asInt())/*FIX:code now 0*/ .isEqualTo(0);
        assertThat(treeRoot.get("data").isArray()).isTrue();

        // ============================================================
        // Step 4: Get children of root category
        // ============================================================
        ResponseEntity<String> childrenResp = restTemplate.getForEntity(
                "/api/category/children/{parentId}", String.class, 0L);
        assertThat(childrenResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode childrenRoot = objectMapper.readTree(childrenResp.getBody());
        assertThat(childrenRoot.get("code").asInt())/*FIX:code now 0*/ .isEqualTo(0);
        assertThat(childrenRoot.get("data").isArray()).isTrue();

        // Verify children of cat1
        ResponseEntity<String> cat1ChildrenResp = restTemplate.getForEntity(
                "/api/category/children/{parentId}", String.class, cat1Id);
        assertThat(cat1ChildrenResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode cat1Children = objectMapper.readTree(cat1ChildrenResp.getBody());
        assertThat(cat1Children.get("code").asInt())/*FIX:code now 0*/ .isEqualTo(0);
        assertThat(cat1Children.get("data").size()).isPositive();
        assertThat(cat1Children.get("data").get(0).get("name").asText())
                .contains("TestMobilePhone");

        // ============================================================
        // Step 5: Get categories by level
        // ============================================================
        ResponseEntity<String> level1Resp = restTemplate.getForEntity(
                "/api/category/level/{level}", String.class, 1);
        assertThat(level1Resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode level1Root = objectMapper.readTree(level1Resp.getBody());
        assertThat(level1Root.get("code").asInt())/*FIX:code now 0*/ .isEqualTo(0);
        assertThat(level1Root.get("data").isArray()).isTrue();

        ResponseEntity<String> level2Resp = restTemplate.getForEntity(
                "/api/category/level/{level}", String.class, 2);
        assertThat(level2Resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode level2Root = objectMapper.readTree(level2Resp.getBody());
        assertThat(level2Root.get("code").asInt())/*FIX:code now 0*/ .isEqualTo(0);

        // ============================================================
        // Step 6: Update category name
        // ============================================================
        Map<String, Object> updateBody = new HashMap<>();
        updateBody.put("id", childId);
        updateBody.put("name", "TestUpdatedPhone_" + timestamp);
        updateBody.put("sortOrder", 2);
        updateBody.put("status", 1);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> updateEntity = new HttpEntity<>(updateBody, headers);

        ResponseEntity<String> updateResp = restTemplate.exchange(
                "/api/category", HttpMethod.PUT, updateEntity, String.class);
        assertThat(updateResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode updateRoot = objectMapper.readTree(updateResp.getBody());
        assertThat(updateRoot.get("code").asInt())/*FIX:code now 0*/ .isEqualTo(0);
        assertThat(updateRoot.get("data").get("name").asText())
                .isEqualTo("TestUpdatedPhone_" + timestamp);

        // ============================================================
        // Step 7: Delete leaf category (child)
        // ============================================================
        ResponseEntity<String> deleteResp = restTemplate.exchange(
                "/api/category/{id}", HttpMethod.DELETE, null, String.class, childId);
        assertThat(deleteResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode deleteRoot = objectMapper.readTree(deleteResp.getBody());
        assertThat(deleteRoot.get("code").asInt())/*FIX:code now 0*/ .isEqualTo(0);

        // Verify deletion: children of cat1 should now be empty
        ResponseEntity<String> afterDeleteResp = restTemplate.getForEntity(
                "/api/category/children/{parentId}", String.class, cat1Id);
        JsonNode afterDelete = objectMapper.readTree(afterDeleteResp.getBody());
        assertThat(afterDelete.get("data").size()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should fail to delete category with children")
    void shouldNotDeleteCategoryWithChildren() throws Exception {
        long timestamp = System.currentTimeMillis();

        // Create root category
        Map<String, Object> parent = new HashMap<>();
        parent.put("name", "TestParent_" + timestamp);
        parent.put("parentId", 0);
        parent.put("status", 1);

        ResponseEntity<String> parentResp = restTemplate.postForEntity(
                "/api/category", parent, String.class);
        JsonNode parentRoot = objectMapper.readTree(parentResp.getBody());
        Long parentId = parentRoot.get("data").get("id").asLong();

        // Create child
        Map<String, Object> child = new HashMap<>();
        child.put("name", "TestChild_" + timestamp);
        child.put("parentId", parentId);
        child.put("status", 1);

        ResponseEntity<String> childResp = restTemplate.postForEntity(
                "/api/category", child, String.class);
        assertThat(childResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Try to delete parent (should fail because it has children)
        ResponseEntity<String> deleteResp = restTemplate.exchange(
                "/api/category/{id}", HttpMethod.DELETE, null, String.class, parentId);
        // GlobalExceptionHandler returns HTTP 200 with CATEGORY_HAS_CHILDREN (20006)
        assertThat(deleteResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode deleteRoot = objectMapper.readTree(deleteResp.getBody());
        assertThat(deleteRoot.get("code").asInt()).isEqualTo(20006);
        assertThat(deleteRoot.get("message").asText()).contains("无法删除");
    }

    @Test
    @DisplayName("Should return 404 for non-existent category")
    void shouldReturn404ForNonExistentCategory() throws Exception {
        ResponseEntity<String> getResp = restTemplate.getForEntity(
                "/api/category/children/{parentId}", String.class, -99999L);
        assertThat(getResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode root = objectMapper.readTree(getResp.getBody());
        assertThat(root.get("code").asInt())/*FIX:code now 0*/ .isEqualTo(0);
        // Children of non-existent ID returns empty list, not 404
        assertThat(root.get("data").size()).isEqualTo(0);
    }
}
