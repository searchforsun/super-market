package com.supermarket.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supermarket.common.core.exception.BizException;
import com.supermarket.common.core.result.ResultCode;
import com.supermarket.inventory.entity.Inventory;
import com.supermarket.inventory.mapper.InventoryMapper;
import com.supermarket.common.dubbo.api.inventory.InventoryDubboService;
import com.supermarket.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Slf4j
@DubboService(interfaceClass = InventoryDubboService.class)
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class InventoryServiceImpl extends ServiceImpl<InventoryMapper, Inventory> implements InventoryService, InventoryDubboService {

    private final RedissonClient redissonClient;

    @Override
    @Transactional
    public Inventory initStock(Long skuId, int totalStock, int safetyStock) {
        Inventory exist = getBySkuId(skuId);
        if (exist != null) {
            throw new BizException(ResultCode.STOCK_ALREADY_INIT);
        }
        Inventory inv = new Inventory();
        inv.setSkuId(skuId);
        inv.setTotalStock(totalStock);
        inv.setAvailableStock(totalStock);
        inv.setLockedStock(0);
        inv.setSafetyStock(safetyStock);
        inv.setVersion(0);
        save(inv);
        return inv;
    }

    @Override
    public Inventory getBySkuId(Long skuId) {
        return getOne(new LambdaQueryWrapper<Inventory>().eq(Inventory::getSkuId, skuId));
    }

    @Override
    @Transactional
    public boolean deductStock(Long skuId, int quantity) {
        String lockKey = "smt:lock:inventory:deduct:" + skuId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(3, 10, TimeUnit.SECONDS)) {
                log.warn("获取库存锁失败: skuId={}", skuId);
                return false;
            }
            Inventory inv = getBySkuId(skuId);
            if (inv == null || inv.getAvailableStock() < quantity) {
                return false;
            }
            int rows = baseMapper.deductStock(skuId, quantity, inv.getVersion());
            if (rows == 0) {
                log.warn("库存扣减失败(乐观锁冲突): skuId={}, version={}", skuId, inv.getVersion());
                return false;
            }
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    @Transactional
    public boolean restoreStock(Long skuId, int quantity) {
        Inventory inv = getBySkuId(skuId);
        if (inv == null || inv.getLockedStock() < quantity) {
            return false;
        }
        return baseMapper.restoreStock(skuId, quantity, inv.getVersion()) > 0;
    }

    @Override
    @Transactional
    public boolean confirmDeduct(Long skuId, int quantity) {
        Inventory inv = getBySkuId(skuId);
        if (inv == null || inv.getLockedStock() < quantity) {
            return false;
        }
        return baseMapper.confirmDeduct(skuId, quantity, inv.getVersion()) > 0;
    }

    // -- InventoryDubboService impl --

    @Override
    public boolean deduct(Long skuId, int quantity) {
        return deductStock(skuId, quantity);
    }

    @Override
    public boolean restore(Long skuId, int quantity) {
        return restoreStock(skuId, quantity);
    }
}
