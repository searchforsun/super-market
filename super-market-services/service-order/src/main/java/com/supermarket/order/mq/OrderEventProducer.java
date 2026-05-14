package com.supermarket.order.mq;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderEventProducer {

    @Autowired(required = false)
    private RocketMQTemplate rocketMQTemplate;

    private static final String ORDER_EVENT_TOPIC = "order-event-topic";
    private static final String DELAY_ORDER_TOPIC = "delay-order-topic";

    public void sendOrderCreated(String orderNo) {
        send(ORDER_EVENT_TOPIC, "ORDER_CREATED", orderNo);
    }

    public void sendOrderTimeoutCheck(String orderNo) {
        if (rocketMQTemplate == null) return;
        Message<String> msg = MessageBuilder.withPayload(orderNo).build();
        rocketMQTemplate.syncSend(DELAY_ORDER_TOPIC, msg, 3000, 16);
        log.info("发送延迟消息: orderNo={}, delayLevel=16(30min)", orderNo);
    }

    public void sendOrderPaid(String orderNo) {
        send(ORDER_EVENT_TOPIC, "ORDER_PAID", orderNo);
    }

    public void sendOrderCancelled(String orderNo) {
        send(ORDER_EVENT_TOPIC, "ORDER_CANCELLED", orderNo);
    }

    private void send(String topic, String tag, String body) {
        if (rocketMQTemplate == null) return;
        Message<String> msg = MessageBuilder.withPayload(body).build();
        rocketMQTemplate.syncSend(topic + ":" + tag, msg);
        log.info("发送消息: topic={}, tag={}", topic, tag);
    }
}
