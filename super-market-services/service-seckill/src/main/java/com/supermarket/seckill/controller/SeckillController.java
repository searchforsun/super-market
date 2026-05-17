package com.supermarket.seckill.controller;

import com.supermarket.common.core.exception.BizException;
import com.supermarket.common.core.result.R;
import com.supermarket.common.core.result.ResultCode;
import com.supermarket.seckill.entity.SeckillProduct;
import com.supermarket.seckill.entity.SeckillSession;
import com.supermarket.seckill.service.SeckillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Validated
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
    public R<SeckillProduct> createProduct(@Parameter(description = "场次ID") @RequestParam Long sessionId,
                                            @Parameter(description = "秒杀商品信息") @RequestBody Map<String, Object> body) {
        SeckillProduct product = new SeckillProduct();
        product.setSessionId(sessionId);
        if (body.get("spuId") != null) {
            product.setSpuId(Long.valueOf(body.get("spuId").toString()));
        }
        if (body.get("skuId") != null) {
            product.setSkuId(Long.valueOf(body.get("skuId").toString()));
        }
        if (body.get("seckillPrice") != null) {
            product.setSeckillPrice(new java.math.BigDecimal(body.get("seckillPrice").toString()));
        }
        if (body.get("seckillStock") != null) {
            product.setSeckillStock(Integer.valueOf(body.get("seckillStock").toString()));
        }
        if (body.get("limitPerUser") != null) {
            product.setLimitPerUser(Integer.valueOf(body.get("limitPerUser").toString()));
        }
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

    @GetMapping("/admin/sessions")
    @Operation(summary = "管理员获取全部秒杀场次列表")
    public R<List<SeckillSession>> adminSessions() {
        return R.ok(seckillService.listAllSessions());
    }

    @PutMapping("/admin/session/{id}")
    @Operation(summary = "更新秒杀场次")
    public R<SeckillSession> updateSession(@Parameter(description = "场次ID") @PathVariable Long id,
                                            @Parameter(description = "秒杀场次信息") @RequestBody SeckillSession session) {
        session.setId(id);
        return R.ok(seckillService.updateSession(session));
    }

    @DeleteMapping("/admin/session/{id}")
    @Operation(summary = "删除秒杀场次")
    public R<Void> deleteSession(@Parameter(description = "场次ID") @PathVariable Long id) {
        seckillService.deleteSession(id);
        return R.ok();
    }

    @DeleteMapping("/admin/product/{id}")
    @Operation(summary = "删除秒杀商品")
    public R<Void> deleteProduct(@Parameter(description = "秒杀商品ID") @PathVariable Long id) {
        seckillService.deleteProduct(id);
        return R.ok();
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
        String msg = (String) result.getOrDefault("message", ResultCode.SECKILL_STOCK_INSUFFICIENT.getMessage());
        throw new BizException(ResultCode.SECKILL_STOCK_INSUFFICIENT, msg);
    }
}
