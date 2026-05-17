package com.supermarket.search.service.impl;

import com.supermarket.common.core.exception.BizException;
import com.supermarket.search.entity.ProductDocument;
import com.supermarket.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.stereotype.Service;

import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final ElasticsearchOperations esOps;
    private static final IndexCoordinates PRODUCT_INDEX = IndexCoordinates.of("product_document");

    @Override
    public Map<String, Object> search(String keyword, Long categoryId, String brand,
                                       BigDecimal minPrice, BigDecimal maxPrice,
                                       String sort, int page, int size) {
        Criteria criteria = new Criteria("shelfStatus").is(1);

        if (keyword != null && !keyword.isBlank()) {
            criteria = criteria.and(new Criteria("name").contains(keyword));
        }
        if (categoryId != null) {
            criteria = criteria.and(new Criteria("categoryId").is(categoryId));
        }
        if (brand != null && !brand.isBlank()) {
            criteria = criteria.and(new Criteria("brandName").is(brand));
        }
        if (minPrice != null && maxPrice != null) {
            criteria = criteria.and(new Criteria("minPrice").between(minPrice.doubleValue(), maxPrice.doubleValue()));
        } else if (minPrice != null) {
            criteria = criteria.and(new Criteria("minPrice").greaterThanEqual(minPrice.doubleValue()));
        } else if (maxPrice != null) {
            criteria = criteria.and(new Criteria("minPrice").lessThanEqual(maxPrice.doubleValue()));
        }

        var query = new CriteriaQuery(criteria).setPageable(
                org.springframework.data.domain.PageRequest.of(page - 1, size));

        try {
            var searchHits = esOps.search(query, ProductDocument.class, PRODUCT_INDEX);
            List<ProductDocument> records = searchHits.getSearchHits().stream()
                    .map(SearchHit::getContent)
                    .collect(Collectors.toList());

            Map<String, Object> result = new HashMap<>();
            result.put("total", searchHits.getTotalHits());
            result.put("records", records);
            result.put("page", page);
            result.put("size", size);
            return result;
        } catch (Exception e) {
            log.error("Search error", e);
            // If ES is not available, return empty result
            Map<String, Object> empty = new HashMap<>();
            empty.put("total", 0L);
            empty.put("records", List.of());
            empty.put("page", page);
            empty.put("size", size);
            return empty;
        }
    }

    @Override
    public void indexProduct(ProductDocument doc) {
        IndexQuery indexQuery = new IndexQuery();
        indexQuery.setId(String.valueOf(doc.getSpuId()));
        indexQuery.setObject(doc);
        esOps.index(indexQuery, PRODUCT_INDEX);
    }

    @Override
    public void updateProduct(ProductDocument doc) {
        indexProduct(doc);
    }

    @Override
    public void deleteProduct(Long spuId) {
        esOps.delete(String.valueOf(spuId), PRODUCT_INDEX);
    }

    @Override
    public void bulkIndex(List<ProductDocument> docs) {
        List<IndexQuery> queries = docs.stream().map(doc -> {
            IndexQuery q = new IndexQuery();
            q.setId(String.valueOf(doc.getSpuId()));
            q.setObject(doc);
            return q;
        }).collect(Collectors.toList());
        esOps.bulkIndex(queries, PRODUCT_INDEX);
    }
}
