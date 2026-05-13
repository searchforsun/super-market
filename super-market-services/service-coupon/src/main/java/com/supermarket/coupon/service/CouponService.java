package com.supermarket.coupon.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supermarket.coupon.entity.CouponBatch;
import com.supermarket.coupon.entity.CouponTemplate;
import com.supermarket.coupon.entity.UserCoupon;

import java.util.List;

public interface CouponService {

    CouponTemplate createTemplate(CouponTemplate template);

    CouponTemplate getTemplateById(Long id);

    Page<CouponTemplate> listTemplates(int page, int size);

    CouponBatch distribute(Long templateId, List<Long> userIds);

    UserCoupon claim(Long userId, Long templateId);

    Page<UserCoupon> listUserCoupons(Long userId, Integer status, int page, int size);

    List<UserCoupon> listAvailableCoupons(Long userId);

    UserCoupon useCoupon(Long userId, Long couponId, String orderNo);

    boolean returnCoupon(Long couponId);
}
