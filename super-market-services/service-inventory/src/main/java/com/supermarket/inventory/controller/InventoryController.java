package com.supermarket.inventory.controller;

import com.supermarket.common.core.result.R;
import com.supermarket.inventory.entity.Inventory;
import com.supermarket.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/init")
    public R<Inventory> init(@RequestParam Long skuId,
                              @RequestParam int totalStock,
                              @RequestParam(defaultValue = "0") int safetyStock) {
        return R.ok(inventoryService.initStock(skuId, totalStock, safetyStock));
    }

    @GetMapping("/sku/{skuId}")
    public R<Inventory> getBySku(@PathVariable Long skuId) {
        Inventory inv = inventoryService.getBySkuId(skuId);
        if (inv == null) {
            return R.fail(404, "库存信息不存在");
        }
        return R.ok(inv);
    }

    @PostMapping("/deduct")
    public R<Boolean> deduct(@RequestParam Long skuId, @RequestParam int quantity) {
        boolean success = inventoryService.deductStock(skuId, quantity);
        return success ? R.ok(true) : R.fail(400, "库存不足或扣减失败");
    }
}
