package com.supermarket.address.controller;

import com.supermarket.address.entity.Address;
import com.supermarket.address.service.AddressService;
import com.supermarket.common.core.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/address")
@RequiredArgsConstructor
@Tag(name = "地址服务", description = "收货地址增删改查接口")
public class AddressController {

    private final AddressService addressService;

    @Operation(summary = "新增收货地址")
    @PostMapping
    public R<Address> create(@Parameter(description = "收货地址信息") @RequestBody Address address) {
        return R.ok(addressService.create(address));
    }

    @Operation(summary = "修改收货地址")
    @PutMapping
    public R<Address> update(@Parameter(description = "收货地址信息") @RequestBody Address address) {
        return R.ok(addressService.update(address));
    }

    @Operation(summary = "删除收货地址")
    @DeleteMapping("/{id}")
    public R<Void> delete(@Parameter(description = "地址ID") @PathVariable Long id,
                          @Parameter(description = "用户ID") @RequestParam Long userId) {
        addressService.delete(id, userId);
        return R.ok();
    }

    @Operation(summary = "查询用户收货地址列表")
    @GetMapping("/list")
    public R<List<Address>> list(@Parameter(description = "用户ID") @RequestParam Long userId) {
        return R.ok(addressService.listByUser(userId));
    }

    @Operation(summary = "设置默认收货地址")
    @PutMapping("/{id}/default")
    public R<Void> setDefault(@Parameter(description = "地址ID") @PathVariable Long id,
                              @Parameter(description = "用户ID") @RequestParam Long userId) {
        addressService.setDefault(id, userId);
        return R.ok();
    }
}
