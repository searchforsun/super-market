package com.supermarket.payment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supermarket.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payments")
public class Payment extends BaseEntity {
    private String payNo;
    private String orderNo;
    private Long userId;
    private BigDecimal amount;
    private Integer payMethod;
    private Integer payStatus;
    private String thirdPayNo;
    private LocalDateTime paidAt;
}
