package com.supermarket.user.controller;

import com.supermarket.user.entity.User;
import com.supermarket.user.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserServiceImpl userService;

    @Test
    void shouldRegisterWhenValidInput() throws Exception {
        // Arrange
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setPhone("13800138000");
        when(userService.register("13800138000", "123456")).thenReturn(mockUser);

        // Act & Assert
        mockMvc.perform(post("/api/user/register")
                        .param("phone", "13800138000")
                        .param("password", "123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.phone").value("13800138000"));
        verify(userService).register("13800138000", "123456");
    }

    @Test
    void shouldReturnErrorWhenRegisterWithInvalidPhone() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/user/register")
                        .param("phone", "12345678901")
                        .param("password", "123456"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
        verify(userService, never()).register(anyString(), anyString());
    }

    @Test
    void shouldReturnErrorWhenRegisterWithShortPassword() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/user/register")
                        .param("phone", "13800138000")
                        .param("password", "12345"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
        verify(userService, never()).register(anyString(), anyString());
    }

    @Test
    void shouldLoginWhenValidCredentials() throws Exception {
        // Arrange
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setPhone("13800138000");
        when(userService.login("13800138000", "123456")).thenReturn(mockUser);

        // Act & Assert
        mockMvc.perform(post("/api/user/login")
                        .param("phone", "13800138000")
                        .param("password", "123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.phone").value("13800138000"));
        verify(userService).login("13800138000", "123456");
    }

    @Test
    void shouldReturnErrorWhenLoginWithBlankPhone() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/user/login")
                        .param("phone", "")
                        .param("password", "123456"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
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
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.phone").value("13800138000"));
        verify(userService).getById(1L);
    }
}
