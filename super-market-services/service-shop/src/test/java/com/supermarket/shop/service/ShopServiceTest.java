package com.supermarket.shop.service;

import com.supermarket.common.core.exception.BizException;
import com.supermarket.shop.entity.Merchant;
import com.supermarket.shop.entity.Shop;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ShopServiceTest {

    @Autowired
    private ShopService shopService;

    @Test
    void shouldApplyMerchant() {
        Merchant merchant = newMerchant(501L);
        Merchant saved = shopService.applyMerchant(merchant);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getAuditStatus()).isEqualTo(0);
    }

    @Test
    void shouldAuditAndAutoCreateShop() {
        Merchant merchant = shopService.applyMerchant(newMerchant(502L));
        shopService.auditMerchant(merchant.getId(), 1, null);

        Shop shop = shopService.getShopByMerchantId(merchant.getId());
        assertThat(shop).isNotNull();
        assertThat(shop.getShopName()).contains("旗舰店");
    }

    @Test
    void shouldRejectDuplicateMerchantApplication() {
        shopService.applyMerchant(newMerchant(503L));
        assertThatThrownBy(() -> shopService.applyMerchant(newMerchant(503L)))
            .hasMessageContaining("已提交");
    }

    private Merchant newMerchant(Long userId) {
        Merchant m = new Merchant();
        m.setUserId(userId);
        m.setCompanyName("测试商家" + userId);
        m.setContactPhone("13800000000");
        return m;
    }
}
