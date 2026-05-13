package com.supermarket.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supermarket.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("spu")
public class Spu extends BaseEntity {
    private String spuNo;
    private Long shopId;
    private Long categoryId;
    private Long brandId;
    private String name;
    private String subtitle;
    private String mainImage;
    private String images;
    private String description;
    private Integer auditStatus;
    private Integer shelfStatus;
    private Integer isDeleted;
}
