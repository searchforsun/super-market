package com.supermarket.common.dubbo.api.inventory;

public interface InventoryDubboService {
    boolean deduct(Long skuId, int quantity);
    boolean restore(Long skuId, int quantity);
}
