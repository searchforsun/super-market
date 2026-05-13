package com.supermarket.common.dubbo.api.order.dto;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateOrderRequest implements Serializable {
    private Long userId;
    private Long addressId;
    private String addressSnapshot;
    private List<OrderItemRequest> items;
    private String remark;

    @Data
    public static class OrderItemRequest implements Serializable {
        private Long skuId;
        private String skuName;
        private String skuImage;
        private BigDecimal skuPrice;
        private Integer quantity;
    }
}
