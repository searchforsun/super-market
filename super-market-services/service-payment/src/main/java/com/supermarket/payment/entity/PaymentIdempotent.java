package com.supermarket.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("payment_idempotent")
public class PaymentIdempotent {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String requestId;
    private String businessType;
    private String businessNo;
    private String responseData;
    private LocalDateTime createdAt;
}
