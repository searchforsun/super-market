package com.supermarket.common.dubbo.api.payment.dto;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentDTO implements Serializable {
    private String payNo;
    private String orderNo;
    private BigDecimal amount;
    private Integer payMethod;
    private Integer payStatus;
    private String thirdPayNo;
    private LocalDateTime paidAt;
}
