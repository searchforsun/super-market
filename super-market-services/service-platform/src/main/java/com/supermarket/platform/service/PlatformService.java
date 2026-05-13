package com.supermarket.platform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supermarket.platform.entity.AdPosition;
import com.supermarket.platform.entity.Banner;
import com.supermarket.platform.entity.RiskLog;
import com.supermarket.platform.entity.RiskRule;

import java.util.List;
import java.util.Map;

public interface PlatformService {

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
}
