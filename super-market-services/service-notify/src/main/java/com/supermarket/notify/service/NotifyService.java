package com.supermarket.notify.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supermarket.notify.entity.Notification;
import com.supermarket.notify.entity.NotifyTemplate;

import java.util.Map;

public interface NotifyService {

    NotifyTemplate createTemplate(NotifyTemplate template);

    void send(Long userId, String templateCode, Map<String, String> params);

    void sendDirect(Long userId, String title, String content, Integer channel, String target);

    Page<Notification> listByUser(Long userId, int page, int size);

    long unreadCount(Long userId);

    void markRead(Long notifyId);

    void markAllRead(Long userId);
}
