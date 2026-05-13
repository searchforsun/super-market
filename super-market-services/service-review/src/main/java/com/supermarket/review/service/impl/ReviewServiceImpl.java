package com.supermarket.review.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supermarket.common.core.exception.BizException;
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
            throw new BizException(400, "评分必须在1-5之间");
        }
        if (StrUtil.isBlank(review.getOrderNo())) {
            throw new BizException(400, "订单号不能为空");
        }
        if (review.getSpuId() == null) {
            throw new BizException(400, "商品ID不能为空");
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
            throw new BizException(404, "评价不存在");
        }
        if (!review.getUserId().equals(userId)) {
            throw new BizException(403, "只能追加自己的评价");
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
            throw new BizException(404, "评价不存在");
        }
        review.setReplyContent(content);
        review.setReplyAt(LocalDateTime.now());
        updateById(review);
    }

    @Override
    public Review getById(Long reviewId) {
        Review review = super.getById(reviewId);
        if (review == null) {
            throw new BizException(404, "评价不存在");
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
    public Map<Integer, Long> getRatingDistribution(Long spuId) {
        List<Map<String, Object>> rows = baseMapper.countByRating(spuId);
        Map<Integer, Long> dist = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            dist.put(i, 0L);
        }
        for (Map<String, Object> row : rows) {
            Integer r = ((Number) row.get("rating")).intValue();
            Long cnt = ((Number) row.get("cnt")).longValue();
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
