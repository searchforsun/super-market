package com.supermarket.cart.service;

import com.supermarket.cart.dto.CartItemDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {CartServiceTest.TestConfig.class})
@ActiveProfiles("test")
class CartServiceTest {

    @Configuration
    @EnableAutoConfiguration
    @ComponentScan(basePackages = "com.supermarket.cart",
        excludeFilters = @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = com.supermarket.cart.config.DubboProviderConfig.class))
    static class TestConfig {
    }

    @MockBean
    private RedissonClient redissonClient;

    @Autowired
    private CartService cartService;

    private long userId;

    private final ConcurrentHashMap<String, ConcurrentHashMap<String, String>> cartStore = new ConcurrentHashMap<>();

    @BeforeEach
    void setUp() {
        userId = System.nanoTime();
        cartStore.clear();
        when(redissonClient.getMap(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            ConcurrentHashMap<String, String> map = cartStore.computeIfAbsent(key, k -> new ConcurrentHashMap<>());
            RMap<String, String> rmap = mock(RMap.class);
            doAnswer(a -> { map.put(a.getArgument(0), a.getArgument(1)); return null; }).when(rmap).put(anyString(), anyString());
            when(rmap.get(anyString())).thenAnswer(a -> map.get(a.getArgument(0)));
            doAnswer(a -> { map.remove(a.getArgument(0)); return null; }).when(rmap).remove(anyString());
            when(rmap.keySet()).thenReturn(map.keySet());
            when(rmap.values()).thenAnswer(a -> map.values());
            when(rmap.entrySet()).thenAnswer(a -> map.entrySet());
            when(rmap.size()).thenAnswer(a -> map.size());
            return rmap;
        });
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
