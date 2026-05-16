package com.supermarket.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supermarket.auth.entity.Role;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {
}
