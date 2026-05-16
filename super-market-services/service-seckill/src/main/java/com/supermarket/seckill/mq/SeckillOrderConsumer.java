package com.supermarket.seckill.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supermarket.common.dubbo.api.order.OrderDubboService;
import com.supermarket.common.dubbo.api.order.dto.CreateOrderRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(RocketMQTemplate.class)
@RocketMQMessageListener(
    topic = "seckill-order-topic",
    consumerGroup = "seckill-order-consumer-group"
)
public class SeckillOrderConsumer implements RocketMQListener<String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @DubboReference(check = false)
    private OrderDubboService orderDubboService;

    @Override
    @SuppressWarnings("unchecked")
    public void onMessage(String messageJson) {
        log.info("收到秒杀订单消息: {}", messageJson);
        try {
            Map<String, Object> msg = MAPPER.readValue(messageJson, Map.class);
            Long userId = Long.valueOf(msg.get("userId").toString());
            Long skuId = Long.valueOf(msg.get("skuId").toString());
            BigDecimal price = new BigDecimal(msg.get("price").toString());
            int quantity = Integer.parseInt(msg.get("quantity").toString());

            CreateOrderRequest request = new CreateOrderRequest();
            request.setUserId(userId);
            request.setItems(List.of(buildItem(skuId, price, quantity)));

            orderDubboService.createOrder(request);
            log.info("秒杀订单创建成功: userId={}, skuId={}", userId, skuId);
        } catch (Exception e) {
            log.error("秒杀订单创建失败: message={}", messageJson, e);
        }
    }

    private CreateOrderRequest.OrderItemRequest buildItem(Long skuId, BigDecimal price, int quantity) {
        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
        item.setSkuId(skuId);
        item.setSkuPrice(price);
        item.setQuantity(quantity);
        return item;
    }
}
