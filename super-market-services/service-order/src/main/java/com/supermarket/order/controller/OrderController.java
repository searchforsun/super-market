package com.supermarket.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supermarket.common.core.result.R;
import com.supermarket.common.dubbo.api.order.dto.CreateOrderRequest;
import com.supermarket.order.entity.Order;
import com.supermarket.order.entity.OrderItem;
import com.supermarket.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/create")
    public R<Order> create(@RequestBody CreateOrderRequest request) {
        return R.ok(orderService.createOrder(request));
    }

    @GetMapping("/{orderNo}")
    public R<Order> detail(@PathVariable String orderNo) {
        return R.ok(orderService.getByOrderNo(orderNo));
    }

    @GetMapping("/list/user/{userId}")
    public R<Page<Order>> listByUser(@PathVariable Long userId,
                                     @RequestParam(required = false) Integer status,
                                     @RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "10") int size) {
        return R.ok(orderService.listByUser(userId, status, page, size));
    }

    @GetMapping("/{orderNo}/items")
    public R<List<OrderItem>> items(@PathVariable String orderNo) {
        return R.ok(orderService.getOrderItems(orderNo));
    }

    @PutMapping("/{orderNo}/cancel")
    public R<Void> cancel(@PathVariable String orderNo, @RequestParam String reason) {
        orderService.cancelOrder(orderNo, reason);
        return R.ok();
    }

    @PutMapping("/{orderNo}/ship")
    public R<Void> ship(@PathVariable String orderNo) {
        orderService.ship(orderNo);
        return R.ok();
    }

    @PutMapping("/{orderNo}/receive")
    public R<Void> receive(@PathVariable String orderNo) {
        orderService.confirmReceive(orderNo);
        return R.ok();
    }
}
