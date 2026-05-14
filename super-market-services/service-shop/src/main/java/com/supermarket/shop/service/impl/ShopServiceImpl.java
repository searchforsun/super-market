package com.supermarket.shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supermarket.common.core.exception.BizException;
import com.supermarket.shop.entity.Merchant;
import com.supermarket.shop.entity.Shop;
import com.supermarket.shop.mapper.MerchantMapper;
import com.supermarket.shop.mapper.ShopMapper;
import com.supermarket.shop.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class ShopServiceImpl implements ShopService {

    private final MerchantMapper merchantMapper;
    private final ShopMapper shopMapper;

    @Override
    @Transactional
    public Merchant applyMerchant(Merchant merchant) {
        Merchant exist = merchantMapper.selectOne(
            new LambdaQueryWrapper<Merchant>().eq(Merchant::getUserId, merchant.getUserId()));
        if (exist != null) {
            throw new BizException(400, "该用户已提交入驻申请");
        }
        merchant.setAuditStatus(0);
        merchant.setStatus(1);
        merchantMapper.insert(merchant);
        return merchant;
    }

    @Override
    @Transactional
    public void auditMerchant(Long merchantId, Integer auditStatus, String reason) {
        Merchant merchant = getMerchantById(merchantId);
        if (merchant.getAuditStatus() != 0) {
            throw new BizException(400, "该商家已审核");
        }
        merchant.setAuditStatus(auditStatus);
        merchant.setAuditReason(reason);
        if (auditStatus == 1) {
            createShop(merchantId, merchant.getCompanyName() + "旗舰店");
        }
        merchantMapper.updateById(merchant);
    }

    @Override
    public IPage<Merchant> pageMerchants(Integer page, Integer size, Integer auditStatus, String startDate, String endDate) {
        Page<Merchant> p = new Page<>(page, size);
        LambdaQueryWrapper<Merchant> wrapper = new LambdaQueryWrapper<>();
        if (auditStatus != null) {
            wrapper.eq(Merchant::getAuditStatus, auditStatus);
        }
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(Merchant::getCreatedAt, LocalDateTime.parse(startDate + " 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(Merchant::getCreatedAt, LocalDateTime.parse(endDate + " 23:59:59", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        wrapper.orderByDesc(Merchant::getCreatedAt);
        return merchantMapper.selectPage(p, wrapper);
    }

    @Override
    public Merchant getMerchantById(Long id) {
        Merchant m = merchantMapper.selectById(id);
        if (m == null) {
            throw new BizException(404, "商家不存在");
        }
        return m;
    }

    @Override
    @Transactional
    public Shop createShop(Long merchantId, String shopName) {
        Shop shop = new Shop();
        shop.setMerchantId(merchantId);
        shop.setShopName(shopName);
        shop.setStatus(1);
        shopMapper.insert(shop);
        return shop;
    }

    @Override
    public Shop getShopById(Long id) {
        Shop shop = shopMapper.selectById(id);
        if (shop == null) {
            throw new BizException(404, "店铺不存在");
        }
        return shop;
    }

    @Override
    public Shop getShopByMerchantId(Long merchantId) {
        return shopMapper.selectOne(
            new LambdaQueryWrapper<Shop>().eq(Shop::getMerchantId, merchantId));
    }
}
