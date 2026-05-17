package com.supermarket.coupon.integration;

import com.supermarket.coupon.CouponApplication;
import com.supermarket.common.dubbo.api.coupon.CouponDubboService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * REAL HTTP integration tests for Coupon domain.
 * Uses TestRestTemplate (RANDOM_PORT) + real MySQL.
 * RedissonClient is mocked since distributed lock infrastructure may not be available.
 */
@SpringBootTest(classes = CouponApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
@DisplayName("Coupon HTTP Integration Test")
class CouponIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private RedissonClient redissonClient;

    @Autowired
    private CouponDubboService couponDubboService;

    @Test
    @DisplayName("Create template -> distribute -> user claim -> list my coupons -> use coupon -> try reuse (fail)")
    void shouldCompleteCouponFlow() throws InterruptedException {
        // Mock Redisson distributed lock for the claim() operation
        RLock lock = mock(RLock.class);
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        long uniqueId = System.currentTimeMillis();
        Long userId = 100000L + uniqueId % 100000;

        // ===============================================================
        // 1. Create a coupon template
        // ===============================================================
        String templateJson = String.format("""
                {
                    "name": "满100减20-%d",
                    "type": 1,
                    "discountValue": 20.00,
                    "minAmount": 100.00,
                    "totalStock": 100,
                    "remainingStock": 100,
                    "perUserLimit": 1,
                    "validDays": 30,
                    "status": 1
                }
                """, uniqueId);

        ResponseEntity<Map> createTplResp = restTemplate.postForEntity(
                "/api/coupon/admin/template",
                createJsonEntity(templateJson),
                Map.class);
        assertThat(createTplResp.getBody().get("code")).isEqualTo(0);
        Map tplData = (Map) createTplResp.getBody().get("data");
        assertThat(tplData.get("name")).isNotNull();
        Long templateId = ((Number) tplData.get("id")).longValue();
        assertThat(templateId).isNotNull();

        // ===============================================================
        // 2. Distribute coupons to users (admin distribution)
        //    Use a different user set than the claim user below.
        // ===============================================================
        Long distUserA = userId;
        Long distUserB = userId + 1;
        Long distUserC = userId + 2;
        List<Long> userIds = List.of(distUserA, distUserB, distUserC);
        HttpEntity<List<Long>> distributeEntity = new HttpEntity<>(userIds, createJsonHeaders());

        ResponseEntity<Map> distributeResp = restTemplate.exchange(
                "/api/coupon/admin/distribute?templateId={templateId}",
                HttpMethod.POST, distributeEntity, Map.class, templateId);
        assertThat(distributeResp.getBody().get("code")).isEqualTo(0);
        Map batchData = (Map) distributeResp.getBody().get("data");
        assertThat(batchData.get("batchName")).isNotNull();
        Long batchId = ((Number) batchData.get("id")).longValue();
        assertThat(batchId).isNotNull();

        // ===============================================================
        // 3. User claims a coupon (self-service claim)
        //    Use a user NOT in the distribution list, otherwise perUserLimit=1 prevents claiming.
        // ===============================================================
        Long claimUserId = userId + 100;
        ResponseEntity<Map> claimResp = restTemplate.postForEntity(
                "/api/coupon/claim?userId={userId}&templateId={templateId}",
                null, Map.class, claimUserId, templateId);
        assertThat(claimResp.getBody().get("code")).isEqualTo(0);
        Map claimData = (Map) claimResp.getBody().get("data");
        assertThat(((Number) claimData.get("userId")).longValue()).isEqualTo(claimUserId);
        assertThat(((Number) claimData.get("templateId")).longValue()).isEqualTo(templateId);
        assertThat(claimData.get("status")).isEqualTo(1); // 1 = unused
        Long couponId = ((Number) claimData.get("id")).longValue();
        assertThat(couponId).isNotNull();

        // ===============================================================
        // 4. List my coupons (for a distributed user)
        // ===============================================================
        ResponseEntity<Map> myCouponsResp = restTemplate.getForEntity(
                "/api/coupon/my?userId={userId}&page={page}&size={size}",
                Map.class, distUserA, 1, 10);
        assertThat(myCouponsResp.getBody().get("code")).isEqualTo(0);
        Map myData = (Map) myCouponsResp.getBody().get("data");
        List<Map> records = (List<Map>) myData.get("records");
        assertThat(records).isNotEmpty();

        // Verify distributed coupons also appear for another user
        ResponseEntity<Map> anotherUserResp = restTemplate.getForEntity(
                "/api/coupon/my?userId={userId}&page={page}&size={size}",
                Map.class, distUserB, 1, 10);
        assertThat(anotherUserResp.getBody().get("code")).isEqualTo(0);
        Map anotherData = (Map) anotherUserResp.getBody().get("data");
        assertThat(((List) anotherData.get("records"))).isNotEmpty();

        // ===============================================================
        // 5. Use the coupon (via service call — no HTTP endpoint for useCoupon)
        // ===============================================================
        String orderNo = "ORDER_TEST_" + uniqueId;
        boolean used = couponDubboService.tryUseCoupon(claimUserId, couponId, orderNo);
        assertThat(used).isTrue();

        // Verify status changed to used (2) via API
        ResponseEntity<Map> usedListResp = restTemplate.getForEntity(
                "/api/coupon/my?userId={userId}&page={page}&size={size}",
                Map.class, claimUserId, 1, 10);
        Map usedData = (Map) usedListResp.getBody().get("data");
        Map usedRecord = ((List<Map>) usedData.get("records")).stream()
                .filter(r -> ((Number) r.get("id")).longValue() == couponId)
                .findFirst().orElse(null);
        assertThat(usedRecord).isNotNull();
        assertThat(usedRecord.get("status")).isEqualTo(2); // 2 = used
        assertThat(usedRecord.get("orderNo")).isEqualTo(orderNo);

        // ===============================================================
        // 6. (Negative) Try to reuse the same coupon — should fail
        // ===============================================================
        boolean reused = couponDubboService.tryUseCoupon(claimUserId, couponId, "ORDER_TEST_002");
        assertThat(reused).isFalse();

        // ===============================================================
        // 7. (Negative) Claim same template again (perUserLimit=1) — should fail
        // ===============================================================
        ResponseEntity<Map> claimAgainResp = restTemplate.postForEntity(
                "/api/coupon/claim?userId={userId}&templateId={templateId}",
                null, Map.class, claimUserId, templateId);
        assertThat(claimAgainResp.getBody().get("code")).isEqualTo(40003);
        assertThat(claimAgainResp.getBody().get("message")).isNotNull();
    }

    // ---- helpers ----

    private HttpEntity<String> createJsonEntity(String json) {
        return new HttpEntity<>(json, createJsonHeaders());
    }

    private HttpHeaders createJsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
