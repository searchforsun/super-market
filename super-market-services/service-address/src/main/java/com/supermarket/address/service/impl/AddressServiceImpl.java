package com.supermarket.address.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supermarket.address.entity.Address;
import com.supermarket.address.mapper.AddressMapper;
import com.supermarket.address.service.AddressService;
import com.supermarket.common.core.exception.BizException;
import com.supermarket.common.core.result.ResultCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddressServiceImpl extends ServiceImpl<AddressMapper, Address> implements AddressService {

    @Override
    @Transactional
    public Address create(Address address) {
        List<Address> existing = listByUser(address.getUserId());
        if (existing.size() >= 20) {
            throw new BizException(ResultCode.ADDRESS_LIMIT);
        }
        if (existing.isEmpty()) {
            address.setIsDefault(1);
        }
        save(address);
        return address;
    }

    @Override
    public Address update(Address address) {
        Address exist = getById(address.getId());
        if (exist == null) {
            throw new BizException(ResultCode.ADDRESS_NOT_FOUND);
        }
        updateById(address);
        return getById(address.getId());
    }

    @Override
    public void delete(Long addressId, Long userId) {
        LambdaUpdateWrapper<Address> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Address::getId, addressId)
               .eq(Address::getUserId, userId)
               .set(Address::getIsDeleted, 1);
        update(wrapper);
    }

    @Override
    public Address getById(Long addressId) {
        Address addr = super.getById(addressId);
        if (addr == null || addr.getIsDeleted() == 1) {
            throw new BizException(ResultCode.ADDRESS_NOT_FOUND);
        }
        return addr;
    }

    @Override
    public List<Address> listByUser(Long userId) {
        LambdaQueryWrapper<Address> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Address::getUserId, userId)
               .eq(Address::getIsDeleted, 0)
               .orderByDesc(Address::getIsDefault)
               .orderByDesc(Address::getCreatedAt);
        return list(wrapper);
    }

    @Override
    @Transactional
    public void setDefault(Long addressId, Long userId) {
        LambdaUpdateWrapper<Address> clearWrapper = new LambdaUpdateWrapper<>();
        clearWrapper.eq(Address::getUserId, userId)
                    .set(Address::getIsDefault, 0);
        update(clearWrapper);

        LambdaUpdateWrapper<Address> setWrapper = new LambdaUpdateWrapper<>();
        setWrapper.eq(Address::getId, addressId)
                 .eq(Address::getUserId, userId)
                 .set(Address::getIsDefault, 1);
        update(setWrapper);
    }
}
