package com.supermarket.common.dubbo.api.coupon;

public interface CouponDubboService {

    boolean tryUseCoupon(Long userId, Long couponId, String orderNo);

    boolean returnCoupon(Long couponId);
}
