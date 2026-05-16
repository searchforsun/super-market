package com.supermarket.search.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supermarket.common.web.handler.GlobalExceptionHandler;
import com.supermarket.search.entity.ProductDocument;
import com.supermarket.search.service.SearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SearchController.class)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SearchService searchService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldSearchProductsWhenKeywordProvided() throws Exception {
        // Arrange
        Map<String, Object> searchResult = new HashMap<>();
        searchResult.put("total", 1);
        searchResult.put("products", List.of());

        when(searchService.search(eq("手机"), isNull(), isNull(), isNull(), isNull(), eq("newest"), eq(1), eq(20)))
                .thenReturn(searchResult);

        // Act & Assert
        mockMvc.perform(get("/api/search/product")
                .param("keyword", "手机"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1));

        verify(searchService).search(eq("手机"), isNull(), isNull(), isNull(), isNull(), eq("newest"), eq(1), eq(20));
    }

    @Test
    void shouldSyncProductWhenDocProvided() throws Exception {
        // Arrange
        ProductDocument doc = new ProductDocument();
        doc.setSpuId(100L);
        doc.setName("测试商品");

        doNothing().when(searchService).indexProduct(any(ProductDocument.class));

        // Act & Assert
        mockMvc.perform(post("/api/search/internal/sync")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(doc)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"));

        verify(searchService).indexProduct(any(ProductDocument.class));
    }

    @Test
    void shouldSyncBatchWhenDocsProvided() throws Exception {
        // Arrange
        ProductDocument doc = new ProductDocument();
        doc.setSpuId(100L);
        doc.setName("测试商品");

        List<ProductDocument> docs = List.of(doc);

        doNothing().when(searchService).bulkIndex(anyList());

        // Act & Assert
        mockMvc.perform(post("/api/search/internal/sync/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(docs)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"));

        verify(searchService).bulkIndex(anyList());
    }

    @Test
    void shouldDeleteIndexWhenSpuIdProvided() throws Exception {
        // Arrange
        doNothing().when(searchService).deleteProduct(100L);

        // Act & Assert
        mockMvc.perform(delete("/api/search/internal/sync/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"));

        verify(searchService).deleteProduct(100L);
    }
}
