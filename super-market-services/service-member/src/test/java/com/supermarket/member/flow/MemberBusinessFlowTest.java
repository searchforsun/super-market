package com.supermarket.member.flow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Postman-style business flow test for the MEMBER domain.
 * <p>
 * Chains multiple API calls to simulate a complete member points scenario:
 * get member (auto-create) -> add points -> verify increase -> deduct points -> verify decrease.
 * <p>
 * In a full microservice environment, the test user would first register
 * via the service-user API. Since this test runs in the service-member module
 * where UserController is not available, we use a pre-determined userId.
 * The MemberService.getOrCreate(userId) creates a member record for any userId
 * without validating existence in the users table.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Member Business Flow Test")
class MemberBusinessFlowTest {

    private static final long TEST_USER_ID = 20001L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Flow 4: Complete Member Points Add/Deduct Flow")
    void shouldCompleteMemberPointsFlow() throws Exception {
        // =========================================================
        // Step 1: Get member info — auto-creates member with 0 points
        // =========================================================
        String getResponse = mockMvc.perform(get("/api/member/{userId}", TEST_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userId").value(TEST_USER_ID))
                .andExpect(jsonPath("$.data.points").value(0))
                .andExpect(jsonPath("$.data.totalPoints").value(0))
                .andExpect(jsonPath("$.data.level").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode memberData = objectMapper.readTree(getResponse).get("data");
        assertThat(memberData.get("points").asInt()).isEqualTo(0);

        // =========================================================
        // Step 2: Add 100 points
        // =========================================================
        mockMvc.perform(post("/api/member/points/add")
                        .param("userId", String.valueOf(TEST_USER_ID))
                        .param("points", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"));

        // =========================================================
        // Step 3: Get member info again — verify points increased to 100
        // =========================================================
        String afterAddResponse = mockMvc.perform(get("/api/member/{userId}", TEST_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userId").value(TEST_USER_ID))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode afterAddData = objectMapper.readTree(afterAddResponse).get("data");
        assertThat(afterAddData.get("points").asInt()).isEqualTo(100);
        assertThat(afterAddData.get("totalPoints").asInt()).isEqualTo(100);
        assertThat(afterAddData.get("level").asInt()).isEqualTo(1); // 100+ total → 青铜

        // =========================================================
        // Step 4: Deduct 30 points
        // =========================================================
        mockMvc.perform(post("/api/member/points/deduct")
                        .param("userId", String.valueOf(TEST_USER_ID))
                        .param("points", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"));

        // =========================================================
        // Step 5: Get member info — verify points decreased, total unchanged
        // =========================================================
        String afterDeductResponse = mockMvc.perform(get("/api/member/{userId}", TEST_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userId").value(TEST_USER_ID))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode afterDeductData = objectMapper.readTree(afterDeductResponse).get("data");
        assertThat(afterDeductData.get("points").asInt()).isEqualTo(70);  // 100 - 30
        assertThat(afterDeductData.get("totalPoints").asInt()).isEqualTo(100); // total unchanged
        assertThat(afterDeductData.get("level").asInt()).isEqualTo(1); // level stays 青铜

        // =========================================================
        // Step 6: Add points to trigger level upgrade to 白银
        // =========================================================
        mockMvc.perform(post("/api/member/points/add")
                        .param("userId", String.valueOf(TEST_USER_ID))
                        .param("points", "950"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"));

        String afterLevelUpResponse = mockMvc.perform(get("/api/member/{userId}", TEST_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userId").value(TEST_USER_ID))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode afterLevelUpData = objectMapper.readTree(afterLevelUpResponse).get("data");
        assertThat(afterLevelUpData.get("points").asInt()).isEqualTo(1020); // 70 + 950
        assertThat(afterLevelUpData.get("totalPoints").asInt()).isEqualTo(1050); // 100 + 950
        assertThat(afterLevelUpData.get("level").asInt()).isEqualTo(2); // >=1000 total → 白银

        // =========================================================
        // Step 7: Try to deduct more points than available -> should fail
        // =========================================================
        // Note: The controller returns R.ok() even when deductPoints returns false.
        // The R code is still 200 because the controller ignores the boolean result.
        mockMvc.perform(post("/api/member/points/deduct")
                        .param("userId", String.valueOf(TEST_USER_ID))
                        .param("points", "99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // Verify points were NOT deducted
        String afterFailedDeductResponse = mockMvc.perform(get("/api/member/{userId}", TEST_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode afterFailedDeductData = objectMapper.readTree(afterFailedDeductResponse).get("data");
        assertThat(afterFailedDeductData.get("points").asInt()).isEqualTo(1020); // unchanged
    }
}
