package com.supermarket.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supermarket.common.core.exception.BizException;
import com.supermarket.product.dto.CreateProductRequest;
import com.supermarket.product.entity.Sku;
import com.supermarket.product.entity.Spu;
import com.supermarket.product.mapper.SkuMapper;
import com.supermarket.product.mapper.SpuMapper;
import com.supermarket.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

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
        spu.setImages(request.getImages() != null ? String.join(",", request.getImages()) : null);
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
            throw new BizException(404, "商品不存在");
        }
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
            throw new BizException(404, "SKU不存在");
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
            throw new BizException(400, "审核状态仅支持 1=通过 2=驳回");
        }
        Spu spu = getSpuById(spuId);
        if (spu.getAuditStatus() != 0) {
            throw new BizException(400, "该商品已审核");
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
            throw new BizException(400, "仅审核通过的商品可以上下架");
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
}
