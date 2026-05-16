package com.supermarket.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supermarket.common.core.result.R;
import com.supermarket.product.dto.CreateProductRequest;
import com.supermarket.product.dto.UpdateProductRequest;
import com.supermarket.product.dto.UpdateSkuRequest;
import com.supermarket.product.entity.Sku;
import com.supermarket.product.entity.Spu;
import com.supermarket.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
@Tag(name = "商品服务", description = "SPU/SKU管理、商品审核、上下架接口")
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "创建商品SPU")
    @PostMapping("/spu")
    public R<Spu> createProduct(@Parameter(description = "商品创建请求") @Valid @RequestBody CreateProductRequest request) {
        return R.ok(productService.createProduct(request));
    }

    @Operation(summary = "查询商品SPU详情")
    @GetMapping("/spu/{spuId}")
    public R<Spu> getSpu(@Parameter(description = "SPU ID") @PathVariable Long spuId) {
        return R.ok(productService.getSpuById(spuId));
    }

    @Operation(summary = "查询商品下SKU列表")
    @GetMapping("/spu/{spuId}/skus")
    public R<List<Sku>> getSkus(@Parameter(description = "SPU ID") @PathVariable Long spuId) {
        return R.ok(productService.getSkusBySpuId(spuId));
    }

    @Operation(summary = "审核商品")
    @PutMapping("/spu/{spuId}/audit")
    public R<Void> audit(@Parameter(description = "SPU ID") @PathVariable Long spuId,
                         @Parameter(description = "审核状态") @NotNull @Min(1) @Max(3) @RequestParam Integer auditStatus,
                         @Parameter(description = "审核原因") @Size(max = 500) @RequestParam(required = false) String reason) {
        productService.auditProduct(spuId, auditStatus, reason);
        return R.ok();
    }

    @Operation(summary = "商品上下架")
    @PutMapping("/spu/{spuId}/shelf")
    public R<Void> updateShelf(@Parameter(description = "SPU ID") @PathVariable Long spuId,
                               @Parameter(description = "上下架状态") @RequestParam Integer shelfStatus) {
        productService.updateShelfStatus(spuId, shelfStatus);
        return R.ok();
    }

    @Operation(summary = "按店铺分页查询商品")
    @GetMapping("/list/shop/{shopId}")
    public R<Page<Spu>> listByShop(@Parameter(description = "店铺ID") @PathVariable Long shopId,
                                    @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
                                    @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size) {
        return R.ok(productService.listByShop(shopId, page, size));
    }

    @Operation(summary = "按分类分页查询商品")
    @GetMapping("/list/category/{categoryId}")
    public R<Page<Spu>> listByCategory(@Parameter(description = "分类ID") @PathVariable Long categoryId,
                                        @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
                                        @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size,
                                        @Parameter(description = "排序方式") @RequestParam(required = false) String sort) {
        return R.ok(productService.listByCategory(categoryId, page, size, sort));
    }

    @Operation(summary = "搜索商品")
    @GetMapping("/search")
    public R<Page<Spu>> search(@Parameter(description = "搜索关键词") @RequestParam String keyword,
                               @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
                               @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size) {
        return R.ok(productService.searchByName(keyword, page, size));
    }

    @Operation(summary = "管理后台分页查询商品")
    @GetMapping("/list")
    public R<Page<Spu>> list(@Parameter(description = "审核状态") @RequestParam(required = false) Integer auditStatus,
                             @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
                             @Parameter(description = "分类ID") @RequestParam(required = false) Long categoryId,
                             @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
                             @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size) {
        return R.ok(productService.listForAdmin(auditStatus, keyword, categoryId, page, size));
    }

    @PutMapping("/spu/{spuId}")
    @Operation(summary = "更新商品SPU信息")
    public R<Void> updateSpu(@PathVariable Long spuId,
                              @Valid @RequestBody UpdateProductRequest request) {
        productService.updateSpu(spuId, request);
        return R.ok();
    }

    @PutMapping("/sku/{skuId}")
    @Operation(summary = "更新SKU信息")
    public R<Void> updateSku(@PathVariable Long skuId,
                              @Valid @RequestBody UpdateSkuRequest request) {
        productService.updateSku(skuId, request);
        return R.ok();
    }
}
