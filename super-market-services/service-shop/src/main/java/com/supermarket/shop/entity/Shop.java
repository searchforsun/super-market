package com.supermarket.shop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supermarket.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("shops")
public class Shop extends BaseEntity {
    private Long merchantId;
    private String shopName;
    private String shopLogo;
    private String shopDesc;
    private Integer status;
}
