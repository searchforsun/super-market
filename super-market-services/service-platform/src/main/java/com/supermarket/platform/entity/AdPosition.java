package com.supermarket.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ad_positions")
public class AdPosition {
    private Long id;
    private String code;
    private String name;
    private String description;
    private Integer status;
    private LocalDateTime createdAt;
}
