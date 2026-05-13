package com.supermarket.auth.service;

import java.util.Map;

public interface AuthService {

    Map<String, String> login(String phone, String password);

    Map<String, String> refreshToken(String refreshToken);
}
