package com.supermarket.platform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supermarket.platform.entity.AdPosition;
import com.supermarket.platform.entity.Banner;
import com.supermarket.platform.entity.RiskLog;
import com.supermarket.platform.entity.RiskRule;

import java.util.List;
import java.util.Map;

public interface PlatformService {

    // Dashboard
    Map<String, Object> getDashboardStats();
    List<Map<String, Object>> getDailyTrend(int days);
    List<Map<String, Object>> getCategorySales();
    List<Map<String, Object>> getStatusDistribution();
    List<Map<String, Object>> getUserTrend(int days);
    Map<String, Object> getPendingAudit();

    // Merchant Dashboard
    Map<String, Object> getMerchantStats(Long shopId);
    List<Map<String, Object>> getMerchantDailyTrend(Long shopId, int days);
    List<Map<String, Object>> getMerchantStatusDistribution(Long shopId);

    Banner createBanner(Banner banner);
    Banner updateBanner(Banner banner);
    void deleteBanner(Long id);
    Page<Banner> listBanners(int page, int size);
    List<Banner> getActiveBanners(String position);

    AdPosition createPosition(AdPosition position);
    List<AdPosition> listPositions();

    RiskRule createRule(RiskRule rule);
    List<RiskRule> listRules();
    Map<String, Object> evaluateRisk(Long userId, String targetId, Integer riskType);

    Page<RiskLog> listRiskLogs(Integer riskType, int page, int size);

    Map<String, Object> getGmvReport(String startDate, String endDate);
    Map<String, Object> getOrderReport(String startDate, String endDate);
    Map<String, Object> getUserReport(String startDate, String endDate);
}
