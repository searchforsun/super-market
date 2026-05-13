package com.supermarket.cart.service;

import com.supermarket.cart.dto.CartItemDTO;
import java.util.List;

public interface CartService {
    void addItem(Long userId, CartItemDTO item);
    void updateQuantity(Long userId, Long skuId, Integer quantity);
    void removeItem(Long userId, Long skuId);
    void selectItem(Long userId, Long skuId, Boolean selected);
    void selectAll(Long userId, Boolean selected);
    List<CartItemDTO> getCartList(Long userId);
    List<CartItemDTO> getSelectedItems(Long userId);
    void clearSelected(Long userId);
    Integer getCartCount(Long userId);
}
