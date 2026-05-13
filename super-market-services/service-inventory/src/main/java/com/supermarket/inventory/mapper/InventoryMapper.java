package com.supermarket.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supermarket.inventory.entity.Inventory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface InventoryMapper extends BaseMapper<Inventory> {

    @Update("UPDATE inventory SET available_stock = available_stock - #{quantity}, " +
            "locked_stock = locked_stock + #{quantity}, version = version + 1 " +
            "WHERE sku_id = #{skuId} AND available_stock >= #{quantity} AND version = #{version}")
    int deductStock(@Param("skuId") Long skuId,
                    @Param("quantity") int quantity,
                    @Param("version") int version);

    @Update("UPDATE inventory SET available_stock = available_stock + #{quantity}, " +
            "locked_stock = locked_stock - #{quantity}, version = version + 1 " +
            "WHERE sku_id = #{skuId} AND locked_stock >= #{quantity} AND version = #{version}")
    int restoreStock(@Param("skuId") Long skuId,
                     @Param("quantity") int quantity,
                     @Param("version") int version);

    @Update("UPDATE inventory SET locked_stock = locked_stock - #{quantity}, " +
            "total_stock = total_stock - #{quantity}, version = version + 1 " +
            "WHERE sku_id = #{skuId} AND locked_stock >= #{quantity} AND version = #{version}")
    int confirmDeduct(@Param("skuId") Long skuId,
                      @Param("quantity") int quantity,
                      @Param("version") int version);
}
