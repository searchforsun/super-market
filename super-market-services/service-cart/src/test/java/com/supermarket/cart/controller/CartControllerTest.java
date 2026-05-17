package com.supermarket.cart.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supermarket.cart.dto.CartItemDTO;
import com.supermarket.cart.service.CartService;
import com.supermarket.common.web.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {CartController.class, CartControllerTest.TestConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CartService cartService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Configuration
    @EnableAutoConfiguration
    @ComponentScan(basePackageClasses = CartController.class)
    @Import(GlobalExceptionHandler.class)
    static class TestConfig {
    }

    @Test
    void shouldAddItemWhenValidInput() throws Exception {
        // Arrange
        CartItemDTO item = new CartItemDTO();
        item.setSkuId(100L);
        item.setQuantity(2);
        doNothing().when(cartService).addItem(anyLong(), any(CartItemDTO.class));

        // Act & Assert
        mockMvc.perform(post("/api/cart/add")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"));
        verify(cartService).addItem(anyLong(), any(CartItemDTO.class));
    }

    @Test
    void shouldUpdateQuantityWhenValidInput() throws Exception {
        // Arrange
        doNothing().when(cartService).updateQuantity(1L, 100L, 3);

        // Act & Assert
        mockMvc.perform(put("/api/cart/item/100")
                        .param("userId", "1")
                        .param("quantity", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"));
        verify(cartService).updateQuantity(1L, 100L, 3);
    }

    @Test
    void shouldRemoveItemWhenValidInput() throws Exception {
        // Arrange
        doNothing().when(cartService).removeItem(1L, 100L);

        // Act & Assert
        mockMvc.perform(delete("/api/cart/item/100")
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"));
        verify(cartService).removeItem(1L, 100L);
    }

    @Test
    void shouldSelectItemWhenValidInput() throws Exception {
        // Arrange
        doNothing().when(cartService).selectItem(1L, 100L, true);

        // Act & Assert
        mockMvc.perform(put("/api/cart/item/100/select")
                        .param("userId", "1")
                        .param("selected", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"));
        verify(cartService).selectItem(1L, 100L, true);
    }

    @Test
    void shouldSelectAllWhenValidInput() throws Exception {
        // Arrange
        doNothing().when(cartService).selectAll(1L, true);

        // Act & Assert
        mockMvc.perform(put("/api/cart/select-all")
                        .param("userId", "1")
                        .param("selected", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"));
        verify(cartService).selectAll(1L, true);
    }

    @Test
    void shouldListWhenValidUserId() throws Exception {
        // Arrange
        CartItemDTO item1 = new CartItemDTO();
        item1.setSkuId(100L);
        item1.setSpuName("商品1");
        item1.setQuantity(2);
        item1.setPrice(new BigDecimal("99.99"));

        CartItemDTO item2 = new CartItemDTO();
        item2.setSkuId(101L);
        item2.setSpuName("商品2");
        item2.setQuantity(1);
        item2.setPrice(new BigDecimal("199.99"));

        when(cartService.getCartList(1L)).thenReturn(List.of(item1, item2));

        // Act & Assert
        mockMvc.perform(get("/api/cart/list")
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].spuName").value("商品1"))
                .andExpect(jsonPath("$.data[1].spuName").value("商品2"));
        verify(cartService).getCartList(1L);
    }

    @Test
    void shouldCountWhenValidUserId() throws Exception {
        // Arrange
        when(cartService.getCartCount(1L)).thenReturn(5);

        // Act & Assert
        mockMvc.perform(get("/api/cart/count")
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(5));
        verify(cartService).getCartCount(1L);
    }
}
