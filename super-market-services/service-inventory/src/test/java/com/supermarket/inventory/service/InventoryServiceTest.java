package com.supermarket.inventory.service;

import com.supermarket.inventory.entity.Inventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class InventoryServiceTest {

    @MockBean
    private RedissonClient redissonClient;

    @Autowired
    private InventoryService inventoryService;

    @BeforeEach
    void setUp() throws InterruptedException {
        RLock mockLock = mock(RLock.class);
        when(mockLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(mockLock.isHeldByCurrentThread()).thenReturn(true);
        when(redissonClient.getLock(anyString())).thenReturn(mockLock);
    }

    @Test
    void shouldInitStock() {
        Inventory inv = inventoryService.initStock(1001L, 100, 10);
        assertThat(inv.getTotalStock()).isEqualTo(100);
        assertThat(inv.getAvailableStock()).isEqualTo(100);
        assertThat(inv.getLockedStock()).isEqualTo(0);
    }

    @Test
    void shouldDeductStock() {
        inventoryService.initStock(1002L, 100, 5);
        boolean success = inventoryService.deductStock(1002L, 10);
        assertThat(success).isTrue();

        Inventory inv = inventoryService.getBySkuId(1002L);
        assertThat(inv.getAvailableStock()).isEqualTo(90);
        assertThat(inv.getLockedStock()).isEqualTo(10);
    }

    @Test
    void shouldFailDeductWhenInsufficient() {
        inventoryService.initStock(1003L, 5, 0);
        boolean success = inventoryService.deductStock(1003L, 10);
        assertThat(success).isFalse();
    }

    @Test
    void shouldRestoreStock() {
        inventoryService.initStock(1004L, 100, 5);
        inventoryService.deductStock(1004L, 10);
        boolean restored = inventoryService.restoreStock(1004L, 10);
        assertThat(restored).isTrue();

        Inventory inv = inventoryService.getBySkuId(1004L);
        assertThat(inv.getAvailableStock()).isEqualTo(100);
        assertThat(inv.getLockedStock()).isEqualTo(0);
    }

    @Test
    void shouldConfirmDeduct() {
        inventoryService.initStock(1005L, 100, 5);
        inventoryService.deductStock(1005L, 10);
        boolean confirmed = inventoryService.confirmDeduct(1005L, 10);
        assertThat(confirmed).isTrue();

        Inventory inv = inventoryService.getBySkuId(1005L);
        assertThat(inv.getTotalStock()).isEqualTo(90);
        assertThat(inv.getLockedStock()).isEqualTo(0);
    }
}
