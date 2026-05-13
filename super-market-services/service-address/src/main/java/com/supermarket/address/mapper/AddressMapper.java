package com.supermarket.address.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supermarket.address.entity.Address;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AddressMapper extends BaseMapper<Address> {
}
