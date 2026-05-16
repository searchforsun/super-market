package com.supermarket.seckill.controller;

import com.supermarket.common.core.result.R;
import com.supermarket.seckill.entity.SeckillProduct;
import com.supermarket.seckill.entity.SeckillSession;
import com.supermarket.seckill.service.SeckillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/seckill")
@RequiredArgsConstructor
@Tag(name = "秒杀服务", description = "秒杀场次、商品管理、预热、执行秒杀接口")
public class SeckillController {

    private final SeckillService seckillService;

    @PostMapping("/admin/session")
    @Operation(summary = "创建秒杀场次")
    public R<SeckillSession> createSession(@Parameter(description = "秒杀场次信息") @RequestBody SeckillSession session) {
        return R.ok(seckillService.createSession(session));
    }

    @PostMapping("/admin/product")
    @Operation(summary = "创建秒杀商品")
    public R<SeckillProduct> createProduct(@Parameter(description = "秒杀商品信息") @RequestBody SeckillProduct product) {
        return R.ok(seckillService.createProduct(product));
    }

    @PostMapping("/admin/preheat/{id}")
    @Operation(summary = "预热秒杀商品缓存")
    public R<Void> preheat(@Parameter(description = "秒杀商品ID") @PathVariable Long id) {
        seckillService.preheat(id);
        return R.ok();
    }

    @GetMapping("/sessions")
    @Operation(summary = "获取秒杀场次列表")
    public R<List<SeckillSession>> sessions() {
        return R.ok(seckillService.listSessions());
    }

    @GetMapping("/products")
    @Operation(summary = "根据场次获取秒杀商品列表")
    public R<List<SeckillProduct>> products(@Parameter(description = "秒杀场次ID") @RequestParam Long sessionId) {
        return R.ok(seckillService.listProducts(sessionId));
    }

    @PostMapping("/execute")
    @Operation(summary = "执行秒杀")
    public R<Map<String, Object>> execute(@Parameter(description = "用户ID") @RequestParam Long userId,
                                          @Parameter(description = "秒杀商品ID") @RequestParam Long seckillProductId,
                                          @Parameter(description = "购买数量") @RequestParam(defaultValue = "1") int quantity) {
        Map<String, Object> result = seckillService.execute(userId, seckillProductId, quantity);
        if (Boolean.TRUE.equals(result.get("success"))) {
            return R.ok(result);
        }
        return R.fail(400, (String) result.get("message"));
    }
}
