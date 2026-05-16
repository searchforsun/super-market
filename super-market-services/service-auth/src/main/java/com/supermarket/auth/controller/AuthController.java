package com.supermarket.auth.controller;

import com.supermarket.auth.service.AuthService;
import com.supermarket.common.core.dto.LoginRequest;
import com.supermarket.common.core.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证服务", description = "登录认证、Token刷新接口")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "用户登录认证")
    @PostMapping("/login")
    public R<Map<String, String>> login(@RequestBody @Valid LoginRequest req) {
        return R.ok(authService.login(req.getPhone(), req.getPassword()));
    }

    @Operation(summary = "刷新令牌")
    @PostMapping("/refresh")
    public R<Map<String, String>> refresh(@RequestParam @NotBlank(message = "refreshToken不能为空") String refreshToken) {
        return R.ok(authService.refreshToken(refreshToken));
    }
}
