package com.supermarket.notify.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supermarket.common.web.handler.GlobalExceptionHandler;
import com.supermarket.notify.entity.Notification;
import com.supermarket.notify.entity.NotifyTemplate;
import com.supermarket.notify.service.NotifyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {NotifyController.class, NotifyControllerTest.TestConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotifyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotifyService notifyService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Configuration
    @EnableAutoConfiguration
    @ComponentScan(basePackageClasses = NotifyController.class)
    @Import(GlobalExceptionHandler.class)
    static class TestConfig {
    }

    @Test
    void shouldReturnNotifyListWhenUserIdProvided() throws Exception {
        // Arrange
        Page<Notification> page = new Page<>(1, 20);
        Notification notification = new Notification();
        notification.setId(1L);
        notification.setUserId(1L);
        notification.setTitle("订单通知");
        notification.setContent("您的订单已发货");
        page.setRecords(List.of(notification));
        page.setTotal(1);

        when(notifyService.listByUser(1L, 1, 20)).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/notify/list")
                .param("userId", "1")
                .param("page", "1")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records[0].title").value("订单通知"))
                .andExpect(jsonPath("$.data.total").value(1));

        verify(notifyService).listByUser(1L, 1, 20);
    }

    @Test
    void shouldReturnUnreadCountWhenUserIdProvided() throws Exception {
        // Arrange
        when(notifyService.unreadCount(1L)).thenReturn(5L);

        // Act & Assert
        mockMvc.perform(get("/api/notify/unread-count")
                .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(5));

        verify(notifyService).unreadCount(1L);
    }

    @Test
    void shouldMarkReadWhenIdProvided() throws Exception {
        // Arrange
        doNothing().when(notifyService).markRead(1L);

        // Act & Assert
        mockMvc.perform(put("/api/notify/1/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"));

        verify(notifyService).markRead(1L);
    }

    @Test
    void shouldMarkAllReadWhenUserIdProvided() throws Exception {
        // Arrange
        doNothing().when(notifyService).markAllRead(1L);

        // Act & Assert
        mockMvc.perform(put("/api/notify/read-all")
                .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"));

        verify(notifyService).markAllRead(1L);
    }

    @Test
    void shouldCreateTemplateWhenRequestIsValid() throws Exception {
        // Arrange
        NotifyTemplate template = new NotifyTemplate();
        template.setCode("ORDER_SHIPPED");
        template.setName("订单发货通知");
        template.setTitle("订单通知");
        template.setContent("您的订单已发货");

        NotifyTemplate created = new NotifyTemplate();
        created.setId(1L);
        created.setCode("ORDER_SHIPPED");
        created.setStatus(1);

        when(notifyService.createTemplate(any(NotifyTemplate.class))).thenReturn(created);

        // Act & Assert
        mockMvc.perform(post("/api/notify/admin/template")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(template)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.code").value("ORDER_SHIPPED"));

        verify(notifyService).createTemplate(any(NotifyTemplate.class));
    }

    @Test
    void shouldSendWhenRequestIsValid() throws Exception {
        // Arrange
        Map<String, String> params = new HashMap<>();
        params.put("orderNo", "ORD20260516001");
        params.put("amount", "299.00");

        doNothing().when(notifyService).send(1L, "ORDER_PAID", params);

        // Act & Assert
        mockMvc.perform(post("/api/notify/send")
                .header("X-Source-Service", "order-service")
                .param("templateCode", "ORDER_PAID")
                .param("userId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"));

        verify(notifyService).send(1L, "ORDER_PAID", params);
    }
}
