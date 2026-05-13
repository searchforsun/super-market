package com.supermarket.inventory.service;

import com.supermarket.inventory.entity.Inventory;

public interface InventoryService {

    Inventory initStock(Long skuId, int totalStock, int safetyStock);

    Inventory getBySkuId(Long skuId);

    boolean deductStock(Long skuId, int quantity);

    boolean restoreStock(Long skuId, int quantity);

    boolean confirmDeduct(Long skuId, int quantity);
}
