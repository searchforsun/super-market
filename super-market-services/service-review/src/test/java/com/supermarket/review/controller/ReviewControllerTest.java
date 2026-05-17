package com.supermarket.review.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supermarket.common.web.handler.GlobalExceptionHandler;
import com.supermarket.review.entity.Review;
import com.supermarket.review.service.ReviewService;
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

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {ReviewController.class, ReviewControllerTest.TestConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReviewService reviewService;

    @Configuration
    @EnableAutoConfiguration
    @ComponentScan(basePackageClasses = ReviewController.class)
    @Import(GlobalExceptionHandler.class)
    static class TestConfig {
    }

    @Test
    void shouldCreateReviewWhenValidBody() throws Exception {
        Review review = new Review();
        review.setId(1L);
        review.setUserId(100L);
        review.setSpuId(200L);
        review.setRating(5);
        review.setContent("Great product!");

        when(reviewService.create(any(Review.class))).thenReturn(review);

        String json = """
                {"userId":100,"spuId":200,"skuId":300,"orderNo":"ORD20240001","rating":5,"content":"Great product!"}
                """;

        mockMvc.perform(post("/api/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.rating").value(5))
                .andExpect(jsonPath("$.data.content").value("Great product!"));
    }

    @Test
    void shouldAppendReviewWhenValidParams() throws Exception {
        Review review = new Review();
        review.setId(1L);
        review.setContent("Original + appended");

        when(reviewService.append(1L, 100L, "追加评价内容", null)).thenReturn(review);

        mockMvc.perform(put("/api/review/1/append")
                        .param("userId", "100")
                        .param("content", "追加评价内容"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.content").value("Original + appended"));
    }

    @Test
    void shouldReplyWhenValidContent() throws Exception {
        doNothing().when(reviewService).reply(1L, "感谢您的评价");

        mockMvc.perform(put("/api/review/1/reply")
                        .param("content", "感谢您的评价"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"));
    }

    @Test
    void shouldReturnPageWhenListBySpu() throws Exception {
        Review review = new Review();
        review.setId(1L);
        review.setContent("Good");
        review.setRating(5);
        Page<Review> page = new Page<>(1, 10);
        page.setRecords(List.of(review));
        page.setTotal(1);

        when(reviewService.listBySpu(eq(200L), any(), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/api/review/list/spu/200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records[0].content").value("Good"));
    }

    @Test
    void shouldReturnPageWhenListByUser() throws Exception {
        Review review = new Review();
        review.setId(1L);
        review.setContent("My review");
        Page<Review> page = new Page<>(1, 10);
        page.setRecords(List.of(review));
        page.setTotal(1);

        when(reviewService.listByUser(eq(100L), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/api/review/list/user/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records[0].content").value("My review"));
    }

    @Test
    void shouldReturnPageWhenListAll() throws Exception {
        Review review = new Review();
        review.setId(1L);
        review.setRating(4);
        Page<Review> page = new Page<>(1, 10);
        page.setRecords(List.of(review));
        page.setTotal(1);

        when(reviewService.listAll(any(), any(), any(), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/api/review/page")
                        .param("minRating", "3")
                        .param("maxRating", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records[0].rating").value(4));
    }

    @Test
    void shouldReturnRatingSummaryWhenSpuExists() throws Exception {
        Map<Integer, Long> distribution = Map.of(5, 10L, 4, 5L);
        when(reviewService.getRatingDistribution(200L)).thenReturn(distribution);
        when(reviewService.getAvgRating(200L)).thenReturn(4.5);

        mockMvc.perform(get("/api/review/rating/200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.avgRating").value(4.5))
                .andExpect(jsonPath("$.data.distribution.5").value(10));
    }
}
