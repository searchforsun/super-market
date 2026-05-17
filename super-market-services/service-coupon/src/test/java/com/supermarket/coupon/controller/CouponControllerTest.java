package com.supermarket.coupon.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supermarket.common.web.handler.GlobalExceptionHandler;
import com.supermarket.coupon.entity.CouponBatch;
import com.supermarket.coupon.entity.CouponTemplate;
import com.supermarket.coupon.entity.UserCoupon;
import com.supermarket.coupon.service.impl.CouponServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {CouponController.class, CouponControllerTest.TestConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CouponControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Mock the concrete impl class so the mock inherits both CouponService and
     * CouponDubboService interfaces, satisfying Dubbo's service export check.
     */
    @MockBean
    private CouponServiceImpl couponService;

    @Configuration
    @EnableAutoConfiguration
    @ComponentScan(basePackageClasses = CouponController.class)
    @Import(GlobalExceptionHandler.class)
    static class TestConfig {
    }

    @Test
    void shouldCreateTemplateWhenValidBody() throws Exception {
        CouponTemplate template = new CouponTemplate();
        template.setId(1L);
        template.setName("满100减20");
        template.setType(1);
        template.setDiscountValue(new BigDecimal("20"));
        template.setMinAmount(new BigDecimal("100"));
        template.setTotalStock(1000);
        template.setRemainingStock(1000);

        when(couponService.createTemplate(any(CouponTemplate.class))).thenReturn(template);

        String json = """
                {"name":"满100减20","type":1,"discountValue":20,"minAmount":100,"totalStock":1000}
                """;

        mockMvc.perform(post("/api/coupon/admin/template")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value("满100减20"))
                .andExpect(jsonPath("$.data.discountValue").value(20));
    }

    @Test
    void shouldDistributeWhenValidParams() throws Exception {
        CouponBatch batch = new CouponBatch();
        batch.setId(1L);
        batch.setTemplateId(10L);
        batch.setQuantity(2);

        List<Long> userIds = List.of(101L, 102L);

        when(couponService.distribute(eq(10L), anyList())).thenReturn(batch);

        mockMvc.perform(post("/api/coupon/admin/distribute")
                        .param("templateId", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userIds)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.templateId").value(10))
                .andExpect(jsonPath("$.data.quantity").value(2));
    }

    @Test
    void shouldReturnAvailableTemplates() throws Exception {
        CouponTemplate template = new CouponTemplate();
        template.setId(1L);
        template.setName("新人优惠券");
        Page<CouponTemplate> page = new Page<>(1, 20);
        page.setRecords(List.of(template));
        page.setTotal(1);

        when(couponService.listTemplates(anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/api/coupon/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records[0].name").value("新人优惠券"));
    }

    @Test
    void shouldClaimWhenValidParams() throws Exception {
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setId(1L);
        userCoupon.setUserId(101L);
        userCoupon.setTemplateId(10L);
        userCoupon.setStatus(0);

        when(couponService.claim(101L, 10L)).thenReturn(userCoupon);

        mockMvc.perform(post("/api/coupon/claim")
                        .param("userId", "101")
                        .param("templateId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userId").value(101))
                .andExpect(jsonPath("$.data.templateId").value(10));
    }

    @Test
    void shouldReturnMyCouponsWhenUserIdGiven() throws Exception {
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setId(1L);
        userCoupon.setUserId(101L);
        userCoupon.setStatus(0);
        Page<UserCoupon> page = new Page<>(1, 10);
        page.setRecords(List.of(userCoupon));
        page.setTotal(1);

        when(couponService.listUserCoupons(eq(101L), any(), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/api/coupon/my")
                        .param("userId", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records[0].userId").value(101))
                .andExpect(jsonPath("$.data.records[0].status").value(0));
    }

    @Test
    void shouldReturnAvailableListWhenUserIdGiven() throws Exception {
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setId(1L);
        userCoupon.setUserId(101L);
        userCoupon.setStatus(0);

        when(couponService.listAvailableCoupons(101L)).thenReturn(List.of(userCoupon));

        mockMvc.perform(get("/api/coupon/available/list")
                        .param("userId", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].userId").value(101));
    }
}
