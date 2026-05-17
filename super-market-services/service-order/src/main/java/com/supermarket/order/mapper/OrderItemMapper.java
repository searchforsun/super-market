package com.supermarket.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supermarket.order.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {

    @Select("SELECT * FROM order_items WHERE order_no = #{orderNo}")
    List<OrderItem> selectByOrderNo(@Param("orderNo") String orderNo);

    @Select("SELECT oi.sku_name as categoryName, COALESCE(SUM(oi.total_price),0) as amount " +
            "FROM order_items oi JOIN orders o ON oi.order_no = o.order_no " +
            "WHERE o.created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
            "GROUP BY oi.sku_name ORDER BY amount DESC LIMIT 10")
    List<Map<String, Object>> selectCategorySales();
}
