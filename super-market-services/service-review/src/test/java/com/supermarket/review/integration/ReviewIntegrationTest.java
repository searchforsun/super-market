package com.supermarket.review.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real HTTP integration test for ReviewController.
 * Tests review creation, listing, append, reply, and rating summary via TestRestTemplate against real MySQL.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "dubbo.application.name=test",
        "dubbo.registry.address=N/A",
        "dubbo.protocol.name=tri",
        "dubbo.protocol.port=-1"
})
@ActiveProfiles("test")
@Transactional
@DisplayName("Review Integration Test")
class ReviewIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should complete full review lifecycle: create -> list by SPU -> list by user -> append -> reply -> rating summary")
    void shouldManageReviewLifecycle() throws Exception {
        long timestamp = System.currentTimeMillis();
        Long userId = 100L + timestamp;
        Long spuId = 200L + timestamp;
        Long skuId = 300L + timestamp;
        String orderNo = "ORD_INTEGRATION_" + timestamp;

        // ============================================================
        // Step 1: Create a review
        // ============================================================
        Map<String, Object> reviewReq = new HashMap<>();
        reviewReq.put("userId", userId);
        reviewReq.put("spuId", spuId);
        reviewReq.put("skuId", skuId);
        reviewReq.put("orderNo", orderNo);
        reviewReq.put("rating", 5);
        reviewReq.put("content", "Great product, fast shipping! " + timestamp);
        reviewReq.put("isAnonymous", 0);

        ResponseEntity<String> createResp = restTemplate.postForEntity(
                "/api/review", reviewReq, String.class);
        assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode createRoot = objectMapper.readTree(createResp.getBody());
        assertThat(createRoot.get("code").asInt())/*FIX:code now 0*/ .isEqualTo(0);
        Long reviewId = createRoot.get("data").get("id").asLong();
        assertThat(reviewId).isPositive();
        assertThat(createRoot.get("data").get("rating").asInt()).isEqualTo(5);
        assertThat(createRoot.get("data").get("orderNo").asText()).isEqualTo(orderNo);
        assertThat(createRoot.get("data").get("status").asInt()).isEqualTo(1);

        // ============================================================
        // Step 2: Create a second review with a different rating
        // ============================================================
        Map<String, Object> reviewReq2 = new HashMap<>();
        reviewReq2.put("userId", userId + 1);
        reviewReq2.put("spuId", spuId);
        reviewReq2.put("skuId", skuId);
        reviewReq2.put("orderNo", "ORD2_" + timestamp);
        reviewReq2.put("rating", 3);
        reviewReq2.put("content", "Average product " + timestamp);
        reviewReq2.put("isAnonymous", 1);

        ResponseEntity<String> createResp2 = restTemplate.postForEntity(
                "/api/review", reviewReq2, String.class);
        assertThat(createResp2.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode createRoot2 = objectMapper.readTree(createResp2.getBody());
        assertThat(createRoot2.get("code").asInt())/*FIX:code now 0*/ .isEqualTo(0);
        Long reviewId2 = createRoot2.get("data").get("id").asLong();

        // ============================================================
        // Step 3: List reviews by SPU
        // ============================================================
        ResponseEntity<String> listSpuResp = restTemplate.getForEntity(
                "/api/review/list/spu/{spuId}?page={page}&size={size}",
                String.class, spuId, 1, 10);
        assertThat(listSpuResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode listSpuRoot = objectMapper.readTree(listSpuResp.getBody());
        assertThat(listSpuRoot.get("code").asInt())/*FIX:code now 0*/ .isEqualTo(0);
        assertThat(listSpuRoot.get("data").get("total").asLong()).isEqualTo(2);
        assertThat(listSpuRoot.get("data").get("records").size()).isEqualTo(2);

        // List with rating filter
        ResponseEntity<String> listSpuFilteredResp = restTemplate.getForEntity(
                "/api/review/list/spu/{spuId}?rating={rating}",
                String.class, spuId, 5);
        assertThat(listSpuFilteredResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode listSpuFiltered = objectMapper.readTree(listSpuFilteredResp.getBody());
        assertThat(listSpuFiltered.get("code").asInt())/*FIX:code now 0*/ .isEqualTo(0);
        assertThat(listSpuFiltered.get("data").get("total").asLong()).isEqualTo(1);

        // ============================================================
        // Step 4: List reviews by user
        // ============================================================
        ResponseEntity<String> listUserResp = restTemplate.getForEntity(
                "/api/review/list/user/{userId}?page={page}&size={size}",
                String.class, userId, 1, 10);
        assertThat(listUserResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode listUserRoot = objectMapper.readTree(listUserResp.getBody());
        assertThat(listUserRoot.get("code").asInt())/*FIX:code now 0*/ .isEqualTo(0);
        assertThat(listUserRoot.get("data").get("total").asLong()).isEqualTo(1);
        assertThat(listUserRoot.get("data").get("records").get(0).get("id").asLong())
                .isEqualTo(reviewId);

        // ============================================================
        // Step 5: Append review content
        // ============================================================
        String appendContent = "Additional review after 1 week use " + timestamp;
        ResponseEntity<String> appendResp = restTemplate.exchange(
                "/api/review/{id}/append?userId={userId}&content={content}",
                HttpMethod.PUT, null, String.class,
                reviewId, userId, appendContent);
        assertThat(appendResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode appendRoot = objectMapper.readTree(appendResp.getBody());
        assertThat(appendRoot.get("code").asInt())/*FIX:code now 0*/ .isEqualTo(0);
        String appendedContent = appendRoot.get("data").get("content").asText();
        assertThat(appendedContent).contains("【追评】");
        assertThat(appendedContent).contains(appendContent);

        // ============================================================
        // Step 6: Reply to review (merchant reply)
        // ============================================================
        String replyContent = "Thank you for your review! We appreciate your feedback. " + timestamp;
        ResponseEntity<String> replyResp = restTemplate.exchange(
                "/api/review/{id}/reply?content={content}",
                HttpMethod.PUT, null, String.class,
                reviewId, replyContent);
        assertThat(replyResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode replyRoot = objectMapper.readTree(replyResp.getBody());
        assertThat(replyRoot.get("code").asInt())/*FIX:code now 0*/ .isEqualTo(0);

        // Verify reply persisted
        ResponseEntity<String> verifyReplyResp = restTemplate.getForEntity(
                "/api/review/list/spu/{spuId}?page=1&size=10",
                String.class, spuId);
        JsonNode verifyReply = objectMapper.readTree(verifyReplyResp.getBody());
        JsonNode firstReview = verifyReply.get("data").get("records").get(0);
        // The first review (most recent by createdAt desc) should have reply content
        assertThat(firstReview.get("replyContent").asText()).contains("Thank you");

        // ============================================================
        // Step 7: Get rating summary
        // ============================================================
        ResponseEntity<String> ratingResp = restTemplate.getForEntity(
                "/api/review/rating/{spuId}", String.class, spuId);
        assertThat(ratingResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode ratingRoot = objectMapper.readTree(ratingResp.getBody());
        assertThat(ratingRoot.get("code").asInt())/*FIX:code now 0*/ .isEqualTo(0);

        JsonNode ratingData = ratingRoot.get("data");
        assertThat(ratingData.has("avgRating")).isTrue();
        assertThat(ratingData.has("distribution")).isTrue();

        double avgRating = ratingData.get("avgRating").asDouble();
        // Average of 5 and 3 = 4.0 (approximately)
        assertThat(avgRating).isBetween(3.0, 5.0);

        JsonNode distribution = ratingData.get("distribution");
        assertThat(distribution.get("5").asLong()).isEqualTo(1);
        assertThat(distribution.get("3").asLong()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should reject review with invalid rating")
    void shouldRejectInvalidRating() throws Exception {
        long timestamp = System.currentTimeMillis();

        Map<String, Object> reviewReq = new HashMap<>();
        reviewReq.put("userId", 999L);
        reviewReq.put("spuId", 888L);
        reviewReq.put("orderNo", "ORD_INVALID_" + timestamp);
        reviewReq.put("rating", 6); // Invalid: must be 1-5
        reviewReq.put("content", "Invalid rating test");

        ResponseEntity<String> createResp = restTemplate.postForEntity(
                "/api/review", reviewReq, String.class);
        // REVIEW_RATING_INVALID (80002)
        assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        JsonNode root = objectMapper.readTree(createResp.getBody());
        assertThat(root.get("code").asInt()).isEqualTo(80002);
        assertThat(root.get("message").asText()).contains("评分");
    }

    @Test
    @DisplayName("Should reject append by different user")
    void shouldRejectAppendByDifferentUser() throws Exception {
        long timestamp = System.currentTimeMillis();

        Map<String, Object> reviewReq = new HashMap<>();
        reviewReq.put("userId", 777L);
        reviewReq.put("spuId", 666L);
        reviewReq.put("orderNo", "ORD_APPEND_" + timestamp);
        reviewReq.put("rating", 4);
        reviewReq.put("content", "Original review");

        ResponseEntity<String> createResp = restTemplate.postForEntity(
                "/api/review", reviewReq, String.class);
        JsonNode createRoot = objectMapper.readTree(createResp.getBody());
        Long reviewId = createRoot.get("data").get("id").asLong();

        // Try to append as a different user
        ResponseEntity<String> appendResp = restTemplate.exchange(
                "/api/review/{id}/append?userId={userId}&content={content}",
                HttpMethod.PUT, null, String.class,
                reviewId, 999L, "Unauthorized append");
        // REVIEW_NOT_OWNED (80005)
        assertThat(appendResp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        JsonNode appendRoot = objectMapper.readTree(appendResp.getBody());
        assertThat(appendRoot.get("code").asInt()).isEqualTo(80005);
        assertThat(appendRoot.get("message").asText()).contains("操作自己的评价");
    }
}
