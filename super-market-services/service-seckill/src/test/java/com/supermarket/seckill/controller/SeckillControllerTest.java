package com.supermarket.seckill.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.supermarket.common.web.handler.GlobalExceptionHandler;
import com.supermarket.seckill.entity.SeckillProduct;
import com.supermarket.seckill.entity.SeckillSession;
import com.supermarket.seckill.service.SeckillService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SeckillController.class)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
class SeckillControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SeckillService seckillService;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void shouldCreateSessionWhenRequestIsValid() throws Exception {
        // Arrange
        SeckillSession session = new SeckillSession();
        session.setName("限时秒杀");
        session.setStartTime(LocalDateTime.now());
        session.setEndTime(LocalDateTime.now().plusHours(2));

        SeckillSession created = new SeckillSession();
        created.setId(1L);
        created.setName("限时秒杀");
        created.setStatus(1);

        when(seckillService.createSession(any(SeckillSession.class))).thenReturn(created);

        // Act & Assert
        mockMvc.perform(post("/api/seckill/admin/session")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.name").value("限时秒杀"));

        verify(seckillService).createSession(any(SeckillSession.class));
    }

    @Test
    void shouldCreateProductWhenRequestIsValid() throws Exception {
        // Arrange
        SeckillProduct product = new SeckillProduct();
        product.setSessionId(1L);
        product.setSpuId(100L);
        product.setSeckillPrice(new BigDecimal("99.99"));
        product.setSeckillStock(100);

        SeckillProduct created = new SeckillProduct();
        created.setId(1L);
        created.setSessionId(1L);
        created.setStatus(1);

        when(seckillService.createProduct(any(SeckillProduct.class))).thenReturn(created);

        // Act & Assert
        mockMvc.perform(post("/api/seckill/admin/product")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.sessionId").value(1L));

        verify(seckillService).createProduct(any(SeckillProduct.class));
    }

    @Test
    void shouldPreheatWhenIdIsValid() throws Exception {
        // Arrange
        doNothing().when(seckillService).preheat(1L);

        // Act & Assert
        mockMvc.perform(post("/api/seckill/admin/preheat/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"));

        verify(seckillService).preheat(1L);
    }

    @Test
    void shouldReturnSessionsWhenCalled() throws Exception {
        // Arrange
        SeckillSession session = new SeckillSession();
        session.setId(1L);
        session.setName("今日秒杀");
        session.setStatus(1);

        when(seckillService.listSessions()).thenReturn(List.of(session));

        // Act & Assert
        mockMvc.perform(get("/api/seckill/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value(1L))
                .andExpect(jsonPath("$.data[0].name").value("今日秒杀"));

        verify(seckillService).listSessions();
    }

    @Test
    void shouldReturnProductsWhenSessionIdProvided() throws Exception {
        // Arrange
        SeckillProduct product = new SeckillProduct();
        product.setId(1L);
        product.setSessionId(1L);
        product.setSeckillPrice(new BigDecimal("49.99"));

        when(seckillService.listProducts(1L)).thenReturn(List.of(product));

        // Act & Assert
        mockMvc.perform(get("/api/seckill/products")
                .param("sessionId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].sessionId").value(1L));

        verify(seckillService).listProducts(1L);
    }

    @Test
    void shouldExecuteSeckillWhenRequestIsValid() throws Exception {
        // Arrange
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("orderId", 10001L);

        when(seckillService.execute(1L, 1L, 1)).thenReturn(result);

        // Act & Assert
        mockMvc.perform(post("/api/seckill/execute")
                .param("userId", "1")
                .param("seckillProductId", "1")
                .param("quantity", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.orderId").value(10001L));

        verify(seckillService).execute(1L, 1L, 1);
    }

    @Test
    void shouldFailExecuteWhenStockInsufficient() throws Exception {
        // Arrange
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", "库存不足");

        when(seckillService.execute(1L, 1L, 10)).thenReturn(result);

        // Act & Assert
        mockMvc.perform(post("/api/seckill/execute")
                .param("userId", "1")
                .param("seckillProductId", "1")
                .param("quantity", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("库存不足"));

        verify(seckillService).execute(1L, 1L, 10);
    }
}
