package com.supermarket.auth.controller;

import com.supermarket.auth.service.AuthService;
import com.supermarket.common.core.result.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public R<Map<String, String>> login(@RequestParam String phone, @RequestParam String password) {
        return R.ok(authService.login(phone, password));
    }

    @PostMapping("/refresh")
    public R<Map<String, String>> refresh(@RequestParam String refreshToken) {
        return R.ok(authService.refreshToken(refreshToken));
    }
}
