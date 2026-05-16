package com.supermarket.product.dto;

import jakarta.validation.constraints.DecimalMin;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class UpdateSkuRequest {
    @DecimalMin(value = "0.01", message = "价格必须大于0")
    private BigDecimal price;

    @DecimalMin(value = "0", message = "市场价不能为负数")
    private BigDecimal marketPrice;

    private String image;

    private Integer status;
}
