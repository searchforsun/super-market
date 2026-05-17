package com.supermarket.shop.controller;

import com.supermarket.common.core.result.R;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.supermarket.shop.entity.Merchant;
import com.supermarket.shop.entity.Shop;
import com.supermarket.shop.service.ShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/shop")
@RequiredArgsConstructor
@Tag(name = "店铺服务", description = "商家入驻、审核、店铺查询接口")
public class ShopController {

    private final ShopService shopService;

    @PostMapping("/merchant/apply")
    @Operation(summary = "商家入驻申请")
    public R<Merchant> apply(@Parameter(description = "商家入驻信息") @RequestBody Merchant merchant) {
        return R.ok(shopService.applyMerchant(merchant));
    }

    @PutMapping("/merchant/{id}/audit")
    @Operation(summary = "审核商家入驻")
    public R<Void> audit(@Parameter(description = "商家ID") @PathVariable Long id,
                         @Parameter(description = "审核状态") @NotNull @RequestParam Integer auditStatus,
                         @Parameter(description = "审核不通过原因") @Size(max = 500) @RequestParam(required = false) String reason) {
        shopService.auditMerchant(id, auditStatus, reason);
        return R.ok();
    }

    @GetMapping("/merchant/page")
    @Operation(summary = "分页查询商家列表")
    public R<IPage<Merchant>> pageMerchants(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "审核状态") @RequestParam(required = false) Integer auditStatus,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate) {
        return R.ok(shopService.pageMerchants(page, size, auditStatus, startDate, endDate));
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询店铺")
    public R<Shop> getShop(@Parameter(description = "店铺ID") @PathVariable Long id) {
        return R.ok(shopService.getShopById(id));
    }

    @GetMapping("/merchant/{merchantId}")
    @Operation(summary = "根据商家ID查询店铺")
    public R<Shop> getByMerchant(@Parameter(description = "商家ID") @PathVariable Long merchantId) {
        return R.ok(shopService.getShopByMerchantId(merchantId));
    }

    @GetMapping("/by-user/{userId}")
    @Operation(summary = "根据用户ID查询店铺ID")
    public R<Long> getShopIdByUserId(@Parameter(description = "用户ID") @PathVariable Long userId) {
        return R.ok(shopService.getShopIdByUserId(userId));
    }
}
