package com.supermarket.coupon.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supermarket.common.core.result.R;
import com.supermarket.coupon.entity.CouponBatch;
import com.supermarket.coupon.entity.CouponTemplate;
import com.supermarket.coupon.entity.UserCoupon;
import com.supermarket.coupon.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/coupon")
@RequiredArgsConstructor
@Tag(name = "优惠券服务", description = "优惠券模板、发放、领取、查询接口")
public class CouponController {

    private final CouponService couponService;

    @PostMapping("/admin/template")
    @Operation(summary = "创建优惠券模板")
    public R<CouponTemplate> createTemplate(@Parameter(description = "优惠券模板信息") @RequestBody CouponTemplate template) {
        return R.ok(couponService.createTemplate(template));
    }

    @GetMapping("/admin/templates")
    @Operation(summary = "分页查询优惠券模板列表")
    public R<Page<CouponTemplate>> adminTemplates(@Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
                                                   @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size) {
        return R.ok(couponService.listTemplates(page, size));
    }

    @PutMapping("/admin/template")
    @Operation(summary = "更新优惠券模板")
    public R<CouponTemplate> updateTemplate(@Parameter(description = "优惠券模板信息") @RequestBody CouponTemplate template) {
        return R.ok(couponService.updateTemplate(template));
    }

    @PutMapping("/admin/template/status")
    @Operation(summary = "设置优惠券模板状态")
    public R<Boolean> setTemplateStatus(@Parameter(description = "模板ID") @RequestParam Long id,
                                        @Parameter(description = "状态") @RequestParam Integer status) {
        return R.ok(couponService.setTemplateStatus(id, status));
    }

    @DeleteMapping("/admin/template/{id}")
    @Operation(summary = "删除优惠券模板")
    public R<Void> deleteTemplate(@Parameter(description = "模板ID") @PathVariable Long id) {
        couponService.deleteTemplate(id);
        return R.ok();
    }

    @GetMapping("/admin/batches")
    @Operation(summary = "分页查询发放记录")
    public R<Page<CouponBatch>> listBatches(@Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
                                             @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size) {
        return R.ok(couponService.listBatches(page, size));
    }

    @PostMapping("/admin/distribute")
    @Operation(summary = "发放优惠券")
    public R<CouponBatch> distribute(@Parameter(description = "模板ID") @RequestParam Long templateId,
                                     @Parameter(description = "用户ID列表") @RequestBody List<Long> userIds) {
        return R.ok(couponService.distribute(templateId, userIds));
    }

    @GetMapping("/available")
    @Operation(summary = "查询可领取的优惠券模板")
    public R<Page<CouponTemplate>> availableTemplates(@Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
                                                       @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size) {
        return R.ok(couponService.listTemplates(page, size));
    }

    @PostMapping("/claim")
    @Operation(summary = "领取优惠券")
    public R<UserCoupon> claim(@Parameter(description = "用户ID") @RequestParam Long userId,
                               @Parameter(description = "模板ID") @RequestParam Long templateId) {
        return R.ok(couponService.claim(userId, templateId));
    }

    @GetMapping("/my")
    @Operation(summary = "查询我的优惠券")
    public R<Page<UserCoupon>> myCoupons(@Parameter(description = "用户ID") @RequestParam Long userId,
                                          @Parameter(description = "优惠券状态") @RequestParam(required = false) Integer status,
                                          @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
                                          @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size) {
        return R.ok(couponService.listUserCoupons(userId, status, page, size));
    }

    @GetMapping("/available/list")
    @Operation(summary = "查询可用优惠券列表")
    public R<List<UserCoupon>> availableList(@Parameter(description = "用户ID") @RequestParam Long userId) {
        return R.ok(couponService.listAvailableCoupons(userId));
    }
}
