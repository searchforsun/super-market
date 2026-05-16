package com.supermarket.notify.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supermarket.common.core.exception.BizException;
import com.supermarket.notify.entity.Notification;
import com.supermarket.notify.entity.NotifyTemplate;
import com.supermarket.notify.mapper.NotificationMapper;
import com.supermarket.notify.mapper.NotifyTemplateMapper;
import com.supermarket.notify.service.NotifyService;
import com.supermarket.notify.service.email.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
public class NotifyServiceImpl implements NotifyService {

    private final NotificationMapper notificationMapper;
    private final NotifyTemplateMapper templateMapper;

    /**
     * 生产环境替换为 AliyunEmailService 或 TencentCloudEmailService
     * 开发环境自动注入 {@link com.supermarket.notify.service.email.MockEmailService}
     */
    @Autowired
    private EmailService emailService;

    public NotifyServiceImpl(NotificationMapper notificationMapper, NotifyTemplateMapper templateMapper) {
        this.notificationMapper = notificationMapper;
        this.templateMapper = templateMapper;
    }

    @Override
    public NotifyTemplate createTemplate(NotifyTemplate template) {
        templateMapper.insert(template);
        return template;
    }

    @Override
    public void send(Long userId, String templateCode, Map<String, String> params) {
        NotifyTemplate tpl = templateMapper.selectOne(
                new LambdaQueryWrapper<NotifyTemplate>().eq(NotifyTemplate::getCode, templateCode));
        if (tpl == null) {
            log.warn("通知模板不存在: {}", templateCode);
            return;
        }

        String title = render(tpl.getTitle(), params);
        String content = render(tpl.getContent(), params);

        Notification notif = new Notification();
        notif.setUserId(userId);
        notif.setTemplateId(tpl.getId());
        notif.setChannel(tpl.getChannel());
        notif.setTitle(title);
        notif.setContent(content);
        notif.setStatus(0);
        notif.setSendStatus(1);
        notif.setSendAt(LocalDateTime.now());
        notificationMapper.insert(notif);

        // 邮件通道（开发环境走 Mock 日志打印，生产对接阿里云/腾讯云）
        if (tpl.getChannel() == 2) {
            String email = params.get("email");
            if (email != null && !email.isBlank()) {
                try {
                    emailService.send(email, title, content);
                    notif.setSendStatus(1);
                } catch (Exception e) {
                    log.error("邮件发送失败: {}", email, e);
                    notif.setSendStatus(2);
                }
                notificationMapper.updateById(notif);
            }
        }
    }

    @Override
    public void sendDirect(Long userId, String title, String content, Integer channel, String target) {
        Notification notif = new Notification();
        notif.setUserId(userId);
        notif.setChannel(channel != null ? channel : 1);
        notif.setTitle(title);
        notif.setContent(content);
        notif.setTarget(target);
        notif.setStatus(0);
        notif.setSendStatus(1);
        notif.setSendAt(LocalDateTime.now());
        notificationMapper.insert(notif);
    }

    @Override
    public Page<Notification> listByUser(Long userId, int page, int size) {
        return notificationMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .orderByDesc(Notification::getCreatedAt));
    }

    @Override
    public long unreadCount(Long userId) {
        return notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getStatus, 0));
    }

    @Override
    public void markRead(Long notifyId) {
        Notification n = notificationMapper.selectById(notifyId);
        if (n == null) throw new BizException(404, "通知不存在");
        n.setStatus(1);
        n.setReadAt(LocalDateTime.now());
        notificationMapper.updateById(n);
    }

    @Override
    public void markAllRead(Long userId) {
        notificationMapper.readAll(userId);
    }

    private String render(String template, Map<String, String> params) {
        if (template == null) return "";
        String result = template;
        for (var entry : params.entrySet()) {
            result = result.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}
