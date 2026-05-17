package com.supermarket.common.dubbo.api.shop;

/**
 * Dubbo service for merchant/shop status queries from other services (e.g. auth-service).
 */
public interface ShopDubboService {

    /**
     * Check if a user has an approved merchant record.
     * @return true if the user has a merchant with approved status
     */
    boolean hasMerchant(Long userId);

    /**
     * Get the shop ID for a given user ID (via merchant record).
     * @return shop ID, or null if the user has no approved shop
     */
    Long getShopIdByUserId(Long userId);
}
