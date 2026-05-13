package com.supermarket.notify.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supermarket.notify.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    @Update("UPDATE notifications SET status = 1, read_at = NOW() WHERE user_id = #{userId} AND status = 0")
    int readAll(@Param("userId") Long userId);
}
