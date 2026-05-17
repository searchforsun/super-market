package com.supermarket.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supermarket.common.core.dto.LoginRequest;
import com.supermarket.common.web.handler.GlobalExceptionHandler;
import com.supermarket.user.entity.User;
import com.supermarket.user.service.impl.UserServiceImpl;
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

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {UserController.class, UserControllerTest.TestConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserServiceImpl userService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Configuration
    @EnableAutoConfiguration
    @ComponentScan(basePackageClasses = UserController.class)
    @Import(GlobalExceptionHandler.class)
    static class TestConfig {
    }

    @Test
    void shouldRegisterWhenValidInput() throws Exception {
        // Arrange
        LoginRequest req = new LoginRequest();
        req.setPhone("13800138000");
        req.setPassword("123456");

        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setPhone("13800138000");
        when(userService.register("13800138000", "123456")).thenReturn(mockUser);

        // Act & Assert
        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.phone").value("13800138000"));
        verify(userService).register("13800138000", "123456");
    }

    @Test
    void shouldReturnErrorWhenRegisterWithInvalidPhone() throws Exception {
        // Arrange
        LoginRequest req = new LoginRequest();
        req.setPhone("12345678901");
        req.setPassword("123456");

        // Act & Assert
        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(90002));
        verify(userService, never()).register(anyString(), anyString());
    }

    @Test
    void shouldReturnErrorWhenRegisterWithShortPassword() throws Exception {
        // Arrange
        LoginRequest req = new LoginRequest();
        req.setPhone("13800138000");
        req.setPassword("12345");

        // Act & Assert
        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(90002));
        verify(userService, never()).register(anyString(), anyString());
    }

    @Test
    void shouldLoginWhenValidCredentials() throws Exception {
        // Arrange
        LoginRequest req = new LoginRequest();
        req.setPhone("13800138000");
        req.setPassword("123456");

        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setPhone("13800138000");
        when(userService.login("13800138000", "123456")).thenReturn(mockUser);

        // Act & Assert
        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.phone").value("13800138000"));
        verify(userService).login("13800138000", "123456");
    }

    @Test
    void shouldReturnErrorWhenLoginWithBlankPhone() throws Exception {
        // Arrange
        LoginRequest req = new LoginRequest();
        req.setPhone("");
        req.setPassword("123456");

        // Act & Assert
        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(90002));
        verify(userService, never()).login(anyString(), anyString());
    }

    @Test
    void shouldGetInfoWhenValidUserId() throws Exception {
        // Arrange
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setPhone("13800138000");
        when(userService.getById(1L)).thenReturn(mockUser);

        // Act & Assert
        mockMvc.perform(get("/api/user/info")
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.phone").value("13800138000"));
        verify(userService).getById(1L);
    }
}
