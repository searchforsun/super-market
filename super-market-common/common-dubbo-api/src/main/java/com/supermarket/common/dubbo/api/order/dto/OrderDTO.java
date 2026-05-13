package com.supermarket.common.dubbo.api.order.dto;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderDTO implements Serializable {
    private Long id;
    private String orderNo;
    private Long userId;
    private Long shopId;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal freightAmount;
    private BigDecimal actualAmount;
    private Integer orderStatus;
    private String addressSnapshot;
    private LocalDateTime expireTime;
    private LocalDateTime createdAt;
}
