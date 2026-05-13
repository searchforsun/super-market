package com.supermarket.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supermarket.product.dto.CreateProductRequest;
import com.supermarket.product.entity.Sku;
import com.supermarket.product.entity.Spu;

import java.util.List;

public interface ProductService {

    Spu createProduct(CreateProductRequest request);

    Spu getSpuById(Long spuId);

    Spu getSpuByNo(String spuNo);

    Sku getSkuById(Long skuId);

    List<Sku> getSkusBySpuId(Long spuId);

    void auditProduct(Long spuId, Integer auditStatus, String reason);

    void updateShelfStatus(Long spuId, Integer shelfStatus);

    Page<Spu> listByShop(Long shopId, int page, int size);

    Page<Spu> listByCategory(Long categoryId, int page, int size, String sort);
}
