package com.supermarket.shop.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supermarket.common.dubbo.api.shop.ShopDubboService;
import com.supermarket.common.web.handler.GlobalExceptionHandler;
import com.supermarket.shop.entity.Merchant;
import com.supermarket.shop.entity.Shop;
import com.supermarket.shop.service.ShopService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {ShopController.class, ShopControllerTest.TestConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ShopControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ShopService shopService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Configuration
    @EnableAutoConfiguration
    @ComponentScan(basePackageClasses = ShopController.class)
    @Import(GlobalExceptionHandler.class)
    static class TestConfig {
        @Bean
        @Primary
        public ShopService shopService() {
            return mock(ShopService.class, withSettings().extraInterfaces(ShopDubboService.class));
        }
    }

    @Test
    void shouldApplyMerchantWhenRequestIsValid() throws Exception {
        // Arrange
        Merchant merchant = new Merchant();
        merchant.setUserId(1L);
        merchant.setCompanyName("测试公司");
        merchant.setContactPhone("13800138000");

        Merchant created = new Merchant();
        created.setId(1L);
        created.setUserId(1L);
        created.setCompanyName("测试公司");
        created.setAuditStatus(0);

        when(shopService.applyMerchant(any(Merchant.class))).thenReturn(created);

        // Act & Assert
        mockMvc.perform(post("/api/shop/merchant/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(merchant)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.companyName").value("测试公司"));

        verify(shopService).applyMerchant(any(Merchant.class));
    }

    @Test
    void shouldAuditMerchantWhenRequestIsValid() throws Exception {
        // Arrange
        doNothing().when(shopService).auditMerchant(1L, 1, "通过审核");

        // Act & Assert
        mockMvc.perform(put("/api/shop/merchant/1/audit")
                .param("auditStatus", "1")
                .param("reason", "通过审核"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"));

        verify(shopService).auditMerchant(1L, 1, "通过审核");
    }

    @Test
    void shouldReturnPageMerchantsWhenCalled() throws Exception {
        // Arrange
        Page<Merchant> page = new Page<>(1, 10);
        Merchant merchant = new Merchant();
        merchant.setId(1L);
        merchant.setCompanyName("测试公司");
        page.setRecords(java.util.List.of(merchant));
        page.setTotal(1);

        when(shopService.pageMerchants(1, 10, null, null, null)).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/shop/merchant/page")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records[0].companyName").value("测试公司"))
                .andExpect(jsonPath("$.data.total").value(1));

        verify(shopService).pageMerchants(1, 10, null, null, null);
    }

    @Test
    void shouldReturnShopWhenIdProvided() throws Exception {
        // Arrange
        Shop shop = new Shop();
        shop.setId(1L);
        shop.setShopName("测试店铺");
        shop.setMerchantId(1L);
        shop.setStatus(1);

        when(shopService.getShopById(1L)).thenReturn(shop);

        // Act & Assert
        mockMvc.perform(get("/api/shop/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.shopName").value("测试店铺"));

        verify(shopService).getShopById(1L);
    }

    @Test
    void shouldReturnShopWhenMerchantIdProvided() throws Exception {
        // Arrange
        Shop shop = new Shop();
        shop.setId(1L);
        shop.setMerchantId(1L);
        shop.setShopName("商家店铺");

        when(shopService.getShopByMerchantId(1L)).thenReturn(shop);

        // Act & Assert
        mockMvc.perform(get("/api/shop/merchant/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.shopName").value("商家店铺"));

        verify(shopService).getShopByMerchantId(1L);
    }
}
