package com.supermarket.auth.controller;

import com.supermarket.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Test
    void shouldLoginWhenValidCredentials() throws Exception {
        // Arrange
        Map<String, String> tokenMap = Map.of(
                "accessToken", "access-token-123",
                "refreshToken", "refresh-token-456"
        );
        when(authService.login("13800138000", "123456")).thenReturn(tokenMap);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .param("phone", "13800138000")
                        .param("password", "123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.accessToken").value("access-token-123"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token-456"));
        verify(authService).login("13800138000", "123456");
    }

    @Test
    void shouldReturnErrorWhenLoginWithInvalidPhone() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .param("phone", "12345678901")
                        .param("password", "123456"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
        verify(authService, never()).login(anyString(), anyString());
    }

    @Test
    void shouldReturnErrorWhenLoginWithShortPassword() throws Exception {
        // Act & Assert (password has @Size(min=6) validation)
        mockMvc.perform(post("/api/auth/login")
                        .param("phone", "13800138000")
                        .param("password", "12345"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
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
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("new-refresh-token"));
        verify(authService).refreshToken("valid-refresh-token");
    }

    @Test
    void shouldReturnErrorWhenRefreshWithBlankToken() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/auth/refresh")
                        .param("refreshToken", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
        verify(authService, never()).refreshToken(anyString());
    }
}
