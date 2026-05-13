package com.supermarket.cart.service.impl;

import cn.hutool.json.JSONUtil;
import com.supermarket.cart.dto.CartItemDTO;
import com.supermarket.cart.service.CartService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final RedissonClient redissonClient;

    private static final String CART_PREFIX = "smt:cart:";

    private RMap<String, String> getCartMap(Long userId) {
        return redissonClient.getMap(CART_PREFIX + userId);
    }

    @Override
    public void addItem(Long userId, CartItemDTO item) {
        RMap<String, String> cart = getCartMap(userId);
        String key = String.valueOf(item.getSkuId());
        String existing = cart.get(key);
        if (existing != null) {
            CartItemDTO existItem = JSONUtil.toBean(existing, CartItemDTO.class);
            existItem.setQuantity(existItem.getQuantity() + item.getQuantity());
            cart.put(key, JSONUtil.toJsonStr(existItem));
        } else {
            item.setSelected(true);
            cart.put(key, JSONUtil.toJsonStr(item));
        }
    }

    @Override
    public void updateQuantity(Long userId, Long skuId, Integer quantity) {
        RMap<String, String> cart = getCartMap(userId);
        String key = String.valueOf(skuId);
        String existing = cart.get(key);
        if (existing != null) {
            CartItemDTO item = JSONUtil.toBean(existing, CartItemDTO.class);
            item.setQuantity(quantity);
            cart.put(key, JSONUtil.toJsonStr(item));
        }
    }

    @Override
    public void removeItem(Long userId, Long skuId) {
        getCartMap(userId).remove(String.valueOf(skuId));
    }

    @Override
    public void selectItem(Long userId, Long skuId, Boolean selected) {
        RMap<String, String> cart = getCartMap(userId);
        String key = String.valueOf(skuId);
        String existing = cart.get(key);
        if (existing != null) {
            CartItemDTO item = JSONUtil.toBean(existing, CartItemDTO.class);
            item.setSelected(selected);
            cart.put(key, JSONUtil.toJsonStr(item));
        }
    }

    @Override
    public void selectAll(Long userId, Boolean selected) {
        RMap<String, String> cart = getCartMap(userId);
        for (Map.Entry<String, String> entry : cart.entrySet()) {
            CartItemDTO item = JSONUtil.toBean(entry.getValue(), CartItemDTO.class);
            item.setSelected(selected);
            cart.put(entry.getKey(), JSONUtil.toJsonStr(item));
        }
    }

    @Override
    public List<CartItemDTO> getCartList(Long userId) {
        return getCartMap(userId).values().stream()
            .map(v -> JSONUtil.toBean(v, CartItemDTO.class))
            .collect(Collectors.toList());
    }

    @Override
    public List<CartItemDTO> getSelectedItems(Long userId) {
        return getCartList(userId).stream()
            .filter(CartItemDTO::getSelected)
            .collect(Collectors.toList());
    }

    @Override
    public void clearSelected(Long userId) {
        RMap<String, String> cart = getCartMap(userId);
        List<String> toRemove = cart.entrySet().stream()
            .filter(e -> {
                CartItemDTO item = JSONUtil.toBean(e.getValue(), CartItemDTO.class);
                return item.getSelected();
            })
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        cart.keySet().removeAll(toRemove);
    }

    @Override
    public Integer getCartCount(Long userId) {
        return getCartMap(userId).size();
    }
}
