package com.supermarket.platform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supermarket.common.core.result.R;
import com.supermarket.platform.entity.*;
import com.supermarket.platform.service.PlatformService;
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
@RequestMapping("/api/platform")
@RequiredArgsConstructor
@Tag(name = "平台服务", description = "Banner管理、广告位、风控规则接口")
public class PlatformController {

    private final PlatformService platformService;

    // ===== Banner (前台) =====
    @GetMapping("/banners")
    @Operation(summary = "获取启用的Banner列表")
    public R<List<Banner>> activeBanners(@Parameter(description = "广告位置") @RequestParam(defaultValue = "HOME_TOP") String position) {
        return R.ok(platformService.getActiveBanners(position));
    }

    // ===== Banner (管理) =====
    @PostMapping("/admin/banner")
    @Operation(summary = "创建Banner")
    public R<Banner> createBanner(@Parameter(description = "Banner信息") @RequestBody Banner banner) {
        return R.ok(platformService.createBanner(banner));
    }

    @PutMapping("/admin/banner/{id}")
    @Operation(summary = "更新Banner")
    public R<Banner> updateBanner(@Parameter(description = "Banner ID") @PathVariable Long id,
                                   @Parameter(description = "Banner信息") @RequestBody Banner banner) {
        banner.setId(id);
        return R.ok(platformService.updateBanner(banner));
    }

    @DeleteMapping("/admin/banner/{id}")
    @Operation(summary = "删除Banner")
    public R<Void> deleteBanner(@Parameter(description = "Banner ID") @PathVariable Long id) {
        platformService.deleteBanner(id);
        return R.ok();
    }

    @GetMapping("/admin/banners")
    @Operation(summary = "分页查询Banner列表")
    public R<Page<Banner>> listBanners(@Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
                                        @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size) {
        return R.ok(platformService.listBanners(page, size));
    }

    // ===== 广告位 =====
    @PostMapping("/admin/position")
    @Operation(summary = "创建广告位")
    public R<AdPosition> createPosition(@Parameter(description = "广告位信息") @RequestBody AdPosition position) {
        return R.ok(platformService.createPosition(position));
    }

    @GetMapping("/admin/positions")
    @Operation(summary = "获取广告位列表")
    public R<List<AdPosition>> listPositions() {
        return R.ok(platformService.listPositions());
    }

    // ===== 风控 =====
    @PostMapping("/admin/risk/rule")
    @Operation(summary = "创建风控规则")
    public R<RiskRule> createRule(@Parameter(description = "风控规则信息") @RequestBody RiskRule rule) {
        return R.ok(platformService.createRule(rule));
    }

    @GetMapping("/admin/risk/rules")
    @Operation(summary = "获取风控规则列表")
    public R<List<RiskRule>> listRules() {
        return R.ok(platformService.listRules());
    }

    @PostMapping("/admin/risk/evaluate")
    @Operation(summary = "执行风险评估")
    public R<Map<String, Object>> evaluate(@Parameter(description = "用户ID") @RequestParam Long userId,
                                           @Parameter(description = "目标ID") @RequestParam String targetId,
                                           @Parameter(description = "风险类型") @RequestParam Integer riskType) {
        return R.ok(platformService.evaluateRisk(userId, targetId, riskType));
    }

    @GetMapping("/admin/risk/logs")
    @Operation(summary = "分页查询风控日志")
    public R<Page<RiskLog>> riskLogs(@Parameter(description = "风险类型") @RequestParam(required = false) Integer riskType,
                                     @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
                                     @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size) {
        return R.ok(platformService.listRiskLogs(riskType, page, size));
    }

    // ===== Dashboard =====
    @GetMapping("/admin/dashboard/stats")
    @Operation(summary = "获取运营看板统计")
    public R<Map<String, Object>> dashboardStats() {
        return R.ok(platformService.getDashboardStats());
    }

    @GetMapping("/admin/dashboard/trend")
    @Operation(summary = "获取近N日销售趋势")
    public R<List<Map<String, Object>>> dailyTrend(@Parameter(description = "天数") @RequestParam(defaultValue = "7") int days) {
        return R.ok(platformService.getDailyTrend(days));
    }

    @GetMapping("/admin/dashboard/category-sales")
    @Operation(summary = "获取分类销售占比")
    public R<List<Map<String, Object>>> categorySales() {
        return R.ok(platformService.getCategorySales());
    }

    @GetMapping("/admin/dashboard/status-distribution")
    @Operation(summary = "获取订单状态分布")
    public R<List<Map<String, Object>>> statusDistribution() {
        return R.ok(platformService.getStatusDistribution());
    }

    @GetMapping("/admin/dashboard/pending-audit")
    @Operation(summary = "获取待审核数量")
    public R<Map<String, Object>> pendingAudit() {
        return R.ok(platformService.getPendingAudit());
    }

    @GetMapping("/admin/dashboard/user-trend")
    @Operation(summary = "获取用户增长趋势")
    public R<List<Map<String, Object>>> userTrend(@Parameter(description = "天数") @RequestParam(defaultValue = "7") int days) {
        return R.ok(platformService.getUserTrend(days));
    }

    // ===== Merchant Dashboard =====
    @GetMapping("/merchant/dashboard/stats")
    @Operation(summary = "获取商家看板统计")
    public R<Map<String, Object>> merchantStats(@Parameter(description = "店铺ID") @RequestParam Long shopId) {
        return R.ok(platformService.getMerchantStats(shopId));
    }

    @GetMapping("/merchant/dashboard/trend")
    @Operation(summary = "获取商家近N日销售趋势")
    public R<List<Map<String, Object>>> merchantDailyTrend(@Parameter(description = "店铺ID") @RequestParam Long shopId,
                                                            @Parameter(description = "天数") @RequestParam(defaultValue = "7") int days) {
        return R.ok(platformService.getMerchantDailyTrend(shopId, days));
    }

    @GetMapping("/merchant/dashboard/status-distribution")
    @Operation(summary = "获取商家订单状态分布")
    public R<List<Map<String, Object>>> merchantStatusDistribution(@Parameter(description = "店铺ID") @RequestParam Long shopId) {
        return R.ok(platformService.getMerchantStatusDistribution(shopId));
    }

    // ===== Reports =====
    @GetMapping("/admin/report/gmv")
    @Operation(summary = "获取GMV报表")
    public R<Map<String, Object>> gmvReport(@Parameter(description = "开始日期") @RequestParam String startDate,
                                            @Parameter(description = "结束日期") @RequestParam String endDate) {
        return R.ok(platformService.getGmvReport(startDate, endDate));
    }

    @GetMapping("/admin/report/orders")
    @Operation(summary = "获取订单报表")
    public R<Map<String, Object>> orderReport(@Parameter(description = "开始日期") @RequestParam String startDate,
                                               @Parameter(description = "结束日期") @RequestParam String endDate) {
        return R.ok(platformService.getOrderReport(startDate, endDate));
    }

    @GetMapping("/admin/report/users")
    @Operation(summary = "获取用户报表")
    public R<Map<String, Object>> userReport(@Parameter(description = "开始日期") @RequestParam String startDate,
                                              @Parameter(description = "结束日期") @RequestParam String endDate) {
        return R.ok(platformService.getUserReport(startDate, endDate));
    }
}
