package com.supermarket.coupon.flow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supermarket.common.core.exception.BizException;
import com.supermarket.coupon.CouponApplication;
import com.supermarket.coupon.entity.UserCoupon;
import com.supermarket.coupon.service.CouponService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Postman-style business flow test for the Coupon domain.
 *
 * Tests the complete coupon lifecycle: template creation → admin distribute →
 * user claim → list my coupons → use coupon → reuse (error).
 *
 * Requires a running MySQL instance (configured via application-test.yml).
 * RedissonClient is mocked (excluded in test profile).
 */
@SpringBootTest(classes = CouponApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("flow-test")
@Transactional
@DisplayName("Coupon Business Flow")
class CouponBusinessFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CouponService couponService;

    @MockBean
    private RedissonClient redissonClient;

    @BeforeEach
    void setUp() throws InterruptedException {
        // Redisson is excluded in application-test.yml; provide a mock lock
        // so claim() can acquire the distributed lock.
        RLock mockLock = mock(RLock.class);
        lenient().when(mockLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        lenient().when(mockLock.isHeldByCurrentThread()).thenReturn(true);
        lenient().when(redissonClient.getLock(anyString())).thenReturn(mockLock);
    }

    @Test
    @DisplayName("Complete coupon flow: template → distribute → claim → list → use → reuse (error)")
    void shouldCompleteCouponFlow() throws Exception {
        // ---------------------------------------------------------------
        // 1. Create coupon template (active, stock 100, perUserLimit 2)
        // ---------------------------------------------------------------
        String templateJson = """
                {
                    "name": "满100减20",
                    "type": 1,
                    "discountValue": 20.00,
                    "minAmount": 100.00,
                    "totalStock": 100,
                    "perUserLimit": 2,
                    "validDays": 30,
                    "status": 1
                }
                """;
        String templateResp = mockMvc.perform(post("/api/coupon/admin/template")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(templateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value("满100减20"))
                .andReturn().getResponse().getContentAsString();

        Long templateId = extractId(templateResp, "data.id");
        assertThat(templateId).isNotNull();

        // ---------------------------------------------------------------
        // 2. Admin distributes coupons to users 101 and 102
        // ---------------------------------------------------------------
        String batchResp = mockMvc.perform(post("/api/coupon/admin/distribute")
                        .param("templateId", String.valueOf(templateId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[101, 102]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.quantity").value(2))
                .andReturn().getResponse().getContentAsString();

        Long batchId = extractId(batchResp, "data.id");
        assertThat(batchId).isNotNull();

        // ---------------------------------------------------------------
        // 3. User 103 claims the coupon independently
        // ---------------------------------------------------------------
        String claimResp = mockMvc.perform(post("/api/coupon/claim")
                        .param("userId", "103")
                        .param("templateId", String.valueOf(templateId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userId").value(103))
                .andReturn().getResponse().getContentAsString();

        Long userCouponId = extractId(claimResp, "data.id");
        String couponCode = objectMapper.readTree(claimResp).path("data").path("couponCode").asText();
        assertThat(userCouponId).isNotNull();
        assertThat(couponCode).isNotBlank();

        // ---------------------------------------------------------------
        // 4. List user 103's coupons → verify the claimed coupon appears
        // ---------------------------------------------------------------
        String myResp = mockMvc.perform(get("/api/coupon/my")
                        .param("userId", "103")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records[0].id").value(userCouponId))
                .andReturn().getResponse().getContentAsString();

        JsonNode myCoupons = objectMapper.readTree(myResp).path("data").path("records");
        assertThat(myCoupons).isNotEmpty();

        // ---------------------------------------------------------------
        // 5. Use the coupon on an order (direct service call, no REST endpoint)
        // ---------------------------------------------------------------
        UserCoupon used = couponService.useCoupon(103L, userCouponId, "TEST_ORDER_001");
        assertThat(used.getStatus()).as("Coupon status should be USED (2)").isEqualTo(2);
        assertThat(used.getOrderNo()).isEqualTo("TEST_ORDER_001");

        // ---------------------------------------------------------------
        // 6. Try to use the already-used coupon → expect BizException
        // ---------------------------------------------------------------
        assertThatThrownBy(() -> couponService.useCoupon(103L, userCouponId, "TEST_ORDER_002"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不可用");

        // Verify the lock was acquired for the claim step
        verify(redissonClient, atLeastOnce()).getLock(contains("coupon:claim"));
    }

    private Long extractId(String json, String path) throws Exception {
        String[] parts = path.split("\\.");
        JsonNode node = objectMapper.readTree(json);
        for (String part : parts) {
            node = node.path(part);
        }
        return node.isMissingNode() ? null : node.asLong();
    }
}
