package com.supermarket.search.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProductDocument {
    private Long spuId;
    private String spuNo;
    private Long shopId;
    private String shopName;
    private Long categoryId;
    private String categoryName;
    private String brandName;
    private String name;
    private String subtitle;
    private String mainImage;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Integer totalStock;
    private Integer salesCount;
    private Double avgRating;
    private Integer reviewCount;
    private List<SkuDoc> skuList;
    private Integer shelfStatus;
    private LocalDateTime createdAt;

    @Data
    public static class SkuDoc {
        private Long skuId;
        private String specName;
        private BigDecimal price;
        private Integer stock;
        private String image;
    }
}
