package com.supermarket.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supermarket.common.core.exception.BizException;
import com.supermarket.common.core.result.ResultCode;
import com.supermarket.common.dubbo.api.user.UserDubboService;
import com.supermarket.common.dubbo.api.user.dto.UserDTO;
import com.supermarket.user.entity.User;
import com.supermarket.user.entity.UserRole;
import com.supermarket.user.mapper.RoleMapper;
import com.supermarket.user.mapper.UserMapper;
import com.supermarket.user.mapper.UserRoleMapper;
import com.supermarket.user.service.UserService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@DubboService(interfaceClass = UserDubboService.class)
@org.springframework.stereotype.Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService, UserDubboService {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Override
    public User register(String phone, String password) {
        if (phone == null || phone.isBlank()) {
            throw new BizException(ResultCode.USER_PHONE_EMPTY);
        }
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            throw new BizException(ResultCode.USER_PHONE_INVALID);
        }
        if (password == null || password.isBlank()) {
            throw new BizException(ResultCode.USER_PASSWORD_EMPTY);
        }
        if (password.length() < 6) {
            throw new BizException(ResultCode.USER_PASSWORD_WEAK);
        }
        User exist = getByPhone(phone);
        if (exist != null) {
            throw new BizException(ResultCode.USER_PHONE_EXISTS);
        }
        User user = new User();
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setNickname("用户" + phone.substring(phone.length() - 4));
        user.setStatus(1);
        save(user);

        // 自动赋予 ROLE_USER 角色
        var roleUser = roleMapper.selectOne(
            new LambdaQueryWrapper<com.supermarket.user.entity.Role>()
                .eq(com.supermarket.user.entity.Role::getName, "ROLE_USER"));
        if (roleUser != null) {
            var ur = new UserRole();
            ur.setUserId(user.getId());
            ur.setRoleId(roleUser.getId());
            userRoleMapper.insert(ur);
        }

        return user;
    }

    @Override
    public User login(String phone, String password) {
        User user = getByPhone(phone);
        if (user == null) {
            throw new BizException(ResultCode.USER_PASSWORD_ERROR);
        }
        if (user.getStatus() != 1) {
            throw new BizException(ResultCode.USER_ACCOUNT_DISABLED);
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BizException(ResultCode.USER_PASSWORD_ERROR);
        }
        user.setLastLoginAt(LocalDateTime.now());
        updateById(user);
        return user;
    }

    @Override
    public User getById(Long userId) {
        User user = getBaseMapper().selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        return user;
    }

    @Override
    public User getByPhone(String phone) {
        return getOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
    }

    @Override
    public void updateLoginTime(Long userId) {
        User user = new User();
        user.setId(userId);
        user.setLastLoginAt(LocalDateTime.now());
        updateById(user);
    }

    // -- UserDubboService impl --

    @Override
    public UserDTO getUserById(Long userId) {
        return toDTO(getById(userId));
    }

    @Override
    public UserDTO getUserByPhone(String phone) {
        User user = getByPhone(phone);
        if (user == null) return null;
        return toDTO(user);
    }

    @Override
    public List<Map<String, Object>> getDailyNewUsers(int days) {
        return getBaseMapper().selectDailyNewUsers(days);
    }

    @Override
    public long countTotalUsers() {
        return getBaseMapper().selectTotalUsers();
    }

    @Override
    public long countTodayNewUsers() {
        return getBaseMapper().selectTodayNewUsers();
    }

    private UserDTO toDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setPhone(user.getPhone());
        dto.setNickname(user.getNickname());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setStatus(user.getStatus());
        dto.setPasswordHash(user.getPasswordHash());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }
}
