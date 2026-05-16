package com.supermarket.user.controller;

import com.supermarket.common.core.result.R;
import com.supermarket.user.entity.User;
import com.supermarket.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "用户服务", description = "用户注册、登录、信息管理接口")
public class UserController {

    private final UserService userService;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public R<User> register(
            @Parameter(description = "手机号") @RequestParam
            @NotBlank(message = "手机号不能为空")
            @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确") String phone,
            @Parameter(description = "密码") @RequestParam
            @NotBlank(message = "密码不能为空")
            @Size(min = 6, max = 32, message = "密码长度为6-32位") String password) {
        User user = userService.register(phone, password);
        user.setPasswordHash(null);
        return R.ok(user);
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public R<User> login(
            @Parameter(description = "手机号") @RequestParam
            @NotBlank(message = "手机号不能为空")
            @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确") String phone,
            @Parameter(description = "密码") @RequestParam
            @NotBlank(message = "密码不能为空") String password) {
        User user = userService.login(phone, password);
        user.setPasswordHash(null);
        return R.ok(user);
    }

    @Operation(summary = "获取用户信息")
    @GetMapping("/info")
    public R<User> info(@Parameter(description = "用户ID") @RequestParam Long userId) {
        User user = userService.getById(userId);
        user.setPasswordHash(null);
        return R.ok(user);
    }
}
