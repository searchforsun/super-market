package com.supermarket.coupon.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supermarket.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("coupon_templates")
public class CouponTemplate extends BaseEntity {
    private String name;
    private Integer type;
    private BigDecimal discountValue;
    private BigDecimal minAmount;
    private Integer totalStock;
    private Integer remainingStock;
    private Integer perUserLimit;
    private Integer validDays;
    private Integer status;
}
