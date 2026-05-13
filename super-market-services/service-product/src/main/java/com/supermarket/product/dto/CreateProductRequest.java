package com.supermarket.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateProductRequest {

    @NotNull(message = "店铺ID不能为空")
    private Long shopId;

    @NotNull(message = "类目ID不能为空")
    private Long categoryId;

    private Long brandId;

    @NotBlank(message = "商品名称不能为空")
    private String name;

    private String subtitle;
    private String mainImage;
    private List<String> images;
    private String description;

    private List<SkuItem> skus;

    @Data
    public static class SkuItem {
        @NotBlank
        private String specName;
        private String specCode;
        @NotNull
        private BigDecimal price;
        private BigDecimal marketPrice;
        private BigDecimal costPrice;
        private String image;
        private Integer weight;
    }
}
