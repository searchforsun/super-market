package com.supermarket.inventory.flow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supermarket.common.core.exception.BizException;
import com.supermarket.common.core.result.ResultCode;
import com.supermarket.inventory.entity.Inventory;
import com.supermarket.inventory.service.impl.InventoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Postman-style business flow test for the Inventory domain.
 * <p>
 * Chains API calls to simulate: Init Stock -> Query Stock -> Deduct ->
 * Deduct too much (expect 400).
 * <p>
 * Uses @SpringBootTest for full context but mocks InventoryService and RedissonClient
 * so no database or Redis middleware is required.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration," +
                "org.apache.dubbo.spring.boot.autoconfigure.DubboAutoConfiguration," +
                "com.alibaba.cloud.nacos.NacosDiscoveryAutoConfiguration," +
                "com.alibaba.cloud.nacos.NacosConfigAutoConfiguration",
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.nacos.config.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Inventory Business Flow Test")
class InventoryBusinessFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Mock the concrete impl class so the mock inherits both InventoryService and
     * InventoryDubboService interfaces, satisfying Dubbo's service export check.
     */
    @MockBean
    private InventoryServiceImpl inventoryService;

    @MockBean
    private RedissonClient redissonClient;

    private Inventory createInventory(Long id, Long skuId, int totalStock,
                                      int availableStock, int lockedStock, int safetyStock) {
        Inventory inv = new Inventory();
        inv.setId(id);
        inv.setSkuId(skuId);
        inv.setTotalStock(totalStock);
        inv.setAvailableStock(availableStock);
        inv.setLockedStock(lockedStock);
        inv.setSafetyStock(safetyStock);
        inv.setVersion(0);
        return inv;
    }

    @BeforeEach
    void setUp() {
        // InventoryService uses Redisson for distributed locks in deductStock().
        // The @MockBean replaces both InventoryService and RedissonClient,
        // so no actual locking or database operations occur.
    }

    // ---------------------------------------------------------------
    // Flow 1: Complete Stock Lifecycle
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Complete stock lifecycle: init -> query -> deduct -> verify remaining")
    void shouldCompleteFullInventoryLifecycleFlow() throws Exception {
        Long skuId = 100L;

        Inventory initializedInventory = createInventory(1L, skuId, 100, 100, 0, 10);
        Inventory afterDeductInventory = createInventory(1L, skuId, 100, 90, 10, 10);

        // --- Mock setup ---

        // Step 1: initStock returns inventory with 100 available
        when(inventoryService.initStock(eq(skuId), eq(100), eq(10))).thenReturn(initializedInventory);

        // Step 2: getBySkuId returns current state (changes as flow progresses)
        // Call sequence:
        //   1st: after init
        //   2nd: after successful deduct
        //   3rd: after failed deduct (stock should remain at 90)
        when(inventoryService.getBySkuId(skuId))
                .thenReturn(initializedInventory)
                .thenReturn(afterDeductInventory, afterDeductInventory);

        // Step 3: deductStock(true) = success
        when(inventoryService.deductStock(skuId, 10)).thenReturn(true);

        // Step 4: deductStock(false) = failure (insufficient stock)
        when(inventoryService.deductStock(skuId, 999)).thenReturn(false);

        // ============================================================
        // Step 1: Initialize stock: 100 units
        // ============================================================
        mockMvc.perform(post("/api/inventory/init")
                        .param("skuId", String.valueOf(skuId))
                        .param("totalStock", "100")
                        .param("safetyStock", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.skuId").value(skuId))
                .andExpect(jsonPath("$.data.totalStock").value(100))
                .andExpect(jsonPath("$.data.availableStock").value(100));

        // ============================================================
        // Step 2: Query inventory by SKU
        // ============================================================
        mockMvc.perform(get("/api/inventory/sku/{skuId}", skuId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.skuId").value(skuId))
                .andExpect(jsonPath("$.data.totalStock").value(100))
                .andExpect(jsonPath("$.data.availableStock").value(100));

        // ============================================================
        // Step 3: Deduct 10 units → success
        // ============================================================
        mockMvc.perform(post("/api/inventory/deduct")
                        .param("skuId", String.valueOf(skuId))
                        .param("quantity", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));

        // Verify remaining stock is 90
        mockMvc.perform(get("/api/inventory/sku/{skuId}", skuId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.availableStock").value(90));

        // ============================================================
        // Step 4: Deduct too much → fail with STOCK_INSUFFICIENT
        // ============================================================
        mockMvc.perform(post("/api/inventory/deduct")
                        .param("skuId", String.valueOf(skuId))
                        .param("quantity", "999"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(20008))
                .andExpect(jsonPath("$.message").value("库存不足"));

        // Verify stock unchanged at 90
        mockMvc.perform(get("/api/inventory/sku/{skuId}", skuId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.availableStock").value(90));

        // Verify all expected service interactions
        verify(inventoryService).initStock(eq(skuId), eq(100), eq(10));
        verify(inventoryService, times(3)).getBySkuId(skuId);
        verify(inventoryService).deductStock(skuId, 10);
        verify(inventoryService).deductStock(skuId, 999);
    }

    // ---------------------------------------------------------------
    // Flow 2: Duplicate Init
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Initialize inventory twice for same SKU returns 400")
    void shouldFailWhenInitializingDuplicateSku() throws Exception {
        Long skuId = 200L;

        // First call succeeds
        when(inventoryService.initStock(eq(skuId), eq(50), eq(0)))
                .thenReturn(createInventory(2L, skuId, 50, 50, 0, 0));

        // Second call throws exception
        when(inventoryService.initStock(eq(skuId), eq(100), eq(0)))
                .thenThrow(new BizException(ResultCode.STOCK_ALREADY_INIT));

        mockMvc.perform(post("/api/inventory/init")
                        .param("skuId", String.valueOf(skuId))
                        .param("totalStock", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/api/inventory/init")
                        .param("skuId", String.valueOf(skuId))
                        .param("totalStock", "100"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(20009));
    }

    // ---------------------------------------------------------------
    // Flow 3: Non-existent SKU Deduct
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Deduct from non-existent SKU returns STOCK_INSUFFICIENT")
    void shouldFailWhenDeductingNonExistentSku() throws Exception {
        when(inventoryService.deductStock(99999L, 1)).thenReturn(false);

        mockMvc.perform(post("/api/inventory/deduct")
                        .param("skuId", "99999")
                        .param("quantity", "1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(20008))
                .andExpect(jsonPath("$.message").value("库存不足"));
    }

    // ---------------------------------------------------------------
    // Flow 4: Non-existent SKU Query
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Query non-existent SKU returns STOCK_NOT_FOUND")
    void shouldReturn404ForNonExistentSku() throws Exception {
        when(inventoryService.getBySkuId(99999L)).thenReturn(null);

        mockMvc.perform(get("/api/inventory/sku/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(20010))
                .andExpect(jsonPath("$.message").value("库存信息不存在"));
    }
}
