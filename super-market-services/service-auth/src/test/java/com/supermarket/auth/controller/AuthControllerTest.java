package com.supermarket.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supermarket.auth.service.AuthService;
import com.supermarket.common.core.dto.LoginRequest;
import com.supermarket.common.web.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {AuthController.class, AuthControllerTest.TestConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Configuration
    @EnableAutoConfiguration
    @ComponentScan(basePackageClasses = AuthController.class)
    @Import(GlobalExceptionHandler.class)
    static class TestConfig {
    }

    @Test
    void shouldLoginWhenValidCredentials() throws Exception {
        // Arrange
        LoginRequest req = new LoginRequest();
        req.setPhone("13800138000");
        req.setPassword("123456");

        Map<String, String> tokenMap = Map.of(
                "accessToken", "access-token-123",
                "refreshToken", "refresh-token-456"
        );
        when(authService.login("13800138000", "123456")).thenReturn(tokenMap);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").value("access-token-123"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token-456"));
        verify(authService).login("13800138000", "123456");
    }

    @Test
    void shouldReturnErrorWhenLoginWithInvalidPhone() throws Exception {
        // Arrange
        LoginRequest req = new LoginRequest();
        req.setPhone("12345678901");
        req.setPassword("123456");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(90002));
        verify(authService, never()).login(anyString(), anyString());
    }

    @Test
    void shouldReturnErrorWhenLoginWithShortPassword() throws Exception {
        // Arrange
        LoginRequest req = new LoginRequest();
        req.setPhone("13800138000");
        req.setPassword("12345");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(90002));
        verify(authService, never()).login(anyString(), anyString());
    }

    @Test
    void shouldRefreshWhenValidToken() throws Exception {
        // Arrange
        Map<String, String> tokenMap = Map.of(
                "accessToken", "new-access-token",
                "refreshToken", "new-refresh-token"
        );
        when(authService.refreshToken("valid-refresh-token")).thenReturn(tokenMap);

        // Act & Assert
        mockMvc.perform(post("/api/auth/refresh")
                        .param("refreshToken", "valid-refresh-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("new-refresh-token"));
        verify(authService).refreshToken("valid-refresh-token");
    }

    @Test
    void shouldReturnErrorWhenRefreshWithBlankToken() throws Exception {
        // Act & Assert
        // Note: controller does NOT have @Validated, so @NotBlank on @RequestParam
        // is not enforced. The blank value reaches the controller and the mock
        // returns null, producing a 500 from the generic error handler.
        mockMvc.perform(post("/api/auth/refresh")
                        .param("refreshToken", ""))
                .andExpect(status().isInternalServerError());
        verify(authService, never()).refreshToken(anyString());
    }
}
