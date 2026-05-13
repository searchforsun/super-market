package com.supermarket.coupon.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supermarket.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("coupon_batches")
public class CouponBatch extends BaseEntity {
    private Long templateId;
    private String batchName;
    private Integer quantity;
    private Integer distributeType;
}
