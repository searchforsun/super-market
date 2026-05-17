package com.supermarket.product.flow;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supermarket.product.dto.CreateProductRequest;
import com.supermarket.product.dto.UpdateProductRequest;
import com.supermarket.product.dto.UpdateSkuRequest;
import com.supermarket.product.entity.Sku;
import com.supermarket.product.entity.Spu;
import com.supermarket.product.service.ProductService;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Postman-style business flow test for the Product domain.
 * <p>
 * Chains API calls through MockMvc to simulate the complete browse-to-cart lifecycle:
 * Create Product -> Get SPU/SKUs -> Audit -> Shelf -> List -> Search -> Update.
 * <p>
 * Uses @SpringBootTest for full context but mocks ProductService and RedissonClient
 * so no database or Redis middleware is required.
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
@DisplayName("Product Business Flow Test")
class ProductBusinessFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @MockBean
    private RedissonClient redissonClient;

    private Spu createSpu(Long id, String name, Long shopId, Long categoryId, int auditStatus, int shelfStatus) {
        Spu spu = new Spu();
        spu.setId(id);
        spu.setSpuNo("SPU" + System.currentTimeMillis());
        spu.setName(name);
        spu.setShopId(shopId);
        spu.setCategoryId(categoryId);
        spu.setSubtitle("副标题");
        spu.setMainImage("https://example.com/img.jpg");
        spu.setDescription("商品描述");
        spu.setAuditStatus(auditStatus);
        spu.setShelfStatus(shelfStatus);
        spu.setIsDeleted(0);
        spu.setCreatedAt(LocalDateTime.now());
        return spu;
    }

    private Sku createSku(Long id, String specName, Long spuId, BigDecimal price) {
        Sku sku = new Sku();
        sku.setId(id);
        sku.setSkuNo("SKU" + System.currentTimeMillis());
        sku.setSpuId(spuId);
        sku.setSpecName(specName);
        sku.setPrice(price);
        sku.setMarketPrice(price.add(new BigDecimal("30")));
        sku.setWeight(500);
        sku.setStatus(1);
        return sku;
    }

    @BeforeEach
    void setUp() {
        // No additional Redisson setup needed — ProductService doesn't use Redisson,
        // and the @MockBean ensures no Redis connection is attempted.
    }

    // ---------------------------------------------------------------
    // Flow 1: Complete Product Lifecycle
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Complete product lifecycle: create -> get -> list -> search -> update")
    void shouldCompleteFullProductLifecycleFlow() throws Exception {
        Long spuId = 100L;
        Long skuId1 = 1001L;
        Long skuId2 = 1002L;

        Spu createdSpu = createSpu(spuId, "旗舰智能手机", 1L, 1L, 0, 0);
        Spu auditedSpu = createSpu(spuId, "旗舰智能手机", 1L, 1L, 1, 1);
        Spu updatedSpu = createSpu(spuId, "旗舰智能手机-2026款", 1L, 1L, 1, 1);
        Sku sku1 = createSku(skuId1, "标准版", spuId, new BigDecimal("99.99"));
        Sku sku2 = createSku(skuId2, "Pro版", spuId, new BigDecimal("199.99"));

        Page<Spu> singleItemPage = new Page<>(1, 20);
        singleItemPage.setRecords(List.of(createdSpu));
        singleItemPage.setTotal(1);

        // --- Mock setup ---

        // Step 1: createProduct returns the spu
        when(productService.createProduct(any(CreateProductRequest.class))).thenReturn(createdSpu);

        // Step 2: getSpuById returns createdSpu, then auditedSpu, then updatedSpu
        when(productService.getSpuById(spuId)).thenReturn(createdSpu, updatedSpu);

        // Step 3: getSkusBySpuId returns two SKUs
        when(productService.getSkusBySpuId(spuId)).thenReturn(List.of(sku1, sku2));

        // Steps 4-5: audit + shelf
        doNothing().when(productService).auditProduct(spuId, 1, "商品信息完整，审核通过");
        doNothing().when(productService).updateShelfStatus(spuId, 1);

        // Steps 6-7: list by shop / category
        when(productService.listByShop(eq(1L), anyInt(), anyInt())).thenReturn(singleItemPage);
        when(productService.listByCategory(eq(1L), anyInt(), anyInt(), isNull())).thenReturn(singleItemPage);

        // Step 8: search
        Page<Spu> searchPage = new Page<>(1, 20);
        searchPage.setRecords(List.of(auditedSpu));
        searchPage.setTotal(1);
        when(productService.searchByName(eq("旗舰"), anyInt(), anyInt())).thenReturn(searchPage);

        // Step 9: admin list
        when(productService.listForAdmin(eq(1), isNull(), isNull(), anyInt(), anyInt())).thenReturn(singleItemPage);

        // Step 10: update spu
        doNothing().when(productService).updateSpu(eq(spuId), any(UpdateProductRequest.class));

        // Step 11: update sku
        doNothing().when(productService).updateSku(eq(skuId1), any(UpdateSkuRequest.class));

        // ============================================================
        // Step 1: Create a product (SPU)
        // ============================================================
        CreateProductRequest.SkuItem standardSku = new CreateProductRequest.SkuItem();
        standardSku.setSpecName("标准版");
        standardSku.setPrice(new BigDecimal("99.99"));
        standardSku.setMarketPrice(new BigDecimal("129.99"));
        standardSku.setWeight(500);

        CreateProductRequest.SkuItem proSku = new CreateProductRequest.SkuItem();
        proSku.setSpecName("Pro版");
        proSku.setPrice(new BigDecimal("199.99"));
        proSku.setMarketPrice(new BigDecimal("259.99"));
        proSku.setWeight(500);

        CreateProductRequest createRequest = new CreateProductRequest();
        createRequest.setShopId(1L);
        createRequest.setCategoryId(1L);
        createRequest.setName("旗舰智能手机");
        createRequest.setSubtitle("2026年度旗舰机型");
        createRequest.setMainImage("https://example.com/phone.jpg");
        createRequest.setImages(List.of("https://example.com/phone_1.jpg", "https://example.com/phone_2.jpg"));
        createRequest.setDescription("这是一款旗舰智能手机，搭载最新处理器。");
        createRequest.setSkus(List.of(standardSku, proSku));

        String createResponse = mockMvc.perform(post("/api/product/spu")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value("旗舰智能手机"))
                .andExpect(jsonPath("$.data.shopId").value(1))
                .andReturn().getResponse().getContentAsString();

        JsonNode createRoot = objectMapper.readTree(createResponse);
        Long returnedSpuId = createRoot.get("data").get("id").asLong();

        // ============================================================
        // Step 2: Retrieve the SPU by ID
        // ============================================================
        mockMvc.perform(get("/api/product/spu/{spuId}", returnedSpuId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value("旗舰智能手机"))
                .andExpect(jsonPath("$.data.categoryId").value(1));

        // ============================================================
        // Step 3: Retrieve SKU list
        // ============================================================
        String skusResponse = mockMvc.perform(get("/api/product/spu/{spuId}/skus", returnedSpuId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].specName").isString())
                .andReturn().getResponse().getContentAsString();

        JsonNode skusRoot = objectMapper.readTree(skusResponse);

        // ============================================================
        // Step 4: Audit the product (approve)
        // ============================================================
        mockMvc.perform(put("/api/product/spu/{spuId}/audit", returnedSpuId)
                        .param("auditStatus", "1")
                        .param("reason", "商品信息完整，审核通过"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"));

        // ============================================================
        // Step 5: Set shelf to online
        // ============================================================
        mockMvc.perform(put("/api/product/spu/{spuId}/shelf", returnedSpuId)
                        .param("shelfStatus", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"));

        // ============================================================
        // Step 6: List by shop
        // ============================================================
        mockMvc.perform(get("/api/product/list/shop/1")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records.length()").isNumber());

        // ============================================================
        // Step 7: List by category
        // ============================================================
        mockMvc.perform(get("/api/product/list/category/1")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // ============================================================
        // Step 8: Search by keyword
        // ============================================================
        mockMvc.perform(get("/api/product/search")
                        .param("keyword", "旗舰")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records[0].name").value("旗舰智能手机"));

        // ============================================================
        // Step 9: Admin list with filter
        // ============================================================
        mockMvc.perform(get("/api/product/list")
                        .param("auditStatus", "1")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // ============================================================
        // Step 10: Update SPU info
        // ============================================================
        UpdateProductRequest updateSpuRequest = new UpdateProductRequest();
        updateSpuRequest.setName("旗舰智能手机-2026款");
        updateSpuRequest.setSubtitle("2026年度旗舰机型-升级版");

        mockMvc.perform(put("/api/product/spu/{spuId}", returnedSpuId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateSpuRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"));

        // Verify the update persisted
        mockMvc.perform(get("/api/product/spu/{spuId}", returnedSpuId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value("旗舰智能手机-2026款"));

        // ============================================================
        // Step 11: Update SKU price
        // ============================================================
        UpdateSkuRequest updateSkuRequest = new UpdateSkuRequest();
        updateSkuRequest.setPrice(new BigDecimal("89.99"));
        updateSkuRequest.setStatus(1);

        mockMvc.perform(put("/api/product/sku/{skuId}", skuId1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateSkuRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"));

        // Verify all service methods were called
        verify(productService).createProduct(any(CreateProductRequest.class));
        verify(productService, times(2)).getSpuById(spuId);
        verify(productService).getSkusBySpuId(spuId);
        verify(productService).auditProduct(spuId, 1, "商品信息完整，审核通过");
        verify(productService).updateShelfStatus(spuId, 1);
        verify(productService).listByShop(eq(1L), anyInt(), anyInt());
        verify(productService).listByCategory(eq(1L), anyInt(), anyInt(), isNull());
        verify(productService).searchByName(eq("旗舰"), anyInt(), anyInt());
        verify(productService).updateSpu(eq(spuId), any(UpdateProductRequest.class));
        verify(productService).updateSku(eq(skuId1), any(UpdateSkuRequest.class));
    }

    // ---------------------------------------------------------------
    // Flow 2: Minimal Create + Verify
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Create product with minimal required fields only")
    void shouldCreateProductWithMinimalFields() throws Exception {
        Long spuId = 200L;
        Spu spu = createSpu(spuId, "简约商品", 2L, 3L, 0, 0);

        when(productService.createProduct(any(CreateProductRequest.class))).thenReturn(spu);
        when(productService.getSpuById(spuId)).thenReturn(spu);

        CreateProductRequest request = new CreateProductRequest();
        request.setShopId(2L);
        request.setCategoryId(3L);
        request.setName("简约商品");

        String response = mockMvc.perform(post("/api/product/spu")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value("简约商品"))
                .andReturn().getResponse().getContentAsString();

        JsonNode root = objectMapper.readTree(response);
        Long returnedSpuId = root.get("data").get("id").asLong();

        mockMvc.perform(get("/api/product/spu/{spuId}", returnedSpuId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value("简约商品"));
    }

    // ---------------------------------------------------------------
    // Flow 3: Admin List Filter
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Admin list filters by audit status")
    void shouldFilterByAuditStatusInAdminList() throws Exception {
        Long spuId = 300L;
        Spu pendingSpu = createSpu(spuId, "待审核商品", 1L, 2L, 0, 0);

        when(productService.createProduct(any(CreateProductRequest.class))).thenReturn(pendingSpu);

        Page<Spu> pendingPage = new Page<>(1, 20);
        pendingPage.setRecords(List.of(pendingSpu));
        pendingPage.setTotal(1);
        when(productService.listForAdmin(eq(0), isNull(), isNull(), anyInt(), anyInt())).thenReturn(pendingPage);

        CreateProductRequest request = new CreateProductRequest();
        request.setShopId(1L);
        request.setCategoryId(2L);
        request.setName("待审核商品");

        mockMvc.perform(post("/api/product/spu")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/product/list")
                        .param("auditStatus", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(productService).listForAdmin(eq(0), isNull(), isNull(), anyInt(), anyInt());
    }

    // ---------------------------------------------------------------
    // Flow 4: Search Not Found
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Search returns empty for non-existent keyword")
    void shouldReturnEmptyWhenSearchKeywordNotFound() throws Exception {
        Page<Spu> emptyPage = new Page<>(1, 20);
        emptyPage.setRecords(List.of());
        emptyPage.setTotal(0);
        when(productService.searchByName(eq("NONEXISTENT_PRODUCT_XYZ_98765"), anyInt(), anyInt()))
                .thenReturn(emptyPage);

        mockMvc.perform(get("/api/product/search")
                        .param("keyword", "NONEXISTENT_PRODUCT_XYZ_98765"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records.length()").value(0));
    }
}
