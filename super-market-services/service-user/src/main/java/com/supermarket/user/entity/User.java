package com.supermarket.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supermarket.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("users")
public class User extends BaseEntity {

    private String phone;
    private String email;
    private String passwordHash;
    private String nickname;
    private String avatarUrl;
    private String realName;
    private String idCard;
    private Integer status;
    private LocalDateTime lastLoginAt;
}
