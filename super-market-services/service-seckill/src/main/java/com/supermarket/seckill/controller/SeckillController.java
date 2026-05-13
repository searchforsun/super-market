package com.supermarket.seckill.controller;

import com.supermarket.common.core.result.R;
import com.supermarket.seckill.entity.SeckillProduct;
import com.supermarket.seckill.entity.SeckillSession;
import com.supermarket.seckill.service.SeckillService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/seckill")
@RequiredArgsConstructor
public class SeckillController {

    private final SeckillService seckillService;

    @PostMapping("/admin/session")
    public R<SeckillSession> createSession(@RequestBody SeckillSession session) {
        return R.ok(seckillService.createSession(session));
    }

    @PostMapping("/admin/product")
    public R<SeckillProduct> createProduct(@RequestBody SeckillProduct product) {
        return R.ok(seckillService.createProduct(product));
    }

    @PostMapping("/admin/preheat/{id}")
    public R<Void> preheat(@PathVariable Long id) {
        seckillService.preheat(id);
        return R.ok();
    }

    @GetMapping("/sessions")
    public R<List<SeckillSession>> sessions() {
        return R.ok(seckillService.listSessions());
    }

    @GetMapping("/products")
    public R<List<SeckillProduct>> products(@RequestParam Long sessionId) {
        return R.ok(seckillService.listProducts(sessionId));
    }

    @PostMapping("/execute")
    public R<Map<String, Object>> execute(@RequestParam Long userId,
                                          @RequestParam Long seckillProductId,
                                          @RequestParam(defaultValue = "1") int quantity) {
        Map<String, Object> result = seckillService.execute(userId, seckillProductId, quantity);
        if (Boolean.TRUE.equals(result.get("success"))) {
            return R.ok(result);
        }
        return R.fail(400, (String) result.get("message"));
    }
}
