package com.supermarket.product.service;

import com.supermarket.product.dto.CreateProductRequest;
import com.supermarket.product.entity.Spu;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @Test
    void shouldCreateProductWithSkus() {
        CreateProductRequest req = buildRequest();
        Spu spu = productService.createProduct(req);

        assertThat(spu.getId()).isNotNull();
        assertThat(spu.getAuditStatus()).isEqualTo(0);
        assertThat(spu.getShelfStatus()).isEqualTo(0);

        var skus = productService.getSkusBySpuId(spu.getId());
        assertThat(skus).hasSize(2);
    }

    @Test
    void shouldAuditProduct() {
        Spu spu = productService.createProduct(buildRequest());
        productService.auditProduct(spu.getId(), 1, null);

        Spu audited = productService.getSpuById(spu.getId());
        assertThat(audited.getAuditStatus()).isEqualTo(1);
        assertThat(audited.getShelfStatus()).isEqualTo(1);
    }

    @Test
    void shouldRejectDuplicateAudit() {
        Spu spu = productService.createProduct(buildRequest());
        productService.auditProduct(spu.getId(), 1, null);

        assertThatThrownBy(() -> productService.auditProduct(spu.getId(), 2, "重复审核"))
            .hasMessageContaining("已审核");
    }

    @Test
    void shouldUpdateShelfStatus() {
        Spu spu = productService.createProduct(buildRequest());
        productService.auditProduct(spu.getId(), 1, null);
        productService.updateShelfStatus(spu.getId(), 0);

        Spu offShelf = productService.getSpuById(spu.getId());
        assertThat(offShelf.getShelfStatus()).isEqualTo(0);
    }

    private CreateProductRequest buildRequest() {
        CreateProductRequest req = new CreateProductRequest();
        req.setShopId(1L);
        req.setCategoryId(10L);
        req.setName("测试商品");
        req.setMainImage("http://example.com/img.jpg");

        CreateProductRequest.SkuItem sku1 = new CreateProductRequest.SkuItem();
        sku1.setSpecName("红色-XL");
        sku1.setPrice(new BigDecimal("99.00"));

        CreateProductRequest.SkuItem sku2 = new CreateProductRequest.SkuItem();
        sku2.setSpecName("蓝色-XL");
        sku2.setPrice(new BigDecimal("109.00"));

        req.setSkus(List.of(sku1, sku2));
        return req;
    }
}
