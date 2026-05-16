package com.supermarket.platform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supermarket.common.web.handler.GlobalExceptionHandler;
import com.supermarket.platform.entity.AdPosition;
import com.supermarket.platform.entity.Banner;
import com.supermarket.platform.entity.RiskLog;
import com.supermarket.platform.entity.RiskRule;
import com.supermarket.platform.service.PlatformService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PlatformController.class)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
class PlatformControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PlatformService platformService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldReturnActiveBannersWhenPositionProvided() throws Exception {
        // Arrange
        Banner banner = new Banner();
        banner.setId(1L);
        banner.setTitle("首页Banner");
        banner.setPosition("HOME_TOP");
        banner.setStatus(1);

        when(platformService.getActiveBanners("HOME_TOP")).thenReturn(List.of(banner));

        // Act & Assert
        mockMvc.perform(get("/api/platform/banners")
                .param("position", "HOME_TOP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].title").value("首页Banner"));

        verify(platformService).getActiveBanners("HOME_TOP");
    }

    @Test
    void shouldCreateBannerWhenRequestIsValid() throws Exception {
        // Arrange
        Banner banner = new Banner();
        banner.setTitle("新品推荐");
        banner.setImageUrl("https://example.com/banner.jpg");
        banner.setPosition("HOME_TOP");

        Banner created = new Banner();
        created.setId(1L);
        created.setTitle("新品推荐");
        created.setStatus(1);

        when(platformService.createBanner(any(Banner.class))).thenReturn(created);

        // Act & Assert
        mockMvc.perform(post("/api/platform/admin/banner")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(banner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.title").value("新品推荐"));

        verify(platformService).createBanner(any(Banner.class));
    }

    @Test
    void shouldUpdateBannerWhenRequestIsValid() throws Exception {
        // Arrange
        Banner banner = new Banner();
        banner.setTitle("更新后的Banner");

        Banner updated = new Banner();
        updated.setId(1L);
        updated.setTitle("更新后的Banner");

        when(platformService.updateBanner(any(Banner.class))).thenReturn(updated);

        // Act & Assert
        mockMvc.perform(put("/api/platform/admin/banner/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(banner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").value("更新后的Banner"));

        verify(platformService).updateBanner(any(Banner.class));
    }

    @Test
    void shouldDeleteBannerWhenIdProvided() throws Exception {
        // Arrange
        doNothing().when(platformService).deleteBanner(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/platform/admin/banner/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"));

        verify(platformService).deleteBanner(1L);
    }

    @Test
    void shouldReturnListBannersWhenCalled() throws Exception {
        // Arrange
        Page<Banner> page = new Page<>(1, 20);
        Banner banner = new Banner();
        banner.setId(1L);
        banner.setTitle("测试Banner");
        page.setRecords(List.of(banner));
        page.setTotal(1);

        when(platformService.listBanners(1, 20)).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/platform/admin/banners")
                .param("page", "1")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].title").value("测试Banner"))
                .andExpect(jsonPath("$.data.total").value(1));

        verify(platformService).listBanners(1, 20);
    }

    @Test
    void shouldCreatePositionWhenRequestIsValid() throws Exception {
        // Arrange
        AdPosition position = new AdPosition();
        position.setCode("HOME_BANNER");
        position.setName("首页轮播");

        AdPosition created = new AdPosition();
        created.setId(1L);
        created.setCode("HOME_BANNER");

        when(platformService.createPosition(any(AdPosition.class))).thenReturn(created);

        // Act & Assert
        mockMvc.perform(post("/api/platform/admin/position")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(position)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.code").value("HOME_BANNER"));

        verify(platformService).createPosition(any(AdPosition.class));
    }

    @Test
    void shouldReturnListPositionsWhenCalled() throws Exception {
        // Arrange
        AdPosition position = new AdPosition();
        position.setId(1L);
        position.setCode("HOME_BANNER");

        when(platformService.listPositions()).thenReturn(List.of(position));

        // Act & Assert
        mockMvc.perform(get("/api/platform/admin/positions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].code").value("HOME_BANNER"));

        verify(platformService).listPositions();
    }

    @Test
    void shouldCreateRuleWhenRequestIsValid() throws Exception {
        // Arrange
        RiskRule rule = new RiskRule();
        rule.setName("登录风控");
        rule.setType(1);

        RiskRule created = new RiskRule();
        created.setId(1L);
        created.setName("登录风控");
        created.setStatus(1);

        when(platformService.createRule(any(RiskRule.class))).thenReturn(created);

        // Act & Assert
        mockMvc.perform(post("/api/platform/admin/risk/rule")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rule)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("登录风控"));

        verify(platformService).createRule(any(RiskRule.class));
    }

    @Test
    void shouldReturnListRulesWhenCalled() throws Exception {
        // Arrange
        RiskRule rule = new RiskRule();
        rule.setId(1L);
        rule.setName("登录风控");
        rule.setType(1);

        when(platformService.listRules()).thenReturn(List.of(rule));

        // Act & Assert
        mockMvc.perform(get("/api/platform/admin/risk/rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].name").value("登录风控"));

        verify(platformService).listRules();
    }

    @Test
    void shouldEvaluateRiskWhenParamsProvided() throws Exception {
        // Arrange
        Map<String, Object> result = new HashMap<>();
        result.put("riskScore", 85);
        result.put("action", 1);
        result.put("decision", "REJECT");

        when(platformService.evaluateRisk(1L, "order_100", 1)).thenReturn(result);

        // Act & Assert
        mockMvc.perform(post("/api/platform/admin/risk/evaluate")
                .param("userId", "1")
                .param("targetId", "order_100")
                .param("riskType", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.riskScore").value(85))
                .andExpect(jsonPath("$.data.decision").value("REJECT"));

        verify(platformService).evaluateRisk(1L, "order_100", 1);
    }

    @Test
    void shouldReturnRiskLogsWhenCalled() throws Exception {
        // Arrange
        Page<RiskLog> page = new Page<>(1, 20);
        RiskLog log = new RiskLog();
        log.setId(1L);
        log.setUserId(1L);
        log.setRiskType(1);
        log.setRiskScore(80);
        page.setRecords(List.of(log));
        page.setTotal(1);

        when(platformService.listRiskLogs(1, 1, 20)).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/platform/admin/risk/logs")
                .param("riskType", "1")
                .param("page", "1")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].userId").value(1L))
                .andExpect(jsonPath("$.data.total").value(1));

        verify(platformService).listRiskLogs(1, 1, 20);
    }
}
