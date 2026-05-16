package com.supermarket.product.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProductRequest {
    @Size(max = 200, message = "商品名称最长200字")
    private String name;

    @Size(max = 500, message = "副标题最长500字")
    private String subtitle;

    private String mainImage;

    private String images;

    @Size(max = 2000, message = "描述最长2000字")
    private String description;
}
