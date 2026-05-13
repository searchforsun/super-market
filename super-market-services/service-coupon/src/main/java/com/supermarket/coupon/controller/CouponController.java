package com.supermarket.coupon.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supermarket.common.core.result.R;
import com.supermarket.coupon.entity.CouponBatch;
import com.supermarket.coupon.entity.CouponTemplate;
import com.supermarket.coupon.entity.UserCoupon;
import com.supermarket.coupon.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coupon")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping("/admin/template")
    public R<CouponTemplate> createTemplate(@RequestBody CouponTemplate template) {
        return R.ok(couponService.createTemplate(template));
    }

    @PostMapping("/admin/distribute")
    public R<CouponBatch> distribute(@RequestParam Long templateId,
                                     @RequestBody List<Long> userIds) {
        return R.ok(couponService.distribute(templateId, userIds));
    }

    @GetMapping("/available")
    public R<Page<CouponTemplate>> availableTemplates(@RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "20") int size) {
        return R.ok(couponService.listTemplates(page, size));
    }

    @PostMapping("/claim")
    public R<UserCoupon> claim(@RequestParam Long userId, @RequestParam Long templateId) {
        return R.ok(couponService.claim(userId, templateId));
    }

    @GetMapping("/my")
    public R<Page<UserCoupon>> myCoupons(@RequestParam Long userId,
                                          @RequestParam(required = false) Integer status,
                                          @RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "10") int size) {
        return R.ok(couponService.listUserCoupons(userId, status, page, size));
    }

    @GetMapping("/available/list")
    public R<List<UserCoupon>> availableList(@RequestParam Long userId) {
        return R.ok(couponService.listAvailableCoupons(userId));
    }
}
