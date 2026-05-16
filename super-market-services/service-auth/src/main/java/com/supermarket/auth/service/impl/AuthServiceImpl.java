package com.supermarket.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.supermarket.auth.entity.Role;
import com.supermarket.auth.entity.UserRole;
import com.supermarket.auth.mapper.RoleMapper;
import com.supermarket.auth.mapper.UserRoleMapper;
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
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;

    @DubboReference(check = false)
    @Autowired(required = false)
    private UserDubboService userDubboService;

    @DubboReference(check = false)
    @Autowired(required = false)
    private ShopDubboService shopDubboService;

    @Autowired
    public AuthServiceImpl(JwtUtil jwtUtil, UserRoleMapper userRoleMapper, RoleMapper roleMapper) {
        this.jwtUtil = jwtUtil;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
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

        // 从 DB 查询角色
        List<String> roles = queryRolesFromDb(user.getId());

        // 兜底：老用户无角色记录时自动赋予 ROLE_USER
        if (roles.isEmpty()) {
            assignRole(user.getId(), "ROLE_USER");
            roles.add("ROLE_USER");
        }

        // 商家首次登录时自动同步 user_roles
        if (!roles.contains("ROLE_MERCHANT")
            && shopDubboService != null
            && shopDubboService.hasMerchant(user.getId())) {
            assignRole(user.getId(), "ROLE_MERCHANT");
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

        // 从 DB 重新查角色，不再硬编码
        List<String> roles = queryRolesFromDb(userId);
        if (roles.isEmpty()) {
            roles.add("ROLE_USER");
        }

        String newAccessToken = jwtUtil.generateAccessToken(userId, roles);
        String newRefreshToken = jwtUtil.generateRefreshToken(userId);
        return Map.of(
            "accessToken", newAccessToken,
            "refreshToken", newRefreshToken
        );
    }

    /**
     * 为用户分配角色
     */
    public void assignRole(Long userId, String roleName) {
        Role role = roleMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getName, roleName));
        if (role == null) {
            return;
        }
        UserRole exist = userRoleMapper.selectOne(new LambdaQueryWrapper<UserRole>()
            .eq(UserRole::getUserId, userId)
            .eq(UserRole::getRoleId, role.getId()));
        if (exist != null) {
            return; // 已有该角色
        }
        UserRole ur = new UserRole();
        ur.setUserId(userId);
        ur.setRoleId(role.getId());
        userRoleMapper.insert(ur);
    }

    private List<String> queryRolesFromDb(Long userId) {
        List<UserRole> userRoles = userRoleMapper.selectList(
            new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));

        if (userRoles.isEmpty()) {
            return new ArrayList<>();
        }

        return userRoles.stream()
            .map(ur -> roleMapper.selectById(ur.getRoleId()))
            .filter(Objects::nonNull)
            .map(Role::getName)
            .collect(Collectors.toList());
    }
}
