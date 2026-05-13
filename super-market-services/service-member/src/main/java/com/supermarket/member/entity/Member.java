package com.supermarket.member.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supermarket.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("members")
public class Member extends BaseEntity {
    private Long userId;
    private Integer level;
    private Integer points;
    private Integer totalPoints;
    private Integer growthValue;
}
