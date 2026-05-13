package com.supermarket.platform.service;

import com.supermarket.platform.entity.Banner;
import com.supermarket.platform.entity.RiskRule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class PlatformServiceTest {

    @Autowired
    private PlatformService platformService;

    @Test
    void shouldCreateBanner() {
        Banner banner = new Banner();
        banner.setTitle("测试Banner");
        banner.setImageUrl("http://example.com/img.jpg");
        banner.setPosition("HOME_TOP");
        banner.setStatus(1);
        banner.setStartTime(LocalDateTime.now().minusDays(1));
        banner.setEndTime(LocalDateTime.now().plusDays(7));
        Banner saved = platformService.createBanner(banner);
        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void shouldGetActiveBanners() {
        Banner banner = new Banner();
        banner.setTitle("有效Banner");
        banner.setImageUrl("http://example.com/img2.jpg");
        banner.setPosition("HOME_TOP");
        banner.setStatus(1);
        banner.setStartTime(LocalDateTime.now().minusDays(1));
        banner.setEndTime(LocalDateTime.now().plusDays(7));
        platformService.createBanner(banner);

        List<Banner> active = platformService.getActiveBanners("HOME_TOP");
        assertThat(active).hasSize(1);
    }

    @Test
    void shouldCreateRiskRule() {
        RiskRule rule = new RiskRule();
        rule.setName("刷单检测");
        rule.setType(1);
        rule.setConfig("{\"threshold\":100,\"score\":50}");
        RiskRule saved = platformService.createRule(rule);
        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void shouldEvaluateRisk() {
        RiskRule rule = new RiskRule();
        rule.setName("恶意退款检测");
        rule.setType(2);
        rule.setConfig("{\"threshold\":50,\"score\":70}");
        platformService.createRule(rule);

        Map<String, Object> result = platformService.evaluateRisk(100L, "ORD001", 2);
        assertThat(result).containsKeys("totalScore", "action", "actionDesc");
    }

    @Test
    void shouldListRiskLogs() {
        var page = platformService.listRiskLogs(null, 1, 10);
        assertThat(page).isNotNull();
    }
}
