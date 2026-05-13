package com.supermarket.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supermarket.coupon.entity.CouponTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CouponTemplateMapper extends BaseMapper<CouponTemplate> {

    @Update("UPDATE coupon_templates SET remaining_stock = remaining_stock - 1 WHERE id = #{id} AND remaining_stock > 0")
    int decrStock(@Param("id") Long templateId);
}
