package com.supermarket.shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.supermarket.common.core.exception.BizException;
import com.supermarket.shop.entity.Merchant;
import com.supermarket.shop.entity.Shop;
import com.supermarket.shop.mapper.MerchantMapper;
import com.supermarket.shop.mapper.ShopMapper;
import com.supermarket.shop.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
