package com.supermarket.shop.service;

import com.supermarket.shop.entity.Merchant;
import com.supermarket.shop.entity.Shop;

public interface ShopService {

    Merchant applyMerchant(Merchant merchant);

    void auditMerchant(Long merchantId, Integer auditStatus, String reason);

    Merchant getMerchantById(Long id);

    Shop createShop(Long merchantId, String shopName);

    Shop getShopById(Long id);

    Shop getShopByMerchantId(Long merchantId);
}
