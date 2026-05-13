package com.supermarket.user.service;

import com.supermarket.user.entity.User;

public interface UserService {

    User register(String phone, String password);

    User login(String phone, String password);

    User getById(Long userId);

    User getByPhone(String phone);

    void updateLoginTime(Long userId);
}
