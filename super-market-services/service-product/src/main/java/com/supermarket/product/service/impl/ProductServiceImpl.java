package com.supermarket.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supermarket.common.core.exception.BizException;
import com.supermarket.common.core.result.ResultCode;
import com.supermarket.product.dto.CreateProductRequest;
import com.supermarket.product.dto.UpdateProductRequest;
import com.supermarket.product.dto.UpdateSkuRequest;
import com.supermarket.product.entity.Sku;
import com.supermarket.product.entity.Spu;
import com.supermarket.product.mapper.SkuMapper;
import com.supermarket.product.mapper.SpuMapper;
import com.supermarket.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends ServiceImpl<SpuMapper, Spu> implements ProductService {

    private final SkuMapper skuMapper;

    @Override
    @Transactional
    public Spu createProduct(CreateProductRequest request) {
        Spu spu = new Spu();
        spu.setSpuNo("SPU" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6));
        spu.setShopId(request.getShopId());
        spu.setCategoryId(request.getCategoryId());
        spu.setBrandId(request.getBrandId());
        spu.setName(request.getName());
        spu.setSubtitle(request.getSubtitle());
        spu.setMainImage(request.getMainImage());
        spu.setImages(request.getImages() != null && !request.getImages().isEmpty()
                ? "[\"" + String.join("\",\"", request.getImages()) + "\"]" : null);
        spu.setDescription(request.getDescription());
        spu.setAuditStatus(0);
        spu.setShelfStatus(0);
        save(spu);

        if (request.getSkus() != null) {
            for (CreateProductRequest.SkuItem item : request.getSkus()) {
                Sku sku = new Sku();
                sku.setSkuNo("SKU" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6));
                sku.setSpuId(spu.getId());
                sku.setSpecName(item.getSpecName());
                sku.setSpecCode(item.getSpecCode());
                sku.setPrice(item.getPrice());
                sku.setMarketPrice(item.getMarketPrice());
                sku.setCostPrice(item.getCostPrice());
                sku.setImage(item.getImage());
                sku.setWeight(item.getWeight() != null ? item.getWeight() : 0);
                sku.setStatus(1);
                skuMapper.insert(sku);
            }
        }
        return spu;
    }

    @Override
    public Spu getSpuById(Long spuId) {
        Spu spu = getById(spuId);
        if (spu == null || spu.getIsDeleted() == 1) {
            throw new BizException(ResultCode.PRODUCT_NOT_FOUND);
        }
        populatePrices(List.of(spu));
        return spu;
    }

    @Override
    public Spu getSpuByNo(String spuNo) {
        return getOne(new LambdaQueryWrapper<Spu>().eq(Spu::getSpuNo, spuNo));
    }

    @Override
    public Sku getSkuById(Long skuId) {
        Sku sku = skuMapper.selectById(skuId);
        if (sku == null) {
            throw new BizException(ResultCode.SKU_NOT_FOUND);
        }
        return sku;
    }

    @Override
    public List<Sku> getSkusBySpuId(Long spuId) {
        return skuMapper.selectList(new LambdaQueryWrapper<Sku>().eq(Sku::getSpuId, spuId));
    }

    @Override
    public void auditProduct(Long spuId, Integer auditStatus, String reason) {
        if (auditStatus != 1 && auditStatus != 2) {
            throw new BizException(ResultCode.PARAM_ERROR);
        }
        Spu spu = getSpuById(spuId);
        if (spu.getAuditStatus() != 0) {
            throw new BizException(ResultCode.PRODUCT_ALREADY_AUDITED);
        }
        spu.setAuditStatus(auditStatus);
        if (auditStatus == 1) {
            spu.setShelfStatus(1);
        }
        updateById(spu);
    }

    @Override
    public void updateShelfStatus(Long spuId, Integer shelfStatus) {
        Spu spu = getSpuById(spuId);
        if (spu.getAuditStatus() != 1) {
            throw new BizException(ResultCode.PRODUCT_NOT_APPROVED);
        }
        spu.setShelfStatus(shelfStatus);
        updateById(spu);
    }

    @Override
    public Page<Spu> listByShop(Long shopId, int page, int size) {
        LambdaQueryWrapper<Spu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Spu::getShopId, shopId)
               .eq(Spu::getIsDeleted, 0)
               .orderByDesc(Spu::getCreatedAt);
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public Page<Spu> listByCategory(Long categoryId, int page, int size, String sort) {
        LambdaQueryWrapper<Spu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Spu::getCategoryId, categoryId)
               .eq(Spu::getAuditStatus, 1)
               .eq(Spu::getShelfStatus, 1)
               .eq(Spu::getIsDeleted, 0);
        if ("price_asc".equals(sort)) {
            wrapper.orderByAsc(Spu::getCreatedAt);
        } else {
            wrapper.orderByDesc(Spu::getCreatedAt);
        }
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public Page<Spu> searchByName(String keyword, int page, int size) {
        LambdaQueryWrapper<Spu> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(Spu::getName, keyword)
               .eq(Spu::getIsDeleted, 0)
               .orderByDesc(Spu::getCreatedAt);
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public Page<Spu> listForAdmin(Integer auditStatus, String keyword, Long categoryId, int page, int size) {
        LambdaQueryWrapper<Spu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Spu::getIsDeleted, 0);
        if (auditStatus != null) {
            wrapper.eq(Spu::getAuditStatus, auditStatus);
        }
        if (categoryId != null) {
            wrapper.eq(Spu::getCategoryId, categoryId);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Spu::getName, keyword);
        }
        wrapper.orderByDesc(Spu::getCreatedAt);
        Page<Spu> result = page(new Page<>(page, size), wrapper);
        populatePrices(result.getRecords());
        return result;
    }

    private void populatePrices(List<Spu> spuList) {
        if (spuList.isEmpty()) return;
        List<Long> spuIds = spuList.stream().map(Spu::getId).toList();
        List<Sku> allSkus = skuMapper.selectList(new LambdaQueryWrapper<Sku>().in(Sku::getSpuId, spuIds));
        Map<Long, List<Sku>> skuMap = allSkus.stream().collect(Collectors.groupingBy(Sku::getSpuId));
        for (Spu spu : spuList) {
            List<Sku> skus = skuMap.getOrDefault(spu.getId(), List.of());
            if (!skus.isEmpty()) {
                spu.setMinPrice(skus.stream().map(Sku::getPrice).min(java.math.BigDecimal::compareTo).orElse(java.math.BigDecimal.ZERO));
                spu.setMaxPrice(skus.stream().map(Sku::getPrice).max(java.math.BigDecimal::compareTo).orElse(java.math.BigDecimal.ZERO));
            }
        }
    }

    @Override
    public void updateSpu(Long spuId, UpdateProductRequest request) {
        Spu spu = getById(spuId);
        if (spu == null || spu.getIsDeleted() == 1) throw new BizException(ResultCode.PRODUCT_NOT_FOUND);
        if (request.getName() != null) spu.setName(request.getName());
        if (request.getSubtitle() != null) spu.setSubtitle(request.getSubtitle());
        if (request.getMainImage() != null) spu.setMainImage(request.getMainImage());
        if (request.getImages() != null) spu.setImages(request.getImages());
        if (request.getDescription() != null) spu.setDescription(request.getDescription());
        updateById(spu);
    }

    @Override
    public void updateSku(Long skuId, UpdateSkuRequest request) {
        Sku sku = skuMapper.selectById(skuId);
        if (sku == null) throw new BizException(ResultCode.SKU_NOT_FOUND);
        if (request.getPrice() != null) sku.setPrice(request.getPrice());
        if (request.getMarketPrice() != null) sku.setMarketPrice(request.getMarketPrice());
        if (request.getImage() != null) sku.setImage(request.getImage());
        if (request.getStatus() != null) sku.setStatus(request.getStatus());
        skuMapper.updateById(sku);
    }
}
