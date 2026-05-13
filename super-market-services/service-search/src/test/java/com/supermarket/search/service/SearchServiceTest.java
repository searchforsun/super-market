package com.supermarket.search.service;

import com.supermarket.search.entity.ProductDocument;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
class SearchServiceTest {

    @Autowired
    private SearchService searchService;

    @MockBean
    private ElasticsearchOperations esOps;

    @Test
    void shouldReturnEmptyWhenEsUnavailable() {
        Map<String, Object> result = searchService.search("手机", null, null,
                null, null, "newest", 1, 20);
        assertThat(result.get("total")).isEqualTo(0L);
        assertThat(result.get("records")).isNotNull();
    }

    @Test
    void shouldAcceptSearchParams() {
        Map<String, Object> result = searchService.search("电脑", 10L, "华为",
                new BigDecimal("1000"), new BigDecimal("5000"), "price_asc", 1, 10);
        assertThat(result).containsKeys("total", "records", "page", "size");
    }

    @Test
    void shouldBuildProductDocument() {
        ProductDocument doc = new ProductDocument();
        doc.setSpuId(1L);
        doc.setSpuNo("SPU001");
        doc.setName("测试手机");
        doc.setCategoryId(10L);
        doc.setMinPrice(new BigDecimal("999.00"));
        doc.setMaxPrice(new BigDecimal("1999.00"));
        doc.setSkuList(List.of());
        doc.setShelfStatus(1);

        assertThat(doc.getSpuId()).isEqualTo(1L);
        assertThat(doc.getName()).isEqualTo("测试手机");
    }
}
