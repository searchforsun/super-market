package com.supermarket.review.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supermarket.review.entity.Review;

import java.util.Map;

public interface ReviewService {

    Review create(Review review);

    Review append(Long reviewId, Long userId, String content, String images);

    void reply(Long reviewId, String content);

    Review getById(Long reviewId);

    Page<Review> listBySpu(Long spuId, Integer rating, int page, int size);

    Page<Review> listByUser(Long userId, int page, int size);

    Map<Integer, Long> getRatingDistribution(Long spuId);

    Double getAvgRating(Long spuId);
}
