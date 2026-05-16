package com.supermarket.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supermarket.common.core.result.R;
import com.supermarket.common.dubbo.api.order.dto.CreateOrderRequest;
import com.supermarket.order.entity.Order;
import com.supermarket.order.entity.OrderItem;
import com.supermarket.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
@Tag(name = "订单服务", description = "订单创建、查询、取消、发货、收货接口")
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/create")
    @Operation(summary = "创建订单")
    public R<Order> create(@Parameter(description = "创建订单请求") @Valid @RequestBody CreateOrderRequest request) {
        return R.ok(orderService.createOrder(request));
    }

    @GetMapping("/{orderNo}")
    @Operation(summary = "查询订单详情")
    public R<Order> detail(@Parameter(description = "订单号") @PathVariable String orderNo) {
        return R.ok(orderService.getByOrderNo(orderNo));
    }

    @GetMapping("/list/user/{userId}")
    @Operation(summary = "查询用户订单列表")
    public R<Page<Order>> listByUser(@Parameter(description = "用户ID") @PathVariable Long userId,
                                     @Parameter(description = "订单状态") @RequestParam(required = false) Integer status,
                                     @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
                                     @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size) {
        return R.ok(orderService.listByUser(userId, status, page, size));
    }

    @GetMapping("/{orderNo}/items")
    @Operation(summary = "查询订单商品明细")
    public R<List<OrderItem>> items(@Parameter(description = "订单号") @PathVariable String orderNo) {
        return R.ok(orderService.getOrderItems(orderNo));
    }

    @PutMapping("/{orderNo}/cancel")
    @Operation(summary = "取消订单")
    public R<Void> cancel(@Parameter(description = "订单号") @PathVariable @NotBlank(message = "订单号不能为空") String orderNo,
                          @Parameter(description = "取消原因") @RequestParam @Size(max = 500, message = "取消原因最长500字") String reason) {
        orderService.cancelOrder(orderNo, reason);
        return R.ok();
    }

    @PutMapping("/{orderNo}/ship")
    @Operation(summary = "订单发货")
    public R<Void> ship(@Parameter(description = "订单号") @PathVariable String orderNo) {
        orderService.ship(orderNo);
        return R.ok();
    }

    @PutMapping("/{orderNo}/receive")
    @Operation(summary = "确认收货")
    public R<Void> receive(@Parameter(description = "订单号") @PathVariable String orderNo) {
        orderService.confirmReceive(orderNo);
        return R.ok();
    }

    @GetMapping("/admin/list")
    @Operation(summary = "管理员查询订单列表")
    public R<Page<Order>> adminList(@Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
                                    @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size) {
        return R.ok(orderService.listAdmin(page, size));
    }
}
