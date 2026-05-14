package com.supermarket.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supermarket.common.core.result.R;
import com.supermarket.product.dto.CreateProductRequest;
import com.supermarket.product.entity.Sku;
import com.supermarket.product.entity.Spu;
import com.supermarket.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping("/spu")
    public R<Spu> createProduct(@Valid @RequestBody CreateProductRequest request) {
        return R.ok(productService.createProduct(request));
    }

    @GetMapping("/spu/{spuId}")
    public R<Spu> getSpu(@PathVariable Long spuId) {
        return R.ok(productService.getSpuById(spuId));
    }

    @GetMapping("/spu/{spuId}/skus")
    public R<List<Sku>> getSkus(@PathVariable Long spuId) {
        return R.ok(productService.getSkusBySpuId(spuId));
    }

    @PutMapping("/spu/{spuId}/audit")
    public R<Void> audit(@PathVariable Long spuId,
                         @RequestParam Integer auditStatus,
                         @RequestParam(required = false) String reason) {
        productService.auditProduct(spuId, auditStatus, reason);
        return R.ok();
    }

    @PutMapping("/spu/{spuId}/shelf")
    public R<Void> updateShelf(@PathVariable Long spuId, @RequestParam Integer shelfStatus) {
        productService.updateShelfStatus(spuId, shelfStatus);
        return R.ok();
    }

    @GetMapping("/list/shop/{shopId}")
    public R<Page<Spu>> listByShop(@PathVariable Long shopId,
                                    @RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        return R.ok(productService.listByShop(shopId, page, size));
    }

    @GetMapping("/list/category/{categoryId}")
    public R<Page<Spu>> listByCategory(@PathVariable Long categoryId,
                                        @RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "20") int size,
                                        @RequestParam(required = false) String sort) {
        return R.ok(productService.listByCategory(categoryId, page, size, sort));
    }

    @GetMapping("/search")
    public R<Page<Spu>> search(@RequestParam String keyword,
                               @RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "20") int size) {
        return R.ok(productService.searchByName(keyword, page, size));
    }

    @GetMapping("/list")
    public R<Page<Spu>> list(@RequestParam(required = false) Integer auditStatus,
                             @RequestParam(required = false) String keyword,
                             @RequestParam(required = false) Long categoryId,
                             @RequestParam(defaultValue = "1") int page,
                             @RequestParam(defaultValue = "20") int size) {
        return R.ok(productService.listForAdmin(auditStatus, keyword, categoryId, page, size));
    }
}
