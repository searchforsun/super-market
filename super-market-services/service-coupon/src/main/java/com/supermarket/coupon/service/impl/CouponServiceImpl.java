package com.supermarket.coupon.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supermarket.common.core.exception.BizException;
import com.supermarket.common.core.result.ResultCode;
import com.supermarket.common.dubbo.api.coupon.CouponDubboService;
import com.supermarket.coupon.entity.CouponBatch;
import com.supermarket.coupon.entity.CouponTemplate;
import com.supermarket.coupon.entity.UserCoupon;
import com.supermarket.coupon.mapper.CouponBatchMapper;
import com.supermarket.coupon.mapper.CouponTemplateMapper;
import com.supermarket.coupon.mapper.UserCouponMapper;
import com.supermarket.coupon.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@DubboService(interfaceClass = CouponDubboService.class)
@Service
@RequiredArgsConstructor
public class CouponServiceImpl extends ServiceImpl<CouponTemplateMapper, CouponTemplate>
        implements CouponService, CouponDubboService {

    private final CouponBatchMapper batchMapper;
    private final UserCouponMapper userCouponMapper;
    private final RedissonClient redissonClient;

    @Override
    @Transactional
    public CouponTemplate createTemplate(CouponTemplate template) {
        if (template.getRemainingStock() == null) {
            template.setRemainingStock(template.getTotalStock());
        }
        save(template);
        return template;
    }

    @Override
    public CouponTemplate getTemplateById(Long id) {
        CouponTemplate t = getById(id);
        if (t == null) throw new BizException(ResultCode.COUPON_TEMPLATE_NOT_FOUND);
        return t;
    }

    @Override
    @Transactional
    public CouponTemplate updateTemplate(CouponTemplate template) {
        updateById(template);
        return getById(template.getId());
    }

    @Override
    @Transactional
    public boolean setTemplateStatus(Long id, Integer status) {
        CouponTemplate t = new CouponTemplate();
        t.setId(id);
        t.setStatus(status);
        return updateById(t);
    }

    @Override
    @Transactional
    public void deleteTemplate(Long id) {
        removeById(id);
    }

    @Override
    public Page<CouponTemplate> listTemplates(int page, int size) {
        return page(new Page<>(page, size),
                new LambdaQueryWrapper<CouponTemplate>().eq(CouponTemplate::getStatus, 1));
    }

    @Override
    public Page<CouponBatch> listBatches(int page, int size) {
        return batchMapper.selectPage(new Page<>(page, size),
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CouponBatch>()
                        .orderByDesc(CouponBatch::getCreatedAt));
    }

    @Override
    @Transactional
    public CouponBatch distribute(Long templateId, List<Long> userIds) {
        CouponTemplate template = getTemplateById(templateId);
        if (template.getRemainingStock() < userIds.size()) {
            throw new BizException(ResultCode.COUPON_STOCK_INSUFFICIENT);
        }

        CouponBatch batch = new CouponBatch();
        batch.setTemplateId(templateId);
        batch.setBatchName(template.getName() + "-批次");
        batch.setQuantity(userIds.size());
        batch.setDistributeType(1); // 平台发放
        batchMapper.insert(batch);

        for (Long userId : userIds) {
            UserCoupon uc = buildCoupon(userId, template, batch.getId());
            userCouponMapper.insert(uc);
        }

        template.setRemainingStock(template.getRemainingStock() - userIds.size());
        updateById(template);

        return batch;
    }

    @Override
    @Transactional
    public UserCoupon claim(Long userId, Long templateId) {
        String lockKey = "smt:lock:coupon:claim:" + userId + ":" + templateId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(3, 5, TimeUnit.SECONDS)) {
                throw new BizException(ResultCode.RATE_LIMITED);
            }

            CouponTemplate template = getTemplateById(templateId);
            if (template.getStatus() != 1) {
                throw new BizException(ResultCode.COUPON_DISABLED);
            }
            if (template.getRemainingStock() <= 0) {
                throw new BizException(ResultCode.COUPON_STOCK_INSUFFICIENT);
            }

            // 检查是否已领取过
            Long count = userCouponMapper.selectCount(new LambdaQueryWrapper<UserCoupon>()
                    .eq(UserCoupon::getUserId, userId)
                    .eq(UserCoupon::getTemplateId, templateId));
            if (count >= template.getPerUserLimit()) {
                throw new BizException(ResultCode.COUPON_CLAIM_LIMIT);
            }

            // 扣减库存
            int rows = baseMapper.decrStock(templateId);
            if (rows == 0) {
                throw new BizException(ResultCode.COUPON_STOCK_INSUFFICIENT);
            }

            UserCoupon uc = buildCoupon(userId, template, null);
            userCouponMapper.insert(uc);
            return uc;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException(ResultCode.SYSTEM_ERROR);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public Page<UserCoupon> listUserCoupons(Long userId, Integer status, int page, int size) {
        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .orderByDesc(UserCoupon::getCreatedAt);
        if (status != null) wrapper.eq(UserCoupon::getStatus, status);
        return userCouponMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public List<UserCoupon> listAvailableCoupons(Long userId) {
        return userCouponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getStatus, 1)
                .gt(UserCoupon::getExpireTime, LocalDateTime.now())
                .orderByAsc(UserCoupon::getExpireTime));
    }

    // Dubbo 接口实现
    @Override
    public boolean tryUseCoupon(Long userId, Long couponId, String orderNo) {
        try {
            useCoupon(userId, couponId, orderNo);
            return true;
        } catch (Exception e) {
            log.error("核销优惠券失败: couponId={}", couponId, e);
            return false;
        }
    }

    @Override
    @Transactional
    public UserCoupon useCoupon(Long userId, Long couponId, String orderNo) {
        UserCoupon uc = userCouponMapper.selectById(couponId);
        if (uc == null) throw new BizException(ResultCode.COUPON_TEMPLATE_NOT_FOUND);
        if (!uc.getUserId().equals(userId)) throw new BizException(ResultCode.COUPON_NOT_OWNED);
        if (uc.getStatus() != 1) throw new BizException(ResultCode.COUPON_UNAVAILABLE);
        if (uc.getExpireTime().isBefore(LocalDateTime.now())) {
            uc.setStatus(3);
            userCouponMapper.updateById(uc);
            throw new BizException(ResultCode.COUPON_EXPIRED);
        }
        uc.setStatus(2);
        uc.setOrderNo(orderNo);
        uc.setUsedAt(LocalDateTime.now());
        userCouponMapper.updateById(uc);
        return uc;
    }

    @Override
    @Transactional
    public boolean returnCoupon(Long couponId) {
        UserCoupon uc = userCouponMapper.selectById(couponId);
        if (uc == null || uc.getStatus() != 2) return false;
        uc.setStatus(1);
        uc.setOrderNo(null);
        uc.setUsedAt(null);
        userCouponMapper.updateById(uc);
        return true;
    }

    private UserCoupon buildCoupon(Long userId, CouponTemplate template, Long batchId) {
        UserCoupon uc = new UserCoupon();
        uc.setUserId(userId);
        uc.setTemplateId(template.getId());
        uc.setBatchId(batchId);
        uc.setCouponCode("CPN" + IdUtil.getSnowflakeNextId());
        uc.setStatus(1);
        uc.setExpireTime(LocalDateTime.now().plusDays(template.getValidDays()));
        return uc;
    }
}
