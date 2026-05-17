package com.supermarket.inventory.controller;

import com.supermarket.common.web.handler.GlobalExceptionHandler;
import com.supermarket.inventory.entity.Inventory;
import com.supermarket.inventory.service.impl.InventoryServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {InventoryController.class, InventoryControllerTest.TestConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Mock the concrete impl class so the mock inherits both InventoryService and
     * InventoryDubboService interfaces, satisfying Dubbo's service export check.
     */
    @MockBean
    private InventoryServiceImpl inventoryService;

    @Configuration
    @EnableAutoConfiguration
    @ComponentScan(basePackageClasses = InventoryController.class)
    @Import(GlobalExceptionHandler.class)
    static class TestConfig {
    }

    @Test
    void shouldInitStockWhenValidParams() throws Exception {
        Inventory inv = new Inventory();
        inv.setId(1L);
        inv.setSkuId(100L);
        inv.setTotalStock(100);
        inv.setAvailableStock(100);
        inv.setSafetyStock(10);

        when(inventoryService.initStock(100L, 100, 10)).thenReturn(inv);

        mockMvc.perform(post("/api/inventory/init")
                        .param("skuId", "100")
                        .param("totalStock", "100")
                        .param("safetyStock", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.skuId").value(100))
                .andExpect(jsonPath("$.data.totalStock").value(100));
    }

    @Test
    void shouldReturnInventoryWhenSkuExists() throws Exception {
        Inventory inv = new Inventory();
        inv.setId(1L);
        inv.setSkuId(100L);
        inv.setTotalStock(50);
        inv.setAvailableStock(30);

        when(inventoryService.getBySkuId(100L)).thenReturn(inv);

        mockMvc.perform(get("/api/inventory/sku/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.availableStock").value(30));
    }

    @Test
    void shouldReturn404WhenSkuNotFound() throws Exception {
        when(inventoryService.getBySkuId(999L)).thenReturn(null);

        mockMvc.perform(get("/api/inventory/sku/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(20010))
                .andExpect(jsonPath("$.message").value("库存信息不存在"));
    }

    @Test
    void shouldReturnTrueWhenDeductSuccess() throws Exception {
        when(inventoryService.deductStock(100L, 5)).thenReturn(true);

        mockMvc.perform(post("/api/inventory/deduct")
                        .param("skuId", "100")
                        .param("quantity", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void shouldReturn400WhenDeductFails() throws Exception {
        when(inventoryService.deductStock(100L, 999)).thenReturn(false);

        mockMvc.perform(post("/api/inventory/deduct")
                        .param("skuId", "100")
                        .param("quantity", "999"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(20008))
                .andExpect(jsonPath("$.message").value("库存不足"));
    }
}
