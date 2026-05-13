package com.supermarket.review.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supermarket.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("reviews")
public class Review extends BaseEntity {
    private Long userId;
    private Long spuId;
    private Long skuId;
    private String orderNo;
    private Integer rating;
    private String content;
    private String images;
    private Integer isAnonymous;
    private String replyContent;
    private LocalDateTime replyAt;
    private Integer status;
}
