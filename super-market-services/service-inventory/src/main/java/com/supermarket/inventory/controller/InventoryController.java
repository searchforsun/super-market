package com.supermarket.inventory.controller;

import com.supermarket.common.core.result.R;
import com.supermarket.inventory.entity.Inventory;
import com.supermarket.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Tag(name = "库存服务", description = "库存初始化、查询、扣减接口")
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/init")
    @Operation(summary = "初始化库存")
    public R<Inventory> init(@Parameter(description = "SKU ID") @RequestParam Long skuId,
                              @Parameter(description = "总库存") @RequestParam int totalStock,
                              @Parameter(description = "安全库存") @RequestParam(defaultValue = "0") int safetyStock) {
        return R.ok(inventoryService.initStock(skuId, totalStock, safetyStock));
    }

    @GetMapping("/sku/{skuId}")
    @Operation(summary = "根据SKU ID查询库存")
    public R<Inventory> getBySku(@Parameter(description = "SKU ID") @PathVariable Long skuId) {
        Inventory inv = inventoryService.getBySkuId(skuId);
        if (inv == null) {
            return R.fail(404, "库存信息不存在");
        }
        return R.ok(inv);
    }

    @PostMapping("/deduct")
    @Operation(summary = "扣减库存")
    public R<Boolean> deduct(@Parameter(description = "SKU ID") @RequestParam Long skuId,
                             @Parameter(description = "扣减数量") @RequestParam int quantity) {
        boolean success = inventoryService.deductStock(skuId, quantity);
        return success ? R.ok(true) : R.fail(400, "库存不足或扣减失败");
    }
}
