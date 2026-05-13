package com.supermarket.category.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supermarket.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("categories")
public class Category extends BaseEntity {
    private Long parentId;
    private String name;
    private Integer level;
    private Integer sortOrder;
    private String iconUrl;
    private Integer status;
}
