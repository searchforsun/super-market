package com.supermarket.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("risk_logs")
public class RiskLog {
    private Long id;
    private Long ruleId;
    private Long userId;
    private String targetId;
    private Integer riskType;
    private Integer riskScore;
    private String detail;
    private Integer action;
    private LocalDateTime createdAt;
}
