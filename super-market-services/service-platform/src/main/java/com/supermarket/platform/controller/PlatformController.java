package com.supermarket.platform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supermarket.common.core.result.R;
import com.supermarket.platform.entity.*;
import com.supermarket.platform.service.PlatformService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/platform")
@RequiredArgsConstructor
public class PlatformController {

    private final PlatformService platformService;

    // ===== Banner (前台) =====
    @GetMapping("/banners")
    public R<List<Banner>> activeBanners(@RequestParam(defaultValue = "HOME_TOP") String position) {
        return R.ok(platformService.getActiveBanners(position));
    }

    // ===== Banner (管理) =====
    @PostMapping("/admin/banner")
    public R<Banner> createBanner(@RequestBody Banner banner) {
        return R.ok(platformService.createBanner(banner));
    }

    @PutMapping("/admin/banner/{id}")
    public R<Banner> updateBanner(@PathVariable Long id, @RequestBody Banner banner) {
        banner.setId(id);
        return R.ok(platformService.updateBanner(banner));
    }

    @DeleteMapping("/admin/banner/{id}")
    public R<Void> deleteBanner(@PathVariable Long id) {
        platformService.deleteBanner(id);
        return R.ok();
    }

    @GetMapping("/admin/banners")
    public R<Page<Banner>> listBanners(@RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        return R.ok(platformService.listBanners(page, size));
    }

    // ===== 广告位 =====
    @PostMapping("/admin/position")
    public R<AdPosition> createPosition(@RequestBody AdPosition position) {
        return R.ok(platformService.createPosition(position));
    }

    @GetMapping("/admin/positions")
    public R<List<AdPosition>> listPositions() {
        return R.ok(platformService.listPositions());
    }

    // ===== 风控 =====
    @PostMapping("/admin/risk/rule")
    public R<RiskRule> createRule(@RequestBody RiskRule rule) {
        return R.ok(platformService.createRule(rule));
    }

    @GetMapping("/admin/risk/rules")
    public R<List<RiskRule>> listRules() {
        return R.ok(platformService.listRules());
    }

    @PostMapping("/admin/risk/evaluate")
    public R<Map<String, Object>> evaluate(@RequestParam Long userId,
                                           @RequestParam String targetId,
                                           @RequestParam Integer riskType) {
        return R.ok(platformService.evaluateRisk(userId, targetId, riskType));
    }

    @GetMapping("/admin/risk/logs")
    public R<Page<RiskLog>> riskLogs(@RequestParam(required = false) Integer riskType,
                                     @RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        return R.ok(platformService.listRiskLogs(riskType, page, size));
    }
}
