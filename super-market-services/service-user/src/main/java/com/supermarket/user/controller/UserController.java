package com.supermarket.user.controller;

import com.supermarket.common.core.dto.LoginRequest;
import com.supermarket.common.core.result.R;
import com.supermarket.user.entity.User;
import com.supermarket.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "用户服务", description = "用户注册、登录、信息管理接口")
public class UserController {

    private final UserService userService;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public R<User> register(@RequestBody @Valid LoginRequest req) {
        User user = userService.register(req.getPhone(), req.getPassword());
        user.setPasswordHash(null);
        return R.ok(user);
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public R<User> login(@RequestBody @Valid LoginRequest req) {
        User user = userService.login(req.getPhone(), req.getPassword());
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
