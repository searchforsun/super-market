package com.supermarket.user.test;

import com.supermarket.common.dubbo.api.order.dto.CreateOrderRequest;
import com.supermarket.user.entity.User;

import java.math.BigDecimal;
import java.util.List;

/**
 * Test data factory providing reusable helper methods for creating test entities
 * with sensible defaults. Each method uses a fluent builder-like pattern via
 * overloaded variants or direct setter access on the returned object.
 */
public final class TestDataFactory {

    private TestDataFactory() {
        // utility class
    }

    // ========== User ==========

    /**
     * Creates a default active User with standard test data.
     */
    public static User createDefaultUser() {
        User user = new User();
        user.setPhone("13800000000");
        user.setEmail("test@example.com");
        user.setPasswordHash("$2a$10$hashedpassword");
        user.setNickname("测试用户");
        user.setRealName("张三");
        user.setStatus(1);
        return user;
    }

    /**
     * Creates a User with the given phone number.
     */
    public static User createUserWithPhone(String phone) {
        User user = createDefaultUser();
        user.setPhone(phone);
        return user;
    }

    /**
     * Creates an inactive (disabled) User.
     */
    public static User createDisabledUser() {
        User user = createDefaultUser();
        user.setStatus(0);
        return user;
    }

    // ========== CreateOrderRequest ==========

    /**
     * Creates a default CreateOrderRequest with a single order item.
     */
    public static CreateOrderRequest createDefaultOrderRequest() {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setUserId(1L);
        req.setShopId(1L);
        req.setAddressSnapshot("{\"name\":\"张三\",\"phone\":\"13800000000\",\"province\":\"北京市\",\"city\":\"北京市\",\"district\":\"朝阳区\",\"detail\":\"某某路100号\"}");

        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
        item.setSkuId(1001L);
        item.setSkuName("默认测试商品");
        item.setSkuPrice(new BigDecimal("99.00"));
        item.setQuantity(1);
        req.setItems(List.of(item));

        return req;
    }

    /**
     * Creates a CreateOrderRequest for the given user.
     */
    public static CreateOrderRequest createOrderRequestForUser(Long userId) {
        CreateOrderRequest req = createDefaultOrderRequest();
        req.setUserId(userId);
        return req;
    }

    /**
     * Creates a CreateOrderRequest with multiple items.
     */
    public static CreateOrderRequest createOrderWithItems(List<CreateOrderRequest.OrderItemRequest> items) {
        CreateOrderRequest req = createDefaultOrderRequest();
        req.setItems(items);
        return req;
    }

    /**
     * Creates a single OrderItemRequest with the given parameters.
     */
    public static CreateOrderRequest.OrderItemRequest createOrderItem(Long skuId, String skuName,
                                                                       BigDecimal price, int quantity) {
        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
        item.setSkuId(skuId);
        item.setSkuName(skuName);
        item.setSkuPrice(price);
        item.setQuantity(quantity);
        return item;
    }
}
