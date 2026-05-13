package com.supermarket.shop.controller;

import com.supermarket.common.core.result.R;
import com.supermarket.shop.entity.Merchant;
import com.supermarket.shop.entity.Shop;
import com.supermarket.shop.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shop")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;

    @PostMapping("/merchant/apply")
    public R<Merchant> apply(@RequestBody Merchant merchant) {
        return R.ok(shopService.applyMerchant(merchant));
    }

    @PutMapping("/merchant/{id}/audit")
    public R<Void> audit(@PathVariable Long id,
                         @RequestParam Integer auditStatus,
                         @RequestParam(required = false) String reason) {
        shopService.auditMerchant(id, auditStatus, reason);
        return R.ok();
    }

    @GetMapping("/{id}")
    public R<Shop> getShop(@PathVariable Long id) {
        return R.ok(shopService.getShopById(id));
    }

    @GetMapping("/merchant/{merchantId}")
    public R<Shop> getByMerchant(@PathVariable Long merchantId) {
        return R.ok(shopService.getShopByMerchantId(merchantId));
    }
}
