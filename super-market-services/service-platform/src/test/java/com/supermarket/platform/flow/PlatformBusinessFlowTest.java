package com.supermarket.platform.flow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supermarket.platform.PlatformApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Postman-style business flow test for the Platform domain.
 *
 * Tests four feature areas:
 *   - Banner lifecycle (create -> list -> update -> delete)
 *   - Ad position management
 *   - Risk rule management and evaluation
 *
 * Requires a running MySQL instance (configured via application-test.yml).
 * No middleware mocking needed -- PlatformService only uses the database.
 */
@SpringBootTest(classes = PlatformApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("flow-test")
@Transactional
@DisplayName("Platform Business Flow")
class PlatformBusinessFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Banner CRUD flow: create -> admin list -> active list -> update -> delete")
    void shouldCompleteBannerFlow() throws Exception {
        // ---------------------------------------------------------------
        // 1. Create a banner
        // ---------------------------------------------------------------
        LocalDate today = LocalDate.now();
        String bannerJson = String.format("""
                {
                    "title": "618大促",
                    "imageUrl": "https://img.example.com/banner1.jpg",
                    "linkUrl": "https://www.example.com/618",
                    "sortOrder": 1,
                    "position": "HOME_TOP",
                    "status": 1,
                    "startTime": "%sT00:00:00",
                    "endTime": "%sT23:59:59"
                }
                """, today, today.plusDays(30));
        String createResp = mockMvc.perform(post("/api/platform/admin/banner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bannerJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.title").value("618大促"))
                .andReturn().getResponse().getContentAsString();

        Long bannerId = extractId(createResp, "data.id");
        assertThat(bannerId).isNotNull();

        // ---------------------------------------------------------------
        // 2. List banners (admin paginated)
        // ---------------------------------------------------------------
        String listResp = mockMvc.perform(get("/api/platform/admin/banners")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records[0].title").value("618大促"))
                .andReturn().getResponse().getContentAsString();

        JsonNode records = objectMapper.readTree(listResp).path("data").path("records");
        assertThat(records).isNotEmpty();

        // ---------------------------------------------------------------
        // 3. Get active banners (front-facing, filtered by status + time range)
        // ---------------------------------------------------------------
        mockMvc.perform(get("/api/platform/banners")
                        .param("position", "HOME_TOP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].title").value("618大促"));

        // ---------------------------------------------------------------
        // 4. Update the banner
        // ---------------------------------------------------------------
        String updateJson = """
                {
                    "title": "618年中大促",
                    "sortOrder": 2
                }
                """;
        mockMvc.perform(put("/api/platform/admin/banner/{id}", bannerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.title").value("618年中大促"));

        // ---------------------------------------------------------------
        // 5. Delete the banner
        // ---------------------------------------------------------------
        mockMvc.perform(delete("/api/platform/admin/banner/{id}", bannerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // Verify deletion: list should be empty
        String afterDelete = mockMvc.perform(get("/api/platform/admin/banners")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();

        long totalAfterDelete = objectMapper.readTree(afterDelete)
                .path("data").path("total").asLong();
        assertThat(totalAfterDelete).isEqualTo(0);
    }

    @Test
    @DisplayName("Ad position flow: create -> list")
    void shouldCompleteAdPositionFlow() throws Exception {
        // ---------------------------------------------------------------
        // 1. Create an ad position
        // ---------------------------------------------------------------
        String positionJson = """
                {
                    "code": "HOME_BANNER",
                    "name": "首页Banner位",
                    "description": "首页顶部轮播广告位",
                    "status": 1
                }
                """;
        String createResp = mockMvc.perform(post("/api/platform/admin/position")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(positionJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value("首页Banner位"))
                .andReturn().getResponse().getContentAsString();

        Long positionId = extractId(createResp, "data.id");
        assertThat(positionId).isNotNull();

        // ---------------------------------------------------------------
        // 2. List all positions
        // ---------------------------------------------------------------
        String listResp = mockMvc.perform(get("/api/platform/admin/positions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].code").value("HOME_BANNER"))
                .andReturn().getResponse().getContentAsString();

        JsonNode positions = objectMapper.readTree(listResp).path("data");
        assertThat(positions).isNotEmpty();
    }

    @Test
    @DisplayName("Risk control flow: create rule -> list rules -> evaluate -> list logs")
    void shouldCompleteRiskControlFlow() throws Exception {
        // ---------------------------------------------------------------
        // 1. Create a risk rule
        // ---------------------------------------------------------------
        String ruleJson = """
                {
                    "name": "高频下单检测",
                    "type": 1,
                    "config": "{\\"threshold\\":100,\\"score\\":50}",
                    "status": 1
                }
                """;
        String createRuleResp = mockMvc.perform(post("/api/platform/admin/risk/rule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ruleJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value("高频下单检测"))
                .andReturn().getResponse().getContentAsString();

        Long ruleId = extractId(createRuleResp, "data.id");
        assertThat(ruleId).isNotNull();

        // ---------------------------------------------------------------
        // 2. List risk rules
        // ---------------------------------------------------------------
        String listRulesResp = mockMvc.perform(get("/api/platform/admin/risk/rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();

        JsonNode rules = objectMapper.readTree(listRulesResp).path("data");
        assertThat(rules).isNotEmpty();

        // ---------------------------------------------------------------
        // 3. Evaluate risk (triggers rule matching + risk log creation)
        // ---------------------------------------------------------------
        String evaluateResp = mockMvc.perform(post("/api/platform/admin/risk/evaluate")
                        .param("userId", "10001")
                        .param("targetId", "ORDER_20260516001")
                        .param("riskType", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();

        JsonNode evalResult = objectMapper.readTree(evaluateResp).path("data");
        assertThat(evalResult.has("totalScore")).isTrue();
        assertThat(evalResult.has("action")).isTrue();
        assertThat(evalResult.has("actionDesc")).isTrue();

        System.out.printf("Risk evaluation result: totalScore=%s, action=%s, desc=%s%n",
                evalResult.path("totalScore").asText(),
                evalResult.path("action").asText(),
                evalResult.path("actionDesc").asText());

        // ---------------------------------------------------------------
        // 4. List risk logs (should contain the evaluation just performed)
        // ---------------------------------------------------------------
        String logsResp = mockMvc.perform(get("/api/platform/admin/risk/logs")
                        .param("riskType", "1")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();

        JsonNode logs = objectMapper.readTree(logsResp).path("data").path("records");
        assertThat(logs).isNotEmpty();
        assertThat(logs.get(0).path("riskType").asInt()).isEqualTo(1);
    }

    private Long extractId(String json, String path) throws Exception {
        String[] parts = path.split("\\.");
        JsonNode node = objectMapper.readTree(json);
        for (String part : parts) {
            node = node.path(part);
        }
        return node.isMissingNode() ? null : node.asLong();
    }
}
