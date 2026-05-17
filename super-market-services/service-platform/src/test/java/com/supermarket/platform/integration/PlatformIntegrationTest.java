package com.supermarket.platform.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supermarket.platform.PlatformApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * REAL HTTP integration tests for Platform domain.
 * Uses TestRestTemplate (RANDOM_PORT) + real MySQL.
 * No middleware mocking needed — PlatformService only uses the database.
 */
@SpringBootTest(classes = PlatformApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
@DisplayName("Platform HTTP Integration Test")
class PlatformIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Banner CRUD: create -> admin list -> active list -> update -> delete -> verify deleted")
    void shouldCompleteBannerFlow() {
        LocalDate today = LocalDate.now();

        // ===============================================================
        // 1. Create a banner
        // ===============================================================
        String bannerJson = String.format("""
                {
                    "title": "618大促-%d",
                    "imageUrl": "https://img.example.com/banner1.jpg",
                    "linkUrl": "https://www.example.com/618",
                    "sortOrder": 1,
                    "position": "HOME_TOP",
                    "status": 1,
                    "startTime": "%sT00:00:00",
                    "endTime": "%sT23:59:59"
                }
                """, System.currentTimeMillis(), today, today.plusDays(30));

        ResponseEntity<Map> createResp = restTemplate.postForEntity(
                "/api/platform/admin/banner",
                createJsonEntity(bannerJson),
                Map.class);
        assertThat(createResp.getBody().get("code"))/*FIX:code now 0*/ .isEqualTo(0);
        assertThat(((Map) createResp.getBody().get("data")).get("title")).isNotNull();
        Long bannerId = extractLong(createResp.getBody(), "data.id");
        assertThat(bannerId).isNotNull();

        // ===============================================================
        // 2. List banners (admin paginated)
        // ===============================================================
        ResponseEntity<Map> listResp = restTemplate.getForEntity(
                "/api/platform/admin/banners?page={page}&size={size}",
                Map.class, 1, 20);
        assertThat(listResp.getBody().get("code"))/*FIX:code now 0*/ .isEqualTo(0);
        Map listData = (Map) listResp.getBody().get("data");
        assertThat(((List) listData.get("records"))).isNotEmpty();

        // ===============================================================
        // 3. Get active banners (front-facing, status=1 + time range)
        // ===============================================================
        ResponseEntity<Map> activeResp = restTemplate.getForEntity(
                "/api/platform/banners?position={position}",
                Map.class, "HOME_TOP");
        assertThat(activeResp.getBody().get("code"))/*FIX:code now 0*/ .isEqualTo(0);
        List<Map> activeBanners = (List<Map>) activeResp.getBody().get("data");
        assertThat(activeBanners).isNotEmpty();
        assertThat(activeBanners.get(0).get("title")).isInstanceOf(String.class);

        // ===============================================================
        // 4. Update the banner
        // ===============================================================
        String updateJson = String.format("""
                {
                    "title": "618年中大促-%d",
                    "sortOrder": 2
                }
                """, System.currentTimeMillis());

        ResponseEntity<Map> updateResp = restTemplate.exchange(
                "/api/platform/admin/banner/{id}",
                HttpMethod.PUT,
                createJsonEntity(updateJson),
                Map.class,
                bannerId);
        assertThat(updateResp.getBody().get("code"))/*FIX:code now 0*/ .isEqualTo(0);
        Map updatedData = (Map) updateResp.getBody().get("data");
        assertThat((String) updatedData.get("title")).contains("618年中大促");

        // ===============================================================
        // 5. Delete the banner
        // ===============================================================
        ResponseEntity<Map> deleteResp = restTemplate.exchange(
                "/api/platform/admin/banner/{id}",
                HttpMethod.DELETE, null, Map.class, bannerId);
        assertThat(deleteResp.getBody().get("code"))/*FIX:code now 0*/ .isEqualTo(0);

        // ===============================================================
        // 6. Verify deleted — should not appear in active banners
        // ===============================================================
        ResponseEntity<Map> afterDeleteResp = restTemplate.getForEntity(
                "/api/platform/banners?position={position}",
                Map.class, "HOME_TOP");
        assertThat(afterDeleteResp.getBody().get("code"))/*FIX:code now 0*/ .isEqualTo(0);
        // The active list is filtered by time+status, so our deleted banner
        // must be absent. If we can't easily check absence by title,
        // at least verify the endpoint returns successfully.
        List<Map> afterDelete = (List<Map>) afterDeleteResp.getBody().get("data");
        boolean found = afterDelete.stream()
                .anyMatch(b -> ((String) b.get("title")).contains("618大促")
                        || ((String) b.get("title")).contains("618年中大促"));
        assertThat(found).as("Deleted banner should not appear in active list").isFalse();
    }

    @Test
    @DisplayName("Ad position: create -> list")
    void shouldCompleteAdPositionFlow() {
        // ===============================================================
        // 1. Create an ad position
        // ===============================================================
        String positionJson = """
                {
                    "code": "HOME_BANNER",
                    "name": "首页Banner位",
                    "description": "首页顶部轮播广告位",
                    "status": 1
                }
                """;

        ResponseEntity<Map> createResp = restTemplate.postForEntity(
                "/api/platform/admin/position",
                createJsonEntity(positionJson),
                Map.class);
        assertThat(createResp.getBody().get("code"))/*FIX:code now 0*/ .isEqualTo(0);
        assertThat(((Map) createResp.getBody().get("data")).get("name")).isEqualTo("首页Banner位");
        Long positionId = extractLong(createResp.getBody(), "data.id");
        assertThat(positionId).isNotNull();

        // ===============================================================
        // 2. List all positions
        // ===============================================================
        ResponseEntity<Map> listResp = restTemplate.getForEntity(
                "/api/platform/admin/positions", Map.class);
        assertThat(listResp.getBody().get("code"))/*FIX:code now 0*/ .isEqualTo(0);
        List<Map> positions = (List<Map>) listResp.getBody().get("data");
        assertThat(positions).isNotEmpty();
        assertThat(positions.get(0).get("code")).isEqualTo("HOME_BANNER");
    }

    @Test
    @DisplayName("Risk control: create rule -> list rules -> evaluate -> list logs")
    void shouldCompleteRiskControlFlow() {
        // ===============================================================
        // 1. Create a risk rule
        // ===============================================================
        String ruleJson = String.format("""
                {
                    "name": "高频下单检测-%d",
                    "type": 1,
                    "config": "{\\"threshold\\":100,\\"score\\":50}",
                    "status": 1
                }
                """, System.currentTimeMillis());

        ResponseEntity<Map> createRuleResp = restTemplate.postForEntity(
                "/api/platform/admin/risk/rule",
                createJsonEntity(ruleJson),
                Map.class);
        assertThat(createRuleResp.getBody().get("code"))/*FIX:code now 0*/ .isEqualTo(0);
        assertThat(((Map) createRuleResp.getBody().get("data")).get("name")).isNotNull();
        Long ruleId = extractLong(createRuleResp.getBody(), "data.id");
        assertThat(ruleId).isNotNull();

        // ===============================================================
        // 2. List risk rules
        // ===============================================================
        ResponseEntity<Map> listRulesResp = restTemplate.getForEntity(
                "/api/platform/admin/risk/rules", Map.class);
        assertThat(listRulesResp.getBody().get("code"))/*FIX:code now 0*/ .isEqualTo(0);
        List<Map> rules = (List<Map>) listRulesResp.getBody().get("data");
        assertThat(rules).isNotEmpty();

        // ===============================================================
        // 3. Evaluate risk (creates a risk log entry)
        // ===============================================================
        ResponseEntity<Map> evaluateResp = restTemplate.exchange(
                "/api/platform/admin/risk/evaluate?userId={userId}&targetId={targetId}&riskType={riskType}",
                HttpMethod.POST, null, Map.class, 10001L, "ORDER_001", 1);
        assertThat(evaluateResp.getBody().get("code"))/*FIX:code now 0*/ .isEqualTo(0);
        Map evalResult = (Map) evaluateResp.getBody().get("data");
        assertThat(evalResult).containsKey("totalScore");
        assertThat(evalResult).containsKey("action");
        assertThat(evalResult).containsKey("actionDesc");

        // ===============================================================
        // 4. List risk logs (should contain the evaluation)
        // ===============================================================
        ResponseEntity<Map> logsResp = restTemplate.getForEntity(
                "/api/platform/admin/risk/logs?riskType={riskType}&page={page}&size={size}",
                Map.class, 1, 1, 20);
        assertThat(logsResp.getBody().get("code"))/*FIX:code now 0*/ .isEqualTo(0);
        Map logsData = (Map) logsResp.getBody().get("data");
        List<Map> logs = (List<Map>) logsData.get("records");
        assertThat(logs).isNotEmpty();
        assertThat(logs.get(0).get("riskType")).isEqualTo(1);
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
