package com.supermarket.search.flow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supermarket.search.entity.ProductDocument;
import com.supermarket.search.service.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Postman-style business flow test for the Search domain.
 * <p>
 * Chains API calls to simulate: Index Product -> Search by keyword ->
 * Search with filters -> Delete index.
 * <p>
 * Uses @SpringBootTest for full context but mocks SearchService and RedissonClient
 * so no Elasticsearch or Redis middleware is required.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration," +
                "org.apache.dubbo.spring.boot.autoconfigure.DubboAutoConfiguration," +
                "com.alibaba.cloud.nacos.NacosDiscoveryAutoConfiguration," +
                "com.alibaba.cloud.nacos.NacosConfigAutoConfiguration",
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.nacos.config.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Search Business Flow Test")
class SearchBusinessFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SearchService searchService;

    @MockBean
    private RedissonClient redissonClient;

    private ProductDocument sampleDoc;

    @BeforeEach
    void setUp() {
        // Build a sample product document
        sampleDoc = new ProductDocument();
        sampleDoc.setSpuId(100L);
        sampleDoc.setSpuNo("SPU20260516001");
        sampleDoc.setShopId(1L);
        sampleDoc.setShopName("官方旗舰店");
        sampleDoc.setCategoryId(10L);
        sampleDoc.setCategoryName("手机通讯");
        sampleDoc.setBrandName("SuperTech");
        sampleDoc.setName("SuperTech X100 智能手机");
        sampleDoc.setSubtitle("2026年度旗舰");
        sampleDoc.setMainImage("https://example.com/x100.jpg");
        sampleDoc.setMinPrice(new BigDecimal("4999.00"));
        sampleDoc.setMaxPrice(new BigDecimal("6999.00"));
        sampleDoc.setTotalStock(1000);
        sampleDoc.setSalesCount(500);
        sampleDoc.setAvgRating(4.8);
        sampleDoc.setReviewCount(200);
        sampleDoc.setShelfStatus(1);

        ProductDocument.SkuDoc skuDoc = new ProductDocument.SkuDoc();
        skuDoc.setSkuId(1001L);
        skuDoc.setSpecName("12GB+256GB");
        skuDoc.setPrice(new BigDecimal("4999.00"));
        skuDoc.setStock(500);
        skuDoc.setImage("https://example.com/x100_1.jpg");
        sampleDoc.setSkuList(List.of(skuDoc));
    }

    // ---------------------------------------------------------------
    // Flow 1: Complete Search Lifecycle
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Complete search lifecycle: sync -> search by keyword -> search with filters -> delete")
    void shouldCompleteFullSearchLifecycleFlow() throws Exception {
        // --- Mock setup ---
        doNothing().when(searchService).indexProduct(any(ProductDocument.class));

        // Single result for keyword search
        Map<String, Object> singleResult = new HashMap<>();
        singleResult.put("total", 1L);
        singleResult.put("records", List.of(sampleDoc));
        singleResult.put("page", 1);
        singleResult.put("size", 20);

        when(searchService.search(eq("智能手机"), isNull(), isNull(), isNull(), isNull(), eq("newest"), eq(1), eq(20)))
                .thenReturn(singleResult);

        // With category filter
        when(searchService.search(eq("智能手机"), eq(10L), isNull(), isNull(), isNull(), eq("newest"), eq(1), eq(20)))
                .thenReturn(singleResult);

        // With price range
        when(searchService.search(eq("智能手机"), isNull(), isNull(), eq(new BigDecimal("4000")), eq(new BigDecimal("8000")), eq("price_asc"), eq(1), eq(20)))
                .thenReturn(singleResult);

        // With brand filter
        when(searchService.search(isNull(), isNull(), eq("SuperTech"), isNull(), isNull(), eq("newest"), eq(1), eq(20)))
                .thenReturn(singleResult);

        doNothing().when(searchService).deleteProduct(100L);

        // ============================================================
        // Step 1: Index a product
        // ============================================================
        mockMvc.perform(post("/api/search/internal/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleDoc)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"));

        verify(searchService).indexProduct(any(ProductDocument.class));

        // ============================================================
        // Step 2: Search by keyword
        // ============================================================
        mockMvc.perform(get("/api/search/product")
                        .param("keyword", "智能手机")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records.length()").value(1));

        // ============================================================
        // Step 3: Search with category filter
        // ============================================================
        mockMvc.perform(get("/api/search/product")
                        .param("keyword", "智能手机")
                        .param("categoryId", "10")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1));

        // ============================================================
        // Step 4: Search with price range
        // ============================================================
        mockMvc.perform(get("/api/search/product")
                        .param("keyword", "智能手机")
                        .param("minPrice", "4000")
                        .param("maxPrice", "8000")
                        .param("sort", "price_asc")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1));

        // ============================================================
        // Step 5: Search with brand filter
        // ============================================================
        mockMvc.perform(get("/api/search/product")
                        .param("brand", "SuperTech")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1));

        // ============================================================
        // Step 6: Delete product index
        // ============================================================
        mockMvc.perform(delete("/api/search/internal/sync/{spuId}", 100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"));

        verify(searchService).deleteProduct(100L);
    }

    // ---------------------------------------------------------------
    // Flow 2: Batch Sync
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Batch sync multiple products then search")
    void shouldSyncBatchAndSearch() throws Exception {
        ProductDocument doc2 = new ProductDocument();
        doc2.setSpuId(101L);
        doc2.setName("SuperTech Pad 平板电脑");
        doc2.setShopId(1L);
        doc2.setCategoryId(10L);
        doc2.setShelfStatus(1);
        doc2.setMinPrice(new BigDecimal("2999.00"));
        doc2.setMaxPrice(new BigDecimal("3999.00"));

        List<ProductDocument> batchDocs = List.of(sampleDoc, doc2);

        doNothing().when(searchService).bulkIndex(anyList());

        Map<String, Object> multiResult = new HashMap<>();
        multiResult.put("total", 2L);
        multiResult.put("records", List.of(sampleDoc, doc2));
        multiResult.put("page", 1);
        multiResult.put("size", 20);

        when(searchService.search(eq("SuperTech"), isNull(), isNull(), isNull(), isNull(), eq("newest"), eq(1), eq(20)))
                .thenReturn(multiResult);

        // Batch sync
        mockMvc.perform(post("/api/search/internal/sync/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchDocs)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(searchService).bulkIndex(anyList());

        // Search should return 2 results
        mockMvc.perform(get("/api/search/product")
                        .param("keyword", "SuperTech")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(2));
    }

    // ---------------------------------------------------------------
    // Flow 3: Search Without Keyword
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Search without keyword returns all indexed products")
    void shouldSearchWithoutKeyword() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("total", 1L);
        result.put("records", List.of(sampleDoc));
        result.put("page", 1);
        result.put("size", 20);

        when(searchService.search(isNull(), isNull(), isNull(), isNull(), isNull(), eq("newest"), eq(1), eq(20)))
                .thenReturn(result);

        mockMvc.perform(get("/api/search/product")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1));
    }
}
