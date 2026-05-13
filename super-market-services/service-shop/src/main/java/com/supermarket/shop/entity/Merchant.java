package com.supermarket.shop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supermarket.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("merchants")
public class Merchant extends BaseEntity {
    private Long userId;
    private String companyName;
    private String businessLicense;
    private String legalPerson;
    private String idCard;
    private String contactPhone;
    private Integer auditStatus;
    private String auditReason;
    private Integer status;
}
