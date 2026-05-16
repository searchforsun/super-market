package com.supermarket.review.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supermarket.common.core.result.R;
import com.supermarket.review.entity.Review;
import com.supermarket.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
@Tag(name = "评价服务", description = "商品评价、追评、回复、评分统计接口")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @Operation(summary = "创建评价")
    public R<Review> create(@Parameter(description = "评价信息") @RequestBody Review review) {
        return R.ok(reviewService.create(review));
    }

    @PutMapping("/{id}/append")
    @Operation(summary = "追加评价")
    public R<Review> append(@Parameter(description = "评价ID") @PathVariable Long id,
                            @Parameter(description = "用户ID") @RequestParam Long userId,
                            @Parameter(description = "追评内容") @RequestParam String content,
                            @Parameter(description = "追评图片") @RequestParam(required = false) String images) {
        return R.ok(reviewService.append(id, userId, content, images));
    }

    @PutMapping("/{id}/reply")
    @Operation(summary = "商家回复评价")
    public R<Void> reply(@Parameter(description = "评价ID") @PathVariable Long id,
                         @Parameter(description = "回复内容") @RequestParam String content) {
        reviewService.reply(id, content);
        return R.ok();
    }

    @GetMapping("/list/spu/{spuId}")
    @Operation(summary = "根据商品SPU查询评价列表")
    public R<Page<Review>> listBySpu(@Parameter(description = "SPU ID") @PathVariable Long spuId,
                                     @Parameter(description = "评分筛选") @RequestParam(required = false) Integer rating,
                                     @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
                                     @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size) {
        return R.ok(reviewService.listBySpu(spuId, rating, page, size));
    }

    @GetMapping("/list/user/{userId}")
    @Operation(summary = "根据用户查询评价列表")
    public R<Page<Review>> listByUser(@Parameter(description = "用户ID") @PathVariable Long userId,
                                      @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
                                      @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size) {
        return R.ok(reviewService.listByUser(userId, page, size));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询评价")
    public R<Page<Review>> page(@Parameter(description = "最低评分") @RequestParam(required = false) Integer minRating,
                                @Parameter(description = "最高评分") @RequestParam(required = false) Integer maxRating,
                                @Parameter(description = "SPU ID") @RequestParam(required = false) Long spuId,
                                @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
                                @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size) {
        return R.ok(reviewService.listAll(minRating, maxRating, spuId, page, size));
    }

    @GetMapping("/rating/{spuId}")
    @Operation(summary = "商品评分统计")
    public R<Map<String, Object>> ratingSummary(@Parameter(description = "SPU ID") @PathVariable Long spuId) {
        Map<Integer, Long> dist = reviewService.getRatingDistribution(spuId);
        Double avg = reviewService.getAvgRating(spuId);
        return R.ok(Map.of("avgRating", avg, "distribution", dist));
    }
}
