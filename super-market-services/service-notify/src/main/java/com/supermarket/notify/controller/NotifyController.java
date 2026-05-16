package com.supermarket.notify.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supermarket.common.core.result.R;
import com.supermarket.notify.entity.Notification;
import com.supermarket.notify.entity.NotifyTemplate;
import com.supermarket.notify.service.NotifyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/notify")
@RequiredArgsConstructor
@Tag(name = "通知服务", description = "通知列表、未读计数、已读标记、模板管理接口")
public class NotifyController {

    private final NotifyService notifyService;

    @GetMapping("/list")
    @Operation(summary = "分页查询通知列表")
    public R<Page<Notification>> list(@Parameter(description = "用户ID") @RequestParam Long userId,
                                       @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
                                       @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size) {
        return R.ok(notifyService.listByUser(userId, page, size));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "获取未读通知数量")
    public R<Long> unreadCount(@Parameter(description = "用户ID") @RequestParam Long userId) {
        return R.ok(notifyService.unreadCount(userId));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "标记单条通知已读")
    public R<Void> markRead(@Parameter(description = "通知ID") @PathVariable Long id) {
        notifyService.markRead(id);
        return R.ok();
    }

    @PutMapping("/read-all")
    @Operation(summary = "标记所有通知已读")
    public R<Void> markAllRead(@Parameter(description = "用户ID") @RequestParam Long userId) {
        notifyService.markAllRead(userId);
        return R.ok();
    }

    @PostMapping("/admin/template")
    @Operation(summary = "创建通知模板")
    public R<NotifyTemplate> createTemplate(@Parameter(description = "通知模板信息") @RequestBody NotifyTemplate template) {
        return R.ok(notifyService.createTemplate(template));
    }

    @PostMapping("/send")
    @Operation(summary = "发送通知（内部服务调用）")
    public R<Void> send(
        @RequestHeader(value = "X-Source-Service", required = false) String sourceService,
        @RequestParam @NotBlank(message = "模板编码不能为空") String templateCode,
        @RequestParam @NotNull(message = "用户ID不能为空") Long userId,
        @RequestBody Map<String, String> params) {
        notifyService.send(userId, templateCode, params);
        return R.ok();
    }
}
