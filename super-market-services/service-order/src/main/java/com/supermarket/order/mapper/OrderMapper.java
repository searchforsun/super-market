package com.supermarket.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supermarket.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    @Select("SELECT DATE(created_at) as date, COALESCE(SUM(actual_amount),0) as gmv, COUNT(*) as orderCount " +
            "FROM orders WHERE created_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY) " +
            "GROUP BY DATE(created_at) ORDER BY date")
    List<Map<String, Object>> selectDailyTrend(@Param("days") int days);

    @Select("SELECT DATE(created_at) as date, COALESCE(SUM(actual_amount),0) as gmv, COUNT(*) as orderCount " +
            "FROM orders WHERE shop_id = #{shopId} AND created_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY) " +
            "GROUP BY DATE(created_at) ORDER BY date")
    List<Map<String, Object>> selectMerchantDailyTrend(@Param("shopId") Long shopId, @Param("days") int days);

    @Select("SELECT CASE order_status " +
            "WHEN 1 THEN '待支付' WHEN 2 THEN '待发货' WHEN 3 THEN '已发货' " +
            "WHEN 4 THEN '已送达' WHEN 5 THEN '已取消' WHEN 6 THEN '退款中' ELSE '未知' END as name, " +
            "order_status as status, COUNT(*) as value " +
            "FROM orders GROUP BY order_status ORDER BY order_status")
    List<Map<String, Object>> selectStatusDistribution();

    @Select("SELECT CASE order_status " +
            "WHEN 1 THEN '待支付' WHEN 2 THEN '待发货' WHEN 3 THEN '已发货' " +
            "WHEN 4 THEN '已送达' WHEN 5 THEN '已取消' WHEN 6 THEN '退款中' ELSE '未知' END as name, " +
            "order_status as status, COUNT(*) as value " +
            "FROM orders WHERE shop_id = #{shopId} GROUP BY order_status ORDER BY order_status")
    List<Map<String, Object>> selectMerchantStatusDistribution(@Param("shopId") Long shopId);

    @Select("SELECT COALESCE(SUM(actual_amount),0) as gmv, COUNT(*) as orderCount " +
            "FROM orders WHERE DATE(created_at) = CURDATE()")
    Map<String, Object> selectTodayStats();

    @Select("SELECT COALESCE(SUM(actual_amount),0) as gmv, COUNT(*) as orderCount " +
            "FROM orders WHERE shop_id = #{shopId} AND DATE(created_at) = CURDATE()")
    Map<String, Object> selectMerchantTodayStats(@Param("shopId") Long shopId);
}
