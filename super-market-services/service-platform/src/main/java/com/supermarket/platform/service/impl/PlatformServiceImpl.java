package com.supermarket.platform.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supermarket.common.core.exception.BizException;
import com.supermarket.common.core.result.ResultCode;
import com.supermarket.common.dubbo.api.order.OrderDubboService;
import com.supermarket.common.dubbo.api.user.UserDubboService;
import com.supermarket.platform.entity.*;
import com.supermarket.platform.mapper.*;
import com.supermarket.platform.service.PlatformService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformServiceImpl implements PlatformService {

    private final BannerMapper bannerMapper;
    private final AdPositionMapper positionMapper;
    private final RiskRuleMapper ruleMapper;
    private final RiskLogMapper logMapper;

    @DubboReference(check = false)
    private OrderDubboService orderDubboService;

    @DubboReference(check = false)
    private UserDubboService userDubboService;

    @Override
    public Banner createBanner(Banner banner) {
        bannerMapper.insert(banner);
        return banner;
    }

    @Override
    public Banner updateBanner(Banner banner) {
        Banner exist = bannerMapper.selectById(banner.getId());
        if (exist == null) throw new BizException(ResultCode.BANNER_NOT_FOUND);
        bannerMapper.updateById(banner);
        return banner;
    }

    @Override
    public void deleteBanner(Long id) {
        bannerMapper.deleteById(id);
    }

    @Override
    public Page<Banner> listBanners(int page, int size) {
        return bannerMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Banner>().orderByDesc(Banner::getSortOrder));
    }

    @Override
    public List<Banner> getActiveBanners(String position) {
        LocalDateTime now = LocalDateTime.now();
        return bannerMapper.selectList(new LambdaQueryWrapper<Banner>()
                .eq(Banner::getPosition, position != null ? position : "HOME_TOP")
                .eq(Banner::getStatus, 1)
                .le(Banner::getStartTime, now)
                .ge(Banner::getEndTime, now)
                .orderByAsc(Banner::getSortOrder));
    }

    @Override
    public AdPosition createPosition(AdPosition position) {
        positionMapper.insert(position);
        return position;
    }

    @Override
    public List<AdPosition> listPositions() {
        return positionMapper.selectList(null);
    }

    @Override
    public RiskRule createRule(RiskRule rule) {
        ruleMapper.insert(rule);
        return rule;
    }

    @Override
    public List<RiskRule> listRules() {
        return ruleMapper.selectList(new LambdaQueryWrapper<RiskRule>().eq(RiskRule::getStatus, 1));
    }

    @Override
    public Map<String, Object> evaluateRisk(Long userId, String targetId, Integer riskType) {
        List<RiskRule> rules = listRules();
        int totalScore = 0;
        int action = 0;

        for (RiskRule rule : rules) {
            if (!rule.getType().equals(riskType)) continue;
            Map<String, Object> config = JSONUtil.toBean(rule.getConfig(), Map.class);
            int threshold = ((Number) config.getOrDefault("threshold", 100)).intValue();
            int score = ((Number) config.getOrDefault("score", 50)).intValue();

            // Simplified: random evaluation for demo
            // In production, query actual data (order count, refund rate, etc.)
            totalScore += score;

            RiskLog riskLog = new RiskLog();
            riskLog.setRuleId(rule.getId());
            riskLog.setUserId(userId);
            riskLog.setTargetId(targetId);
            riskLog.setRiskType(riskType);
            riskLog.setRiskScore(score);
            riskLog.setDetail("{\"rule\":\"" + rule.getName() + "\",\"score\":" + score + "}");
            riskLog.setAction(totalScore >= 120 ? 2 : totalScore >= 80 ? 1 : 0);
            logMapper.insert(riskLog);

            if (totalScore >= 120) action = 2;
            else if (totalScore >= 80) action = 1;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("riskType", riskType);
        result.put("totalScore", totalScore);
        result.put("action", action);
        result.put("actionDesc", action == 2 ? "拦截" : action == 1 ? "告警" : "通过");
        return result;
    }

    @Override
    public Page<RiskLog> listRiskLogs(Integer riskType, int page, int size) {
        LambdaQueryWrapper<RiskLog> wrapper = new LambdaQueryWrapper<RiskLog>()
                .orderByDesc(RiskLog::getCreatedAt);
        if (riskType != null) wrapper.eq(RiskLog::getRiskType, riskType);
        return logMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        try {
            Map<String, Object> orderStats = orderDubboService.getTodayStats();
            stats.put("todayGmv", orderStats.getOrDefault("gmv", 0));
            stats.put("todayOrderCount", orderStats.getOrDefault("orderCount", 0));
        } catch (Exception e) {
            log.warn("Failed to fetch order stats", e);
            stats.put("todayGmv", 0);
            stats.put("todayOrderCount", 0);
        }
        try {
            stats.put("newUsers", userDubboService.countTodayNewUsers());
        } catch (Exception e) {
            log.warn("Failed to fetch user stats", e);
            stats.put("newUsers", 0);
        }
        // pending merchants count from platform's own DB
        stats.put("pendingMerchants", 0);
        return stats;
    }

    @Override
    public List<Map<String, Object>> getDailyTrend(int days) {
        try {
            return orderDubboService.getDailyTrend(days);
        } catch (Exception e) {
            log.warn("Failed to fetch daily trend", e);
            return List.of();
        }
    }

    @Override
    public List<Map<String, Object>> getCategorySales() {
        try {
            return orderDubboService.getCategorySales();
        } catch (Exception e) {
            log.warn("Failed to fetch category sales", e);
            return List.of();
        }
    }

    @Override
    public List<Map<String, Object>> getStatusDistribution() {
        try {
            return orderDubboService.getOrderStatusDistribution();
        } catch (Exception e) {
            log.warn("Failed to fetch status distribution", e);
            return List.of();
        }
    }

    @Override
    public List<Map<String, Object>> getUserTrend(int days) {
        try {
            return userDubboService.getDailyNewUsers(days);
        } catch (Exception e) {
            log.warn("Failed to fetch user trend", e);
            return List.of();
        }
    }

    // -- Merchant Dashboard --

    @Override
    public Map<String, Object> getMerchantStats(Long shopId) {
        Map<String, Object> stats = new HashMap<>();
        try {
            Map<String, Object> orderStats = orderDubboService.getMerchantTodayStats(shopId);
            stats.put("todayOrders", orderStats.getOrDefault("orderCount", 0));
            stats.put("todayGmv", orderStats.getOrDefault("gmv", 0));
        } catch (Exception e) {
            log.warn("Failed to fetch merchant stats", e);
            stats.put("todayOrders", 0);
            stats.put("todayGmv", 0);
        }
        stats.put("pendingShip", 0);
        stats.put("pendingRefund", 0);
        return stats;
    }

    @Override
    public List<Map<String, Object>> getMerchantDailyTrend(Long shopId, int days) {
        try {
            return orderDubboService.getMerchantDailyTrend(shopId, days);
        } catch (Exception e) {
            log.warn("Failed to fetch merchant daily trend", e);
            return List.of();
        }
    }

    @Override
    public List<Map<String, Object>> getMerchantStatusDistribution(Long shopId) {
        try {
            return orderDubboService.getMerchantOrderStatusDistribution(shopId);
        } catch (Exception e) {
            log.warn("Failed to fetch merchant status distribution", e);
            return List.of();
        }
    }

    @Override
    public Map<String, Object> getPendingAudit() {
        Map<String, Object> result = new HashMap<>();
        result.put("pendingMerchants", 0);
        result.put("pendingProducts", 0);
        return result;
    }

    @Override
    public Map<String, Object> getGmvReport(String startDate, String endDate) {
        Map<String, Object> data = new HashMap<>();
        data.put("totalGmv", 0);
        data.put("totalOrders", 0);
        data.put("avgOrderValue", 0.0);
        data.put("gmvGrowth", 0.0);
        data.put("daily", List.of());
        return data;
    }

    @Override
    public Map<String, Object> getOrderReport(String startDate, String endDate) {
        Map<String, Object> data = new HashMap<>();
        data.put("totalOrders", 0);
        data.put("completedOrders", 0);
        data.put("cancelledOrders", 0);
        data.put("refundRate", 0.0);
        data.put("daily", List.of());
        return data;
    }

    @Override
    public Map<String, Object> getUserReport(String startDate, String endDate) {
        Map<String, Object> data = new HashMap<>();
        try {
            List<Map<String, Object>> dailyNew = userDubboService.getDailyNewUsers(30);
            data.put("daily", dailyNew);
        } catch (Exception e) {
            data.put("daily", List.of());
        }
        try {
            data.put("totalUsers", userDubboService.countTotalUsers());
        } catch (Exception e) {
            data.put("totalUsers", 0);
        }
        data.put("totalGmv", 0);
        data.put("totalOrders", 0);
        data.put("avgOrderValue", 0.0);
        data.put("gmvGrowth", 0.0);
        return data;
    }
}
