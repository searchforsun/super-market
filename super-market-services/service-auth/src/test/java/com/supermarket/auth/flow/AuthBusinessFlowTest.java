package com.supermarket.auth.flow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supermarket.auth.controller.AuthController;
import com.supermarket.auth.mapper.RoleMapper;
import com.supermarket.auth.mapper.UserRoleMapper;
import com.supermarket.auth.service.impl.AuthServiceImpl;
import com.supermarket.common.core.dto.LoginRequest;
import com.supermarket.common.dubbo.api.user.UserDubboService;
import com.supermarket.common.dubbo.api.user.dto.UserDTO;
import com.supermarket.common.security.config.JwtProperties;
import com.supermarket.common.security.util.JwtUtil;
import com.supermarket.common.web.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Postman-style business flow test for the AUTH domain.
 * <p>
 * Chains multiple API calls to simulate a complete authentication scenario:
 * login (obtain tokens) -> refresh (obtain new tokens using the refresh token).
 * <p>
 * Uses MockMvcBuilders.standaloneSetup() to avoid Dubbo initialization issues
 * that arise with @SpringBootTest (Dubbo auto-configures itself via spring.factories
 * regardless of auto-configuration exclusions). The controller, service, and JwtUtil
 * are real; external dependencies (UserDubboService, mappers) are mocked.
 */
@DisplayName("Auth Business Flow Test")
class AuthBusinessFlowTest {

    private static final String TEST_PHONE = "13800000001";
    private static final String TEST_PASSWORD = "Pass1234";
    private static final long TEST_USER_ID = 1L;
    private static String PASSWORD_HASH;
    private static JwtUtil jwtUtil;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    static void init() {
        PASSWORD_HASH = new BCryptPasswordEncoder().encode(TEST_PASSWORD);

        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("test-secret-key-for-unit-tests-min-256-bits-long-enough");
        jwtProperties.setAccessTokenExpire(7200);
        jwtProperties.setRefreshTokenExpire(604800);
        jwtUtil = new JwtUtil(jwtProperties);
    }

    @BeforeEach
    void setUp() {
        // -- Mock external dependencies --
        UserDubboService userDubboService = mock(UserDubboService.class);
        UserRoleMapper userRoleMapper = mock(UserRoleMapper.class);
        RoleMapper roleMapper = mock(RoleMapper.class);

        // Mock user lookup: return a valid user with BCrypt-hashed password
        UserDTO mockUser = new UserDTO();
        mockUser.setId(TEST_USER_ID);
        mockUser.setPhone(TEST_PHONE);
        mockUser.setStatus(1);
        mockUser.setPasswordHash(PASSWORD_HASH);
        when(userDubboService.getUserByPhone(TEST_PHONE)).thenReturn(mockUser);

        // Mock role queries: empty user_roles -> triggers auto-assign of ROLE_USER
        when(userRoleMapper.selectList(any())).thenReturn(Collections.emptyList());

        // Build real AuthServiceImpl with mocked dependencies
        AuthServiceImpl authService = new AuthServiceImpl(jwtUtil, userRoleMapper, roleMapper);

        // Inject userDubboService via reflection (it uses @Autowired(required = false))
        try {
            Field field = AuthServiceImpl.class.getDeclaredField("userDubboService");
            field.setAccessible(true);
            field.set(authService, userDubboService);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject userDubboService mock", e);
        }
        // shopDubboService is left as null — the code already handles null checks for it

        // Build MockMvc with real controller, real service, and real exception handler
        AuthController authController = new AuthController(authService);
        mockMvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
                .standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Flow 3: Complete Auth Login -> Token Refresh Flow")
    void shouldCompleteLoginAndTokenRefreshFlow() throws Exception {
        // =========================================================
        // Step 1: Login via /api/auth/login with valid credentials
        // =========================================================
        LoginRequest loginReq = new LoginRequest();
        loginReq.setPhone(TEST_PHONE);
        loginReq.setPassword(TEST_PASSWORD);

        MockHttpServletResponse loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andExpect(jsonPath("$.data.userId").value(String.valueOf(TEST_USER_ID)))
                .andExpect(jsonPath("$.data.roles").isString())
                .andReturn()
                .getResponse();

        // Extract tokens from login response
        JsonNode loginData = objectMapper.readTree(loginResponse.getContentAsString()).get("data");
        String originalRefreshToken = loginData.get("refreshToken").asText();
        String userId = loginData.get("userId").asText();
        String roles = loginData.get("roles").asText();

        // Verify JWT token structure (3 dot-separated segments)
        assertThat(originalRefreshToken).contains(".");
        assertThat(userId).isEqualTo(String.valueOf(TEST_USER_ID));
        assertThat(roles).contains("ROLE_USER");

        // =========================================================
        // Step 2: Refresh tokens using the refresh token
        // =========================================================
        MockHttpServletResponse refreshResponse = mockMvc.perform(post("/api/auth/refresh")
                        .param("refreshToken", originalRefreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andReturn()
                .getResponse();

        // Extract new tokens
        JsonNode refreshData = objectMapper.readTree(refreshResponse.getContentAsString()).get("data");
        String newAccessToken = refreshData.get("accessToken").asText();
        String newRefreshToken = refreshData.get("refreshToken").asText();

        // Verify returned tokens are valid JWTs (3 dot-separated segments)
        assertThat(newAccessToken).contains(".");
        assertThat(newRefreshToken).contains(".");
        // Verify the new tokens can be parsed by our JwtUtil
        assertThat(jwtUtil.validate(newAccessToken)).isTrue();
        assertThat(jwtUtil.validate(newRefreshToken)).isTrue();
        assertThat(jwtUtil.getUserId(newAccessToken)).isEqualTo(TEST_USER_ID);

        // =========================================================
        // Step 3: Verify the new refresh token also works
        // =========================================================
        mockMvc.perform(post("/api/auth/refresh")
                        .param("refreshToken", newRefreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString());
    }
}
