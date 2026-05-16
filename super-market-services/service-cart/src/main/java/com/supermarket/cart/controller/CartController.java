package com.supermarket.cart.controller;

import com.supermarket.cart.dto.CartItemDTO;
import com.supermarket.cart.service.CartService;
import com.supermarket.common.core.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "购物车服务", description = "购物车添加、修改、选中、查询接口")
public class CartController {

    private final CartService cartService;

    @PostMapping("/add")
    @Operation(summary = "添加商品到购物车")
    public R<Void> add(@Parameter(description = "用户ID") @RequestParam Long userId,
                       @Parameter(description = "购物车商品信息") @RequestBody CartItemDTO item) {
        cartService.addItem(userId, item);
        return R.ok();
    }

    @PutMapping("/item/{skuId}")
    @Operation(summary = "修改购物车商品数量")
    public R<Void> updateQuantity(@Parameter(description = "用户ID") @RequestParam Long userId,
                                   @Parameter(description = "SKU ID") @PathVariable Long skuId,
                                   @Parameter(description = "数量") @RequestParam Integer quantity) {
        cartService.updateQuantity(userId, skuId, quantity);
        return R.ok();
    }

    @DeleteMapping("/item/{skuId}")
    @Operation(summary = "删除购物车商品")
    public R<Void> remove(@Parameter(description = "用户ID") @RequestParam Long userId,
                          @Parameter(description = "SKU ID") @PathVariable Long skuId) {
        cartService.removeItem(userId, skuId);
        return R.ok();
    }

    @PutMapping("/item/{skuId}/select")
    @Operation(summary = "选中/取消选中购物车商品")
    public R<Void> select(@Parameter(description = "用户ID") @RequestParam Long userId,
                          @Parameter(description = "SKU ID") @PathVariable Long skuId,
                          @Parameter(description = "是否选中") @RequestParam Boolean selected) {
        cartService.selectItem(userId, skuId, selected);
        return R.ok();
    }

    @PutMapping("/select-all")
    @Operation(summary = "全选/取消全选购物车商品")
    public R<Void> selectAll(@Parameter(description = "用户ID") @RequestParam Long userId,
                             @Parameter(description = "是否全选") @RequestParam Boolean selected) {
        cartService.selectAll(userId, selected);
        return R.ok();
    }

    @GetMapping("/list")
    @Operation(summary = "查询购物车列表")
    public R<List<CartItemDTO>> list(@Parameter(description = "用户ID") @RequestParam Long userId) {
        return R.ok(cartService.getCartList(userId));
    }

    @GetMapping("/count")
    @Operation(summary = "查询购物车商品数量")
    public R<Integer> count(@Parameter(description = "用户ID") @RequestParam Long userId) {
        return R.ok(cartService.getCartCount(userId));
    }
}
