package com.supermarket.user.flow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supermarket.common.core.dto.LoginRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Postman-style business flow test for the USER domain.
 * <p>
 * Chains multiple API calls together to simulate complete business scenarios:
 * register -> login -> profile -> duplicate-register error -> wrong-password error.
 * <p>
 * Uses @SpringBootTest + MockMvc for real endpoint invocation against the
 * service-user module, with @Transactional to roll back database state after
 * each test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("User Business Flow Test")
class UserBusinessFlowTest {

    private static final String TEST_PASSWORD = "Pass1234";
    private static final String WRONG_PASSWORD = "WrongPass1";
    private static final String TEST_PHONE = generateUniquePhone();

    /**
     * Generates a phone number that is unlikely to collide with existing data.
     * Uses a timestamp-derived suffix to ensure uniqueness across test runs.
     */
    private static String generateUniquePhone() {
        long stamp = System.currentTimeMillis();
        // Take the last 8 digits of the timestamp (ensuring it fits in 11-digit phone)
        String suffix = String.valueOf(stamp % 100_000_000);
        suffix = "0".repeat(8 - suffix.length()) + suffix;
        return "188" + suffix;
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Flow 1: Complete User Registration -> Login -> Profile with error cases")
    void shouldCompleteUserRegistrationLoginAndProfileFlow() throws Exception {
        // =========================================================
        // Step 1: Register a new user with phone and password
        // =========================================================
        LoginRequest registerReq = new LoginRequest();
        registerReq.setPhone(TEST_PHONE);
        registerReq.setPassword(TEST_PASSWORD);

        String registerResponse = mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.phone").value(TEST_PHONE))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract userId from registration response for subsequent calls
        JsonNode registerData = objectMapper.readTree(registerResponse).get("data");
        Long userId = registerData.get("id").asLong();

        // =========================================================
        // Step 2: Login with the same credentials
        // =========================================================
        LoginRequest loginReq = new LoginRequest();
        loginReq.setPhone(TEST_PHONE);
        loginReq.setPassword(TEST_PASSWORD);

        String loginResponse = mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.phone").value(TEST_PHONE))
                .andExpect(jsonPath("$.data.id").value(userId))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Verify login returned the same userId as registration
        JsonNode loginData = objectMapper.readTree(loginResponse).get("data");
        Long loginUserId = loginData.get("id").asLong();
        org.assertj.core.api.Assertions.assertThat(loginUserId).isEqualTo(userId);

        // =========================================================
        // Step 3: Get user info by userId — verify phone matches
        // =========================================================
        mockMvc.perform(get("/api/user/info")
                        .param("userId", String.valueOf(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.phone").value(TEST_PHONE))
                .andExpect(jsonPath("$.data.id").value(userId));

        // =========================================================
        // Step 4: Try to re-register with the same phone
        //          -> BizException("手机号已注册", code=10002)
        // =========================================================
        LoginRequest duplicateReq = new LoginRequest();
        duplicateReq.setPhone(TEST_PHONE);
        duplicateReq.setPassword(TEST_PASSWORD);

        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(10002))
                .andExpect(jsonPath("$.message").value("手机号已注册"));

        // =========================================================
        // Step 5: Try to login with a wrong password
        //          -> BizException("密码错误", code=10003)
        // =========================================================
        LoginRequest wrongPwdReq = new LoginRequest();
        wrongPwdReq.setPhone(TEST_PHONE);
        wrongPwdReq.setPassword(WRONG_PASSWORD);

        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongPwdReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(10003))
                .andExpect(jsonPath("$.message").value("密码错误"));

        // =========================================================
        // Step 6: Try to get info with a non-existent userId
        //          -> BizException("用户不存在", code=10001)
        // =========================================================
        mockMvc.perform(get("/api/user/info")
                        .param("userId", "99999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(10001))
                .andExpect(jsonPath("$.message").value("用户不存在"));
    }
}
