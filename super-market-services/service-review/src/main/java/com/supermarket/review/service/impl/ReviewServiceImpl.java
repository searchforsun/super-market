package com.supermarket.review.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supermarket.common.core.exception.BizException;
import com.supermarket.common.core.result.ResultCode;
import com.supermarket.review.entity.Review;
import com.supermarket.review.mapper.ReviewMapper;
import com.supermarket.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl extends ServiceImpl<ReviewMapper, Review> implements ReviewService {

    @Override
    @Transactional
    public Review create(Review review) {
        if (review.getRating() < 1 || review.getRating() > 5) {
            throw new BizException(ResultCode.REVIEW_RATING_INVALID);
        }
        if (StrUtil.isBlank(review.getOrderNo())) {
            throw new BizException(ResultCode.REVIEW_ORDER_REQUIRED);
        }
        if (review.getSpuId() == null) {
            throw new BizException(ResultCode.REVIEW_PRODUCT_REQUIRED);
        }
        review.setStatus(1);
        save(review);
        return review;
    }

    @Override
    @Transactional
    public Review append(Long reviewId, Long userId, String content, String images) {
        Review review = getById(reviewId);
        if (review == null) {
            throw new BizException(ResultCode.REVIEW_NOT_FOUND);
        }
        if (!review.getUserId().equals(userId)) {
            throw new BizException(ResultCode.REVIEW_NOT_OWNED);
        }
        review.setContent((review.getContent() != null ? review.getContent() : "")
                + "\n【追评】" + content);
        if (images != null) {
            review.setImages(images);
        }
        updateById(review);
        return review;
    }

    @Override
    @Transactional
    public void reply(Long reviewId, String content) {
        Review review = getById(reviewId);
        if (review == null) {
            throw new BizException(ResultCode.REVIEW_NOT_FOUND);
        }
        review.setReplyContent(content);
        review.setReplyAt(LocalDateTime.now());
        updateById(review);
    }

    @Override
    public Review getById(Long reviewId) {
        Review review = super.getById(reviewId);
        if (review == null) {
            throw new BizException(ResultCode.REVIEW_NOT_FOUND);
        }
        return review;
    }

    @Override
    public Page<Review> listBySpu(Long spuId, Integer rating, int page, int size) {
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getSpuId, spuId)
               .eq(Review::getStatus, 1)
               .orderByDesc(Review::getCreatedAt);
        if (rating != null && rating >= 1 && rating <= 5) {
            wrapper.eq(Review::getRating, rating);
        }
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public Page<Review> listByUser(Long userId, int page, int size) {
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getUserId, userId)
               .eq(Review::getStatus, 1)
               .orderByDesc(Review::getCreatedAt);
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public Page<Review> listAll(Integer minRating, Integer maxRating, Long spuId, int page, int size) {
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Review::getCreatedAt);
        if (minRating != null) {
            wrapper.ge(Review::getRating, minRating);
        }
        if (maxRating != null) {
            wrapper.le(Review::getRating, maxRating);
        }
        if (spuId != null) {
            wrapper.eq(Review::getSpuId, spuId);
        }
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public Map<Integer, Long> getRatingDistribution(Long spuId) {
        List<Map<String, Object>> rows = baseMapper.countByRating(spuId);
        Map<Integer, Long> dist = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            dist.put(i, 0L);
        }
        for (Map<String, Object> row : rows) {
            Object rObj = row.get("rating") != null ? row.get("rating") : row.get("RATING");
            Object cObj = row.get("cnt") != null ? row.get("cnt") : row.get("CNT");
            if (rObj == null || cObj == null) continue;
            Integer r = ((Number) rObj).intValue();
            Long cnt = ((Number) cObj).longValue();
            dist.put(r, cnt);
        }
        return dist;
    }

    @Override
    public Double getAvgRating(Long spuId) {
        Double avg = baseMapper.avgRating(spuId);
        return avg != null ? avg : 0.0;
    }
}
