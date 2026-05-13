package com.supermarket.search.service;

import com.supermarket.search.entity.ProductDocument;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface SearchService {

    Map<String, Object> search(String keyword, Long categoryId, String brand,
                               BigDecimal minPrice, BigDecimal maxPrice,
                               String sort, int page, int size);

    void indexProduct(ProductDocument doc);

    void updateProduct(ProductDocument doc);

    void deleteProduct(Long spuId);

    void bulkIndex(List<ProductDocument> docs);
}
