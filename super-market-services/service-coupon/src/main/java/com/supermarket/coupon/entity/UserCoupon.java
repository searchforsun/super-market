package com.supermarket.coupon.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supermarket.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_coupons")
public class UserCoupon extends BaseEntity {
    private Long userId;
    private Long templateId;
    private Long batchId;
    private String couponCode;
    private Integer status;
    private String orderNo;
    private LocalDateTime usedAt;
    private LocalDateTime expireTime;
}
