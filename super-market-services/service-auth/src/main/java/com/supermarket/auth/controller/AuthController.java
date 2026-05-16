package com.supermarket.auth.controller;

import com.supermarket.auth.service.AuthService;
import com.supermarket.common.core.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
    public R<Map<String, String>> login(@Parameter(description = "手机号") @RequestParam String phone,
                                        @Parameter(description = "密码") @RequestParam String password) {
        return R.ok(authService.login(phone, password));
    }

    @Operation(summary = "刷新令牌")
    @PostMapping("/refresh")
    public R<Map<String, String>> refresh(@Parameter(description = "刷新令牌") @RequestParam String refreshToken) {
        return R.ok(authService.refreshToken(refreshToken));
    }
}
