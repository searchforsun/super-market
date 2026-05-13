package com.supermarket.auth.service;

import com.supermarket.auth.service.impl.AuthServiceImpl;
import com.supermarket.common.core.exception.BizException;
import com.supermarket.common.dubbo.api.user.UserDubboService;
import com.supermarket.common.dubbo.api.user.dto.UserDTO;
import com.supermarket.common.security.config.JwtProperties;
import com.supermarket.common.security.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private AuthService authService;
    private UserDubboService userDubboService;
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        userDubboService = mock(UserDubboService.class);
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("test-secret-key-for-unit-tests-min-256-bits-long-enough");
        jwtProperties.setAccessTokenExpire(7200);
        jwtProperties.setRefreshTokenExpire(604800);
        jwtUtil = new JwtUtil(jwtProperties);
        authService = new AuthServiceImpl(jwtUtil, userDubboService);
    }

    @Test
    void shouldLoginAndReturnTokens() {
        UserDTO mockUser = new UserDTO();
        mockUser.setId(1L);
        mockUser.setPhone("13800138001");
        mockUser.setStatus(1);
        when(userDubboService.getUserByPhone("13800138001")).thenReturn(mockUser);

        Map<String, String> result = authService.login("13800138001", "any-password");

        assertThat(result).containsKeys("accessToken", "refreshToken", "userId");
        assertThat(jwtUtil.validate(result.get("accessToken"))).isTrue();
    }

    @Test
    void shouldFailLoginWithUnknownPhone() {
        when(userDubboService.getUserByPhone(anyString())).thenReturn(null);
        assertThatThrownBy(() -> authService.login("00000000000", "any"))
            .isInstanceOf(BizException.class);
    }

    @Test
    void shouldRefreshToken() {
        String refreshToken = jwtUtil.generateRefreshToken(1L);
        Map<String, String> result = authService.refreshToken(refreshToken);
        assertThat(jwtUtil.validate(result.get("accessToken"))).isTrue();
    }
}
