package com.supermarket.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supermarket.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT DATE(created_at) as date, COUNT(*) as count " +
            "FROM users WHERE created_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY) " +
            "GROUP BY DATE(created_at) ORDER BY date")
    List<Map<String, Object>> selectDailyNewUsers(@Param("days") int days);

    @Select("SELECT COUNT(*) FROM users")
    long selectTotalUsers();

    @Select("SELECT COUNT(*) FROM users WHERE DATE(created_at) = CURDATE()")
    long selectTodayNewUsers();
}
