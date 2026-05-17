package com.supermarket.review.flow;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supermarket.common.core.exception.BizException;
import com.supermarket.common.core.result.ResultCode;
import com.supermarket.review.entity.Review;
import com.supermarket.review.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Postman-style business flow test for the Review domain.
 * <p>
 * Chains API calls to simulate: Create Review -> List by SPU -> List by User ->
 * Append -> Reply -> Rating Summary -> Filtered page query.
 * <p>
 * Uses @SpringBootTest for full context but mocks ReviewService and RedissonClient
 * so no database or Redis middleware is required.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration," +
                "org.apache.dubbo.spring.boot.autoconfigure.DubboAutoConfiguration," +
                "com.alibaba.cloud.nacos.NacosDiscoveryAutoConfiguration," +
                "com.alibaba.cloud.nacos.NacosConfigAutoConfiguration",
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.nacos.config.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Review Business Flow Test")
class ReviewBusinessFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReviewService reviewService;

    @MockBean
    private RedissonClient redissonClient;

    private Review createReview(Long id, Long userId, Long spuId, Long skuId,
                                String orderNo, Integer rating, String content, Integer status) {
        Review review = new Review();
        review.setId(id);
        review.setUserId(userId);
        review.setSpuId(spuId);
        review.setSkuId(skuId);
        review.setOrderNo(orderNo);
        review.setRating(rating);
        review.setContent(content);
        review.setStatus(status);
        review.setCreatedAt(LocalDateTime.now());
        return review;
    }

    @BeforeEach
    void setUp() {
        // ReviewService does not use Redisson — mock is just to prevent Redis connection attempt.
    }

    // ---------------------------------------------------------------
    // Flow 1: Complete Review Lifecycle
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Complete review lifecycle: create -> list by spu/user -> append -> reply -> rating")
    void shouldCompleteFullReviewLifecycleFlow() throws Exception {
        Long reviewId = 100L;
        Long spuId = 200L;
        Long userId = 1001L;
        Long skuId = 300L;
        String orderNo = "ORD202605160001";

        Review createdReview = createReview(reviewId, userId, spuId, skuId, orderNo, 5,
                "手机质量非常好，拍照清晰，运行流畅！", 1);

        Review appendedReview = createReview(reviewId, userId, spuId, skuId, orderNo, 5,
                "手机质量非常好，拍照清晰，运行流畅！\n【追评】使用一周后，续航表现优秀", 1);

        Review repliedReview = createReview(reviewId, userId, spuId, skuId, orderNo, 5,
                "手机质量非常好，拍照清晰，运行流畅！", 1);
        repliedReview.setReplyContent("感谢您的评价，我们会继续努力！");

        Page<Review> singleReviewPage = new Page<>(1, 10);
        singleReviewPage.setRecords(List.of(createdReview));
        singleReviewPage.setTotal(1);

        // --- Mock setup ---

        // Step 1: create review
        when(reviewService.create(any(Review.class))).thenReturn(createdReview);

        // Step 2: list by spu
        when(reviewService.listBySpu(eq(spuId), isNull(), anyInt(), anyInt())).thenReturn(singleReviewPage);

        // Step 3: list by user
        when(reviewService.listByUser(eq(userId), anyInt(), anyInt())).thenReturn(singleReviewPage);

        // Step 4: append
        when(reviewService.append(eq(reviewId), eq(userId), eq("使用一周后，续航表现优秀"), isNull()))
                .thenReturn(appendedReview);

        // Step 5: reply
        doNothing().when(reviewService).reply(eq(reviewId), eq("感谢您的评价，我们会继续努力！"));

        // Step 6: rating summary
        when(reviewService.getRatingDistribution(spuId)).thenReturn(Map.of(1, 0L, 2, 0L, 3, 0L, 4, 0L, 5, 1L));
        when(reviewService.getAvgRating(spuId)).thenReturn(5.0);

        // Step 7: filtered page query
        Page<Review> filteredPage = new Page<>(1, 10);
        filteredPage.setRecords(List.of(createdReview));
        filteredPage.setTotal(1);
        when(reviewService.listAll(eq(4), eq(5), eq(spuId), anyInt(), anyInt())).thenReturn(filteredPage);

        // ============================================================
        // Step 1: Create a review
        // ============================================================
        String reviewJson = """
                {
                    "userId": %d,
                    "spuId": %d,
                    "skuId": %d,
                    "orderNo": "%s",
                    "rating": 5,
                    "content": "手机质量非常好，拍照清晰，运行流畅！",
                    "isAnonymous": 0
                }
                """.formatted(userId, spuId, skuId, orderNo);

        String createResponse = mockMvc.perform(post("/api/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.rating").value(5))
                .andExpect(jsonPath("$.data.content").value("手机质量非常好，拍照清晰，运行流畅！"))
                .andExpect(jsonPath("$.data.userId").value(userId))
                .andReturn().getResponse().getContentAsString();

        // ============================================================
        // Step 2: List by SPU
        // ============================================================
        mockMvc.perform(get("/api/review/list/spu/{spuId}", spuId)
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].content").value("手机质量非常好，拍照清晰，运行流畅！"))
                .andExpect(jsonPath("$.data.records[0].rating").value(5));

        // ============================================================
        // Step 3: List by user
        // ============================================================
        mockMvc.perform(get("/api/review/list/user/{userId}", userId)
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].spuId").value(spuId));

        // ============================================================
        // Step 4: Append to review
        // ============================================================
        mockMvc.perform(put("/api/review/{reviewId}/append", reviewId)
                        .param("userId", String.valueOf(userId))
                        .param("content", "使用一周后，续航表现优秀"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.content").value(
                        org.hamcrest.Matchers.containsString("续航表现优秀")));

        // ============================================================
        // Step 5: Merchant reply
        // ============================================================
        mockMvc.perform(put("/api/review/{reviewId}/reply", reviewId)
                        .param("content", "感谢您的评价，我们会继续努力！"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"));

        // ============================================================
        // Step 6: Rating summary
        // ============================================================
        mockMvc.perform(get("/api/review/rating/{spuId}", spuId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.avgRating").value(5.0))
                .andExpect(jsonPath("$.data.distribution.5").value(1));

        // ============================================================
        // Step 7: Filtered page query
        // ============================================================
        mockMvc.perform(get("/api/review/page")
                        .param("minRating", "4")
                        .param("maxRating", "5")
                        .param("spuId", String.valueOf(spuId))
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records.length()").value(1));
    }

    // ---------------------------------------------------------------
    // Flow 2: Multi-review Rating Aggregation
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Create multiple reviews and verify aggregated rating")
    void shouldAggregateRatingsAcrossMultipleReviews() throws Exception {
        Long spuId = 201L;
        Review review5star = createReview(101L, 1002L, spuId, 301L, "ORD202605160002", 5, "Excellent!", 1);
        Review review4star = createReview(102L, 1003L, spuId, 301L, "ORD202605160003", 4, "Good", 1);

        when(reviewService.create(any(Review.class)))
                .thenReturn(review5star)   // first call
                .thenReturn(review4star);  // second call

        when(reviewService.getRatingDistribution(spuId))
                .thenReturn(Map.of(1, 0L, 2, 0L, 3, 0L, 4, 1L, 5, 1L));
        when(reviewService.getAvgRating(spuId)).thenReturn(4.5);

        // First review
        String review1Json = """
                {"userId": 1002, "spuId": %d, "skuId": 301, "orderNo": "ORD202605160002", "rating": 5, "content": "Excellent!"}
                """.formatted(spuId);

        mockMvc.perform(post("/api/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(review1Json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // Second review
        String review2Json = """
                {"userId": 1003, "spuId": %d, "skuId": 301, "orderNo": "ORD202605160003", "rating": 4, "content": "Good"}
                """.formatted(spuId);

        mockMvc.perform(post("/api/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(review2Json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // Verify rating summary
        mockMvc.perform(get("/api/review/rating/{spuId}", spuId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.avgRating").value(4.5))
                .andExpect(jsonPath("$.data.distribution.5").value(1))
                .andExpect(jsonPath("$.data.distribution.4").value(1));
    }

    // ---------------------------------------------------------------
    // Flow 3: Validation Error Flows
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Create review with invalid rating returns 400")
    void shouldFailWhenRatingOutOfRange() throws Exception {
        when(reviewService.create(any(Review.class)))
                .thenThrow(new BizException(ResultCode.REVIEW_RATING_INVALID));

        String invalidReviewJson = """
                {"userId": 1001, "spuId": 2001, "skuId": 3001, "orderNo": "ORD202605160004", "rating": 6, "content": "Invalid rating"}
                """;

        mockMvc.perform(post("/api/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidReviewJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(80002));
    }

    @Test
    @DisplayName("Create review without orderNo returns 400")
    void shouldFailWhenOrderNoMissing() throws Exception {
        when(reviewService.create(any(Review.class)))
                .thenThrow(new BizException(ResultCode.REVIEW_ORDER_REQUIRED));

        String missingOrderJson = """
                {"userId": 1001, "spuId": 2001, "skuId": 3001, "rating": 5, "content": "No order no"}
                """;

        mockMvc.perform(post("/api/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(missingOrderJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(80003));
    }
}
