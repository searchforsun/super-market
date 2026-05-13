package com.supermarket.auth.service.impl;

import com.supermarket.auth.service.AuthService;
import com.supermarket.common.core.exception.BizException;
import com.supermarket.common.dubbo.api.user.UserDubboService;
import com.supermarket.common.dubbo.api.user.dto.UserDTO;
import com.supermarket.common.security.util.JwtUtil;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AuthServiceImpl implements AuthService {

    private final JwtUtil jwtUtil;

    @DubboReference(check = false)
    @Autowired(required = false)
    private UserDubboService userDubboService;

    @Autowired
    public AuthServiceImpl(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    public AuthServiceImpl(JwtUtil jwtUtil, UserDubboService userDubboService) {
        this.jwtUtil = jwtUtil;
        this.userDubboService = userDubboService;
    }

    @Override
    public Map<String, String> login(String phone, String password) {
        UserDTO user = userDubboService.getUserByPhone(phone);
        if (user == null) {
            throw new BizException(401, "手机号或密码错误");
        }
        if (user.getStatus() != 1) {
            throw new BizException(403, "账号已被禁用或注销");
        }
        String accessToken = jwtUtil.generateAccessToken(user.getId(), List.of("ROLE_USER"));
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());
        return Map.of(
            "accessToken", accessToken,
            "refreshToken", refreshToken,
            "userId", String.valueOf(user.getId())
        );
    }

    @Override
    public Map<String, String> refreshToken(String refreshToken) {
        if (!jwtUtil.validate(refreshToken)) {
            throw new BizException(401, "Refresh Token 无效或已过期");
        }
        Long userId = jwtUtil.getUserId(refreshToken);
        String newAccessToken = jwtUtil.generateAccessToken(userId, List.of("ROLE_USER"));
        String newRefreshToken = jwtUtil.generateRefreshToken(userId);
        return Map.of(
            "accessToken", newAccessToken,
            "refreshToken", newRefreshToken
        );
    }
}
