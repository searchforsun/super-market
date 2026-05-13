package com.supermarket.notify.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supermarket.common.core.result.R;
import com.supermarket.notify.entity.Notification;
import com.supermarket.notify.entity.NotifyTemplate;
import com.supermarket.notify.service.NotifyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notify")
@RequiredArgsConstructor
public class NotifyController {

    private final NotifyService notifyService;

    @GetMapping("/list")
    public R<Page<Notification>> list(@RequestParam Long userId,
                                       @RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        return R.ok(notifyService.listByUser(userId, page, size));
    }

    @GetMapping("/unread-count")
    public R<Long> unreadCount(@RequestParam Long userId) {
        return R.ok(notifyService.unreadCount(userId));
    }

    @PutMapping("/{id}/read")
    public R<Void> markRead(@PathVariable Long id) {
        notifyService.markRead(id);
        return R.ok();
    }

    @PutMapping("/read-all")
    public R<Void> markAllRead(@RequestParam Long userId) {
        notifyService.markAllRead(userId);
        return R.ok();
    }

    @PostMapping("/admin/template")
    public R<NotifyTemplate> createTemplate(@RequestBody NotifyTemplate template) {
        return R.ok(notifyService.createTemplate(template));
    }
}
