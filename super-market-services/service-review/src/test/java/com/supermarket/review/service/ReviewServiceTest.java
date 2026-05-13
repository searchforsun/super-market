package com.supermarket.review.service;

import com.supermarket.common.core.exception.BizException;
import com.supermarket.review.entity.Review;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class ReviewServiceTest {

    @Autowired
    private ReviewService reviewService;

    @Test
    void shouldCreateReview() {
        Review review = newReview(1L, 100L, "ORD001", 5, "非常好");
        Review saved = reviewService.create(review);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(1);
    }

    @Test
    void shouldRejectInvalidRating() {
        Review review = newReview(1L, 100L, "ORD001", 6, "超出范围");
        assertThatThrownBy(() -> reviewService.create(review))
                .hasMessageContaining("评分必须在1-5之间");
    }

    @Test
    void shouldAppendReview() {
        Review review = reviewService.create(newReview(2L, 101L, "ORD002", 4, "还行"));
        Review appended = reviewService.append(review.getId(), 2L, "用了几天还不错", null);
        assertThat(appended.getContent()).contains("追评");
    }

    @Test
    void shouldNotAppendOthersReview() {
        Review review = reviewService.create(newReview(3L, 102L, "ORD003", 3, "一般"));
        assertThatThrownBy(() -> reviewService.append(review.getId(), 999L, "不能追加别人的", null))
                .isInstanceOf(BizException.class);
    }

    @Test
    void shouldReplyReview() {
        Review review = reviewService.create(newReview(4L, 103L, "ORD004", 5, "完美"));
        reviewService.reply(review.getId(), "感谢您的评价");
        Review replied = reviewService.getById(review.getId());
        assertThat(replied.getReplyContent()).isEqualTo("感谢您的评价");
        assertThat(replied.getReplyAt()).isNotNull();
    }

    @Test
    void shouldGetRatingDistribution() {
        reviewService.create(newReview(5L, 200L, "ORD101", 5, "好1"));
        reviewService.create(newReview(6L, 200L, "ORD102", 5, "好2"));
        reviewService.create(newReview(7L, 200L, "ORD103", 3, "中"));
        reviewService.create(newReview(8L, 200L, "ORD104", 1, "差"));

        Map<Integer, Long> dist = reviewService.getRatingDistribution(200L);
        assertThat(dist.get(5)).isEqualTo(2);
        assertThat(dist.get(3)).isEqualTo(1);
        assertThat(dist.get(1)).isEqualTo(1);
    }

    @Test
    void shouldGetAvgRating() {
        reviewService.create(newReview(9L, 201L, "ORD201", 5, "好"));
        reviewService.create(newReview(10L, 201L, "ORD202", 3, "中"));
        Double avg = reviewService.getAvgRating(201L);
        assertThat(avg).isEqualTo(4.0);
    }

    @Test
    void shouldListBySpuFilterByRating() {
        reviewService.create(newReview(11L, 300L, "ORD301", 5, "好"));
        reviewService.create(newReview(12L, 300L, "ORD302", 5, "很好"));
        reviewService.create(newReview(13L, 300L, "ORD303", 3, "一般"));

        var page = reviewService.listBySpu(300L, 5, 1, 10);
        assertThat(page.getTotal()).isEqualTo(2);
    }

    private Review newReview(Long userId, Long spuId, String orderNo, int rating, String content) {
        Review review = new Review();
        review.setUserId(userId);
        review.setSpuId(spuId);
        review.setSkuId(spuId + 1000);
        review.setOrderNo(orderNo);
        review.setRating(rating);
        review.setContent(content);
        review.setIsAnonymous(0);
        return review;
    }
}
