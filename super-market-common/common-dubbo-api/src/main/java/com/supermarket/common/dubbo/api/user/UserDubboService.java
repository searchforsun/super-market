package com.supermarket.common.dubbo.api.user;

import com.supermarket.common.dubbo.api.user.dto.UserDTO;

import java.util.List;
import java.util.Map;

public interface UserDubboService {

    UserDTO getUserById(Long userId);

    UserDTO getUserByPhone(String phone);

    /** 近N日每日新增用户数 */
    List<Map<String, Object>> getDailyNewUsers(int days);

    /** 总用户数 */
    long countTotalUsers();

    /** 今日新增用户数 */
    long countTodayNewUsers();
}
