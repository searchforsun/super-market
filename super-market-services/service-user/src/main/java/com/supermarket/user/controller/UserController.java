package com.supermarket.user.controller;

import com.supermarket.common.core.result.R;
import com.supermarket.user.entity.User;
import com.supermarket.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public R<User> register(@RequestParam String phone, @RequestParam String password) {
        User user = userService.register(phone, password);
        user.setPasswordHash(null);
        return R.ok(user);
    }

    @PostMapping("/login")
    public R<User> login(@RequestParam String phone, @RequestParam String password) {
        User user = userService.login(phone, password);
        user.setPasswordHash(null);
        return R.ok(user);
    }

    @GetMapping("/info")
    public R<User> info(@RequestParam Long userId) {
        User user = userService.getById(userId);
        user.setPasswordHash(null);
        return R.ok(user);
    }
}
