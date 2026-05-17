package com.supermarket.common.dubbo.api.order;

import com.supermarket.common.dubbo.api.order.dto.CreateOrderRequest;

import java.util.List;
import java.util.Map;

public interface OrderDubboService {

    void updateStatus(String orderNo, Integer toStatus);

    void placeOrder(CreateOrderRequest request);

    /** 近N日销售趋势（日期、GMV、订单数） */
    List<Map<String, Object>> getDailyTrend(int days);

    /** 订单状态分布（状态名、数量） */
    List<Map<String, Object>> getOrderStatusDistribution();

    /** 分类销售占比（分类名、销售金额、占比） */
    List<Map<String, Object>> getCategorySales();

    /** 商家近N日销售趋势 */
    List<Map<String, Object>> getMerchantDailyTrend(Long shopId, int days);

    /** 商家订单状态分布 */
    List<Map<String, Object>> getMerchantOrderStatusDistribution(Long shopId);

    /** 今日GMV统计（总GMV、订单数） */
    Map<String, Object> getTodayStats();

    /** 商家今日统计 */
    Map<String, Object> getMerchantTodayStats(Long shopId);
}
