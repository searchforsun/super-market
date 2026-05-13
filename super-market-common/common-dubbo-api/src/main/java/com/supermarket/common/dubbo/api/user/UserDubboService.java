package com.supermarket.common.dubbo.api.user;

import com.supermarket.common.dubbo.api.user.dto.UserDTO;

public interface UserDubboService {

    UserDTO getUserById(Long userId);

    UserDTO getUserByPhone(String phone);
}
