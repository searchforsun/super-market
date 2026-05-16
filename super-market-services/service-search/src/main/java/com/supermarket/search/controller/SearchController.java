package com.supermarket.search.controller;

import com.supermarket.common.core.result.R;
import com.supermarket.search.entity.ProductDocument;
import com.supermarket.search.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "搜索服务", description = "商品搜索、索引同步接口")
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/product")
    @Operation(summary = "商品搜索")
    public R<Map<String, Object>> search(@Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
                                          @Parameter(description = "分类ID") @RequestParam(required = false) Long categoryId,
                                          @Parameter(description = "品牌") @RequestParam(required = false) String brand,
                                          @Parameter(description = "最低价格") @RequestParam(required = false) BigDecimal minPrice,
                                          @Parameter(description = "最高价格") @RequestParam(required = false) BigDecimal maxPrice,
                                          @Parameter(description = "排序方式") @RequestParam(required = false, defaultValue = "newest") String sort,
                                          @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
                                          @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size) {
        return R.ok(searchService.search(keyword, categoryId, brand, minPrice, maxPrice, sort, page, size));
    }

    @PostMapping("/internal/sync")
    @Operation(summary = "同步单个商品索引")
    public R<Void> syncProduct(@Parameter(description = "商品文档") @RequestBody ProductDocument doc) {
        searchService.indexProduct(doc);
        return R.ok();
    }

    @PostMapping("/internal/sync/batch")
    @Operation(summary = "批量同步商品索引")
    public R<Void> syncBatch(@Parameter(description = "商品文档列表") @RequestBody List<ProductDocument> docs) {
        searchService.bulkIndex(docs);
        return R.ok();
    }

    @DeleteMapping("/internal/sync/{spuId}")
    @Operation(summary = "删除商品索引")
    public R<Void> deleteIndex(@Parameter(description = "商品SPU ID") @PathVariable Long spuId) {
        searchService.deleteProduct(spuId);
        return R.ok();
    }
}
