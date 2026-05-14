package com.supermarket.review.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supermarket.common.core.result.R;
import com.supermarket.review.entity.Review;
import com.supermarket.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public R<Review> create(@RequestBody Review review) {
        return R.ok(reviewService.create(review));
    }

    @PutMapping("/{id}/append")
    public R<Review> append(@PathVariable Long id,
                            @RequestParam Long userId,
                            @RequestParam String content,
                            @RequestParam(required = false) String images) {
        return R.ok(reviewService.append(id, userId, content, images));
    }

    @PutMapping("/{id}/reply")
    public R<Void> reply(@PathVariable Long id, @RequestParam String content) {
        reviewService.reply(id, content);
        return R.ok();
    }

    @GetMapping("/list/spu/{spuId}")
    public R<Page<Review>> listBySpu(@PathVariable Long spuId,
                                     @RequestParam(required = false) Integer rating,
                                     @RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "10") int size) {
        return R.ok(reviewService.listBySpu(spuId, rating, page, size));
    }

    @GetMapping("/list/user/{userId}")
    public R<Page<Review>> listByUser(@PathVariable Long userId,
                                      @RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "10") int size) {
        return R.ok(reviewService.listByUser(userId, page, size));
    }

    @GetMapping("/page")
    public R<Page<Review>> page(@RequestParam(required = false) Integer minRating,
                                @RequestParam(required = false) Integer maxRating,
                                @RequestParam(required = false) Long spuId,
                                @RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "10") int size) {
        return R.ok(reviewService.listAll(minRating, maxRating, spuId, page, size));
    }

    @GetMapping("/rating/{spuId}")
    public R<Map<String, Object>> ratingSummary(@PathVariable Long spuId) {
        Map<Integer, Long> dist = reviewService.getRatingDistribution(spuId);
        Double avg = reviewService.getAvgRating(spuId);
        return R.ok(Map.of("avgRating", avg, "distribution", dist));
    }
}
