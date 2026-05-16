package com.supermarket.auth.service.impl;

import com.supermarket.auth.service.AuthService;
import com.supermarket.common.core.exception.BizException;
import com.supermarket.common.dubbo.api.shop.ShopDubboService;
import com.supermarket.common.dubbo.api.user.UserDubboService;
import com.supermarket.common.dubbo.api.user.dto.UserDTO;
import com.supermarket.common.security.util.JwtUtil;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AuthServiceImpl implements AuthService {

    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private static final Set<String> ADMIN_PHONES = Set.of("13800000000", "13900000000");

    @DubboReference(check = false)
    @Autowired(required = false)
    private UserDubboService userDubboService;

    @DubboReference(check = false)
    @Autowired(required = false)
    private ShopDubboService shopDubboService;

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
        if (user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BizException(401, "手机号或密码错误");
        }

        List<String> roles = new ArrayList<>();
        roles.add("ROLE_USER");
        if (ADMIN_PHONES.contains(phone)) {
            roles.add("ROLE_ADMIN");
        }
        if (shopDubboService != null && shopDubboService.hasMerchant(user.getId())) {
            roles.add("ROLE_MERCHANT");
        }

        String accessToken = jwtUtil.generateAccessToken(user.getId(), roles);
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());
        return Map.of(
            "accessToken", accessToken,
            "refreshToken", refreshToken,
            "userId", String.valueOf(user.getId()),
            "roles", String.join(",", roles)
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
