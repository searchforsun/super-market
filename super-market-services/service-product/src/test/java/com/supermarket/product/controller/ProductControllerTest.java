package com.supermarket.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supermarket.common.web.handler.GlobalExceptionHandler;
import com.supermarket.product.dto.CreateProductRequest;
import com.supermarket.product.dto.UpdateProductRequest;
import com.supermarket.product.dto.UpdateSkuRequest;
import com.supermarket.product.entity.Sku;
import com.supermarket.product.entity.Spu;
import com.supermarket.product.service.ProductService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {ProductController.class, ProductControllerTest.TestConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @Configuration
    @EnableAutoConfiguration
    @ComponentScan(basePackageClasses = ProductController.class)
    @Import(GlobalExceptionHandler.class)
    static class TestConfig {
    }

    @Test
    void shouldCreateProductWhenValidRequest() throws Exception {
        CreateProductRequest request = new CreateProductRequest();
        request.setShopId(1L);
        request.setCategoryId(2L);
        request.setName("Test Product");

        Spu spu = new Spu();
        spu.setId(100L);
        spu.setName("Test Product");
        spu.setShopId(1L);

        when(productService.createProduct(any())).thenReturn(spu);

        mockMvc.perform(post("/api/product/spu")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.name").value("Test Product"));
    }

    @Test
    void shouldReturnSpuWhenGetSpuById() throws Exception {
        Spu spu = new Spu();
        spu.setId(100L);
        spu.setName("Test Spu");
        spu.setSpuNo("SPU20240001");

        when(productService.getSpuById(100L)).thenReturn(spu);

        mockMvc.perform(get("/api/product/spu/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value("Test Spu"))
                .andExpect(jsonPath("$.data.spuNo").value("SPU20240001"));
    }

    @Test
    void shouldReturnSkusWhenGetSkusBySpuId() throws Exception {
        Sku sku = new Sku();
        sku.setId(1L);
        sku.setSpuId(100L);
        sku.setSpecName("Size M");
        sku.setPrice(new BigDecimal("99.99"));

        when(productService.getSkusBySpuId(100L)).thenReturn(List.of(sku));

        mockMvc.perform(get("/api/product/spu/100/skus"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].specName").value("Size M"));
    }

    @Test
    void shouldAuditWhenValidParams() throws Exception {
        doNothing().when(productService).auditProduct(100L, 1, "Approved");

        mockMvc.perform(put("/api/product/spu/100/audit")
                        .param("auditStatus", "1")
                        .param("reason", "Approved"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"));
    }

    @Test
    void shouldUpdateShelfWhenValidStatus() throws Exception {
        doNothing().when(productService).updateShelfStatus(100L, 1);

        mockMvc.perform(put("/api/product/spu/100/shelf")
                        .param("shelfStatus", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"));
    }

    @Test
    void shouldReturnPageWhenListByShop() throws Exception {
        Spu spu = new Spu();
        spu.setId(100L);
        spu.setName("Shop Product");
        Page<Spu> page = new Page<>(1, 20);
        page.setRecords(List.of(spu));
        page.setTotal(1);

        when(productService.listByShop(eq(1L), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/api/product/list/shop/1")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records[0].name").value("Shop Product"));
    }

    @Test
    void shouldReturnPageWhenListByCategory() throws Exception {
        Spu spu = new Spu();
        spu.setId(100L);
        spu.setName("Category Product");
        Page<Spu> page = new Page<>(1, 20);
        page.setRecords(List.of(spu));
        page.setTotal(1);

        when(productService.listByCategory(eq(1L), anyInt(), anyInt(), any())).thenReturn(page);

        mockMvc.perform(get("/api/product/list/category/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records[0].name").value("Category Product"));
    }

    @Test
    void shouldReturnPageWhenSearchByKeyword() throws Exception {
        Spu spu = new Spu();
        spu.setId(100L);
        spu.setName("Searched Product");
        Page<Spu> page = new Page<>(1, 20);
        page.setRecords(List.of(spu));
        page.setTotal(1);

        when(productService.searchByName(eq("phone"), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/api/product/search")
                        .param("keyword", "phone"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records[0].name").value("Searched Product"));
    }

    @Test
    void shouldReturnPageWhenListForAdmin() throws Exception {
        Spu spu = new Spu();
        spu.setId(100L);
        spu.setName("Admin Product");
        Page<Spu> page = new Page<>(1, 20);
        page.setRecords(List.of(spu));
        page.setTotal(1);

        when(productService.listForAdmin(any(), any(), any(), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/api/product/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records[0].name").value("Admin Product"));
    }

    @Test
    void shouldUpdateSpuWhenValidRequest() throws Exception {
        UpdateProductRequest request = new UpdateProductRequest();
        request.setName("Updated Name");

        doNothing().when(productService).updateSpu(eq(100L), any(UpdateProductRequest.class));

        mockMvc.perform(put("/api/product/spu/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"));
    }

    @Test
    void shouldUpdateSkuWhenValidRequest() throws Exception {
        UpdateSkuRequest request = new UpdateSkuRequest();
        request.setPrice(new BigDecimal("49.99"));

        doNothing().when(productService).updateSku(eq(1L), any(UpdateSkuRequest.class));

        mockMvc.perform(put("/api/product/sku/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"));
    }
}
