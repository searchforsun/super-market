package com.supermarket.payment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supermarket.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_refunds")
public class PaymentRefund extends BaseEntity {
    private String refundNo;
    private String payNo;
    private String orderNo;
    private BigDecimal refundAmount;
    private String refundReason;
    private Integer refundStatus;
    private String thirdRefundNo;
    private LocalDateTime refundedAt;
}
