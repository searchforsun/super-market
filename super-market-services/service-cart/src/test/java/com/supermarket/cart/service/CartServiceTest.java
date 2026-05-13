package com.supermarket.cart.service;

import com.supermarket.cart.dto.CartItemDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class CartServiceTest {

    @Autowired
    private CartService cartService;

    private long userId;

    @BeforeEach
    void setUp() {
        userId = System.nanoTime();
    }

    @Test
    void shouldAddItem() {
        cartService.addItem(userId, newItem(1001L, 2));
        List<CartItemDTO> list = cartService.getCartList(userId);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    void shouldUpdateQuantity() {
        cartService.addItem(userId, newItem(1001L, 1));
        cartService.updateQuantity(userId, 1001L, 5);
        List<CartItemDTO> list = cartService.getCartList(userId);
        assertThat(list.get(0).getQuantity()).isEqualTo(5);
    }

    @Test
    void shouldRemoveItem() {
        cartService.addItem(userId, newItem(1001L, 1));
        cartService.removeItem(userId, 1001L);
        assertThat(cartService.getCartList(userId)).isEmpty();
    }

    @Test
    void shouldGetSelectedItems() {
        cartService.addItem(userId, newItem(1001L, 1));
        cartService.addItem(userId, newItem(1002L, 1));
        cartService.selectItem(userId, 1001L, false);
        List<CartItemDTO> selected = cartService.getSelectedItems(userId);
        assertThat(selected).hasSize(1);
    }

    @Test
    void shouldClearSelected() {
        cartService.addItem(userId, newItem(1001L, 1));
        cartService.addItem(userId, newItem(1002L, 1));
        cartService.selectItem(userId, 1002L, false);
        cartService.clearSelected(userId);
        assertThat(cartService.getCartList(userId)).hasSize(1);
    }

    private CartItemDTO newItem(Long skuId, int qty) {
        CartItemDTO item = new CartItemDTO();
        item.setSkuId(skuId);
        item.setSpuId(skuId + 10000);
        item.setSpuName("测试商品");
        item.setSkuSpec("默认规格");
        item.setPrice(new BigDecimal("99.00"));
        item.setQuantity(qty);
        return item;
    }
}
