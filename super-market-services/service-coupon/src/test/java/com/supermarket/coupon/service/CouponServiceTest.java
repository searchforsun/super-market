package com.supermarket.coupon.service;

import com.supermarket.common.core.exception.BizException;
import com.supermarket.coupon.entity.CouponTemplate;
import com.supermarket.coupon.entity.UserCoupon;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class CouponServiceTest {

    @Autowired
    private CouponService couponService;

    @Test
    void shouldCreateTemplate() {
        CouponTemplate t = couponService.createTemplate(newTemplate("满100减20", 1, 20, 100, 100));
        assertThat(t.getId()).isNotNull();
        assertThat(t.getRemainingStock()).isEqualTo(100);
    }

    @Test
    void shouldDistributeToUsers() {
        CouponTemplate t = couponService.createTemplate(newTemplate("新人券", 3, 10, 0, 50));
        couponService.distribute(t.getId(), List.of(1L, 2L, 3L));

        var uc1 = couponService.listUserCoupons(1L, 1, 1, 10);
        assertThat(uc1.getTotal()).isEqualTo(1);

        CouponTemplate updated = couponService.getTemplateById(t.getId());
        assertThat(updated.getRemainingStock()).isEqualTo(47);
    }

    @Test
    void shouldClaimCoupon() {
        CouponTemplate t = couponService.createTemplate(newTemplate("满50减10", 1, 10, 50, 100));
        UserCoupon uc = couponService.claim(10L, t.getId());
        assertThat(uc.getCouponCode()).startsWith("CPN");
        assertThat(uc.getStatus()).isEqualTo(1);
        assertThat(uc.getExpireTime()).isNotNull();
    }

    @Test
    void shouldPreventDuplicateClaim() {
        CouponTemplate tpl = newTemplate("每人限1张", 3, 5, 0, 10);
        tpl.setPerUserLimit(1);
        CouponTemplate t = couponService.createTemplate(tpl);

        couponService.claim(20L, t.getId());
        assertThatThrownBy(() -> couponService.claim(20L, t.getId()))
                .hasMessageContaining("已达每人限领数量");
    }

    @Test
    void shouldUseAndReturnCoupon() {
        CouponTemplate t = couponService.createTemplate(newTemplate("测试券", 3, 5, 0, 100));
        UserCoupon uc = couponService.claim(30L, t.getId());

        UserCoupon used = couponService.useCoupon(30L, uc.getId(), "ORD-TEST-001");
        assertThat(used.getStatus()).isEqualTo(2);

        boolean returned = couponService.returnCoupon(uc.getId());
        assertThat(returned).isTrue();
    }

    @Test
    void shouldListAvailableCoupons() {
        CouponTemplate t = couponService.createTemplate(newTemplate("可用券", 3, 5, 0, 100));
        couponService.claim(40L, t.getId());
        var list = couponService.listAvailableCoupons(40L);
        assertThat(list).hasSize(1);
    }

    private CouponTemplate newTemplate(String name, int type, int discountValue, int minAmount, int stock) {
        CouponTemplate t = new CouponTemplate();
        t.setName(name);
        t.setType(type);
        t.setDiscountValue(new BigDecimal(discountValue));
        t.setMinAmount(new BigDecimal(minAmount));
        t.setTotalStock(stock);
        t.setRemainingStock(stock);
        t.setPerUserLimit(1);
        t.setValidDays(30);
        t.setStatus(1);
        return t;
    }
}
