package com.supermarket.cart.controller;

import com.supermarket.cart.dto.CartItemDTO;
import com.supermarket.cart.service.CartService;
import com.supermarket.common.core.result.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping("/add")
    public R<Void> add(@RequestParam Long userId, @RequestBody CartItemDTO item) {
        cartService.addItem(userId, item);
        return R.ok();
    }

    @PutMapping("/item/{skuId}")
    public R<Void> updateQuantity(@RequestParam Long userId,
                                   @PathVariable Long skuId,
                                   @RequestParam Integer quantity) {
        cartService.updateQuantity(userId, skuId, quantity);
        return R.ok();
    }

    @DeleteMapping("/item/{skuId}")
    public R<Void> remove(@RequestParam Long userId, @PathVariable Long skuId) {
        cartService.removeItem(userId, skuId);
        return R.ok();
    }

    @PutMapping("/item/{skuId}/select")
    public R<Void> select(@RequestParam Long userId,
                          @PathVariable Long skuId,
                          @RequestParam Boolean selected) {
        cartService.selectItem(userId, skuId, selected);
        return R.ok();
    }

    @PutMapping("/select-all")
    public R<Void> selectAll(@RequestParam Long userId, @RequestParam Boolean selected) {
        cartService.selectAll(userId, selected);
        return R.ok();
    }

    @GetMapping("/list")
    public R<List<CartItemDTO>> list(@RequestParam Long userId) {
        return R.ok(cartService.getCartList(userId));
    }

    @GetMapping("/count")
    public R<Integer> count(@RequestParam Long userId) {
        return R.ok(cartService.getCartCount(userId));
    }
}
