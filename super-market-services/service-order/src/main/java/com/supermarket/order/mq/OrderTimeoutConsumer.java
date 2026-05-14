package com.supermarket.order.mq;

import com.supermarket.order.entity.Order;
import com.supermarket.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(RocketMQTemplate.class)
@RocketMQMessageListener(
    topic = "delay-order-topic",
    consumerGroup = "order-consumer-group"
)
public class OrderTimeoutConsumer implements RocketMQListener<String> {

    private final OrderService orderService;

    @Override
    public void onMessage(String orderNo) {
        log.info("收到延迟消息: orderNo={}", orderNo);
        try {
            Order order = orderService.getByOrderNo(orderNo);
            if (order.getOrderStatus() == 1 && order.getExpireTime().isBefore(LocalDateTime.now())) {
                orderService.cancelOrder(orderNo, "支付超时自动取消");
                log.info("超时订单已取消: orderNo={}", orderNo);
            }
        } catch (Exception e) {
            log.error("处理超时订单失败: orderNo={}", orderNo, e);
        }
    }
}
