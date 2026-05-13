package com.supermarket.user.service;

import com.supermarket.user.entity.User;
import com.supermarket.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Test
    void shouldRegisterNewUser() {
        User user = userService.register("13800138001", "Pass1234");
        assertThat(user.getId()).isNotNull();
        assertThat(user.getPhone()).isEqualTo("13800138001");
        assertThat(user.getPasswordHash()).isNotEqualTo("Pass1234");
    }

    @Test
    void shouldFailRegisterDuplicatePhone() {
        userService.register("13800138002", "Pass1234");
        assertThatThrownBy(() -> userService.register("13800138002", "Pass1234"))
            .hasMessageContaining("已注册");
    }

    @Test
    void shouldLoginWithCorrectCredentials() {
        userService.register("13800138003", "Pass1234");
        User user = userService.login("13800138003", "Pass1234");
        assertThat(user).isNotNull();
        assertThat(user.getLastLoginAt()).isNotNull();
    }

    @Test
    void shouldFailLoginWithWrongPassword() {
        userService.register("13800138004", "Pass1234");
        assertThatThrownBy(() -> userService.login("13800138004", "WrongPass"))
            .hasMessageContaining("密码错误");
    }

    @Test
    void shouldGetUserById() {
        User registered = userService.register("13800138005", "Pass1234");
        User found = userService.getById(registered.getId());
        assertThat(found.getPhone()).isEqualTo("13800138005");
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        assertThatThrownBy(() -> userService.getById(99999L))
            .hasMessageContaining("用户不存在");
    }
}
