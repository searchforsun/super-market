package com.supermarket.search.controller;

import com.supermarket.common.core.result.R;
import com.supermarket.search.entity.ProductDocument;
import com.supermarket.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/product")
    public R<Map<String, Object>> search(@RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) Long categoryId,
                                          @RequestParam(required = false) String brand,
                                          @RequestParam(required = false) BigDecimal minPrice,
                                          @RequestParam(required = false) BigDecimal maxPrice,
                                          @RequestParam(required = false, defaultValue = "newest") String sort,
                                          @RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        return R.ok(searchService.search(keyword, categoryId, brand, minPrice, maxPrice, sort, page, size));
    }

    @PostMapping("/internal/sync")
    public R<Void> syncProduct(@RequestBody ProductDocument doc) {
        searchService.indexProduct(doc);
        return R.ok();
    }

    @PostMapping("/internal/sync/batch")
    public R<Void> syncBatch(@RequestBody List<ProductDocument> docs) {
        searchService.bulkIndex(docs);
        return R.ok();
    }

    @DeleteMapping("/internal/sync/{spuId}")
    public R<Void> deleteIndex(@PathVariable Long spuId) {
        searchService.deleteProduct(spuId);
        return R.ok();
    }
}
