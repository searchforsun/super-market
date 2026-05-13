package com.supermarket.common.dubbo.api.user.dto;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class UserDTO implements Serializable {
    private Long id;
    private String phone;
    private String nickname;
    private String avatarUrl;
    private Integer status;
    private LocalDateTime createdAt;
}
