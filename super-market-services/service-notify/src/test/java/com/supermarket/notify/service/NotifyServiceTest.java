package com.supermarket.notify.service;

import com.supermarket.notify.entity.NotifyTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotifyServiceTest {

    @Autowired
    private NotifyService notifyService;

    @Test
    void shouldCreateTemplate() {
        NotifyTemplate tpl = new NotifyTemplate();
        tpl.setCode("ORDER_CREATED");
        tpl.setName("订单创建通知");
        tpl.setChannel(1);
        tpl.setTitle("订单已创建");
        tpl.setContent("您的订单 ${orderNo} 已创建");
        NotifyTemplate saved = notifyService.createTemplate(tpl);
        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void shouldSendNotification() {
        NotifyTemplate tpl = new NotifyTemplate();
        tpl.setCode("TEST_NOTIFY");
        tpl.setName("测试通知");
        tpl.setChannel(1);
        tpl.setTitle("测试");
        tpl.setContent("您好 ${name}");
        notifyService.createTemplate(tpl);

        notifyService.send(1L, "TEST_NOTIFY", Map.of("name", "张三"));

        var page = notifyService.listByUser(1L, 1, 10);
        assertThat(page.getTotal()).isEqualTo(1);
    }

    @Test
    void shouldCountUnread() {
        notifyService.sendDirect(2L, "新消息", "内容", 1, null);
        long count = notifyService.unreadCount(2L);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void shouldMarkRead() {
        notifyService.sendDirect(3L, "消息", "内容", 1, null);
        var page = notifyService.listByUser(3L, 1, 10);
        Long id = page.getRecords().get(0).getId();

        notifyService.markRead(id);
        long count = notifyService.unreadCount(3L);
        assertThat(count).isEqualTo(0);
    }

    @Test
    void shouldMarkAllRead() {
        notifyService.sendDirect(4L, "消息1", "c1", 1, null);
        notifyService.sendDirect(4L, "消息2", "c2", 1, null);
        notifyService.markAllRead(4L);
        assertThat(notifyService.unreadCount(4L)).isEqualTo(0);
    }
}
