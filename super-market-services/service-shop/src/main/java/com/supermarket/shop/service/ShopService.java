package com.supermarket.shop.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.supermarket.shop.entity.Merchant;
import com.supermarket.shop.entity.Shop;

public interface ShopService {

    Merchant applyMerchant(Merchant merchant);

    void auditMerchant(Long merchantId, Integer auditStatus, String reason);

    IPage<Merchant> pageMerchants(Integer page, Integer size, Integer auditStatus, String startDate, String endDate);

    Merchant getMerchantById(Long id);

    Shop createShop(Long merchantId, String shopName);

    Shop getShopById(Long id);

    Shop getShopByMerchantId(Long merchantId);
}
