package com.supermarket.address.controller;

import com.supermarket.address.entity.Address;
import com.supermarket.address.service.AddressService;
import com.supermarket.common.core.result.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/address")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @PostMapping
    public R<Address> create(@RequestBody Address address) {
        return R.ok(addressService.create(address));
    }

    @PutMapping
    public R<Address> update(@RequestBody Address address) {
        return R.ok(addressService.update(address));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id, @RequestParam Long userId) {
        addressService.delete(id, userId);
        return R.ok();
    }

    @GetMapping("/list")
    public R<List<Address>> list(@RequestParam Long userId) {
        return R.ok(addressService.listByUser(userId));
    }

    @PutMapping("/{id}/default")
    public R<Void> setDefault(@PathVariable Long id, @RequestParam Long userId) {
        addressService.setDefault(id, userId);
        return R.ok();
    }
}
