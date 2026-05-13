package com.supermarket.seckill.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supermarket.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("seckill_products")
public class SeckillProduct extends BaseEntity {
    private Long sessionId;
    private Long spuId;
    private Long skuId;
    private BigDecimal seckillPrice;
    private Integer seckillStock;
    private Integer limitPerUser;
    private Integer status;
}
