package com.supermarket.order.mq;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.Message;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class OrderEventProducerTest {

    private OrderEventProducer producer;
    private RocketMQTemplate rocketMQTemplate;

    @BeforeEach
    void setUp() throws Exception {
        rocketMQTemplate = mock(RocketMQTemplate.class);
        producer = new OrderEventProducer();
        Field field = OrderEventProducer.class.getDeclaredField("rocketMQTemplate");
        field.setAccessible(true);
        field.set(producer, rocketMQTemplate);
    }

    @Test
    void shouldSendOrderCreatedMessage() {
        String orderNo = "ORD20260516123456";

        producer.sendOrderCreated(orderNo);

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(rocketMQTemplate).syncSend(eq("order-event-topic:ORDER_CREATED"), messageCaptor.capture());
        assertEquals(orderNo, messageCaptor.getValue().getPayload());
    }

    @Test
    void shouldSendOrderTimeoutCheckMessage() {
        String orderNo = "ORD20260516123456";

        producer.sendOrderTimeoutCheck(orderNo);

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(rocketMQTemplate).syncSend(eq("delay-order-topic"), messageCaptor.capture(), eq(3000L), eq(16));
        assertEquals(orderNo, messageCaptor.getValue().getPayload());
    }

    @Test
    void shouldSendOrderPaidMessage() {
        String orderNo = "ORD20260516123456";

        producer.sendOrderPaid(orderNo);

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(rocketMQTemplate).syncSend(eq("order-event-topic:ORDER_PAID"), messageCaptor.capture());
        assertEquals(orderNo, messageCaptor.getValue().getPayload());
    }

    @Test
    void shouldSendOrderCancelledMessage() {
        String orderNo = "ORD20260516123456";

        producer.sendOrderCancelled(orderNo);

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(rocketMQTemplate).syncSend(eq("order-event-topic:ORDER_CANCELLED"), messageCaptor.capture());
        assertEquals(orderNo, messageCaptor.getValue().getPayload());
    }

    @Test
    void shouldNotThrowExceptionWhenTemplateIsNull() {
        OrderEventProducer nullProducer = new OrderEventProducer();

        assertDoesNotThrow(() -> nullProducer.sendOrderCreated("ORD123"));
        assertDoesNotThrow(() -> nullProducer.sendOrderTimeoutCheck("ORD123"));
        assertDoesNotThrow(() -> nullProducer.sendOrderPaid("ORD123"));
        assertDoesNotThrow(() -> nullProducer.sendOrderCancelled("ORD123"));
    }
}
