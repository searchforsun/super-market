package com.supermarket.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("risk_rules")
public class RiskRule {
    private Long id;
    private String name;
    private Integer type;
    private String config;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
