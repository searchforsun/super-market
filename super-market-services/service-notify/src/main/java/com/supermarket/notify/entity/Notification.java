package com.supermarket.notify.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notifications")
public class Notification {
    private Long id;
    private Long userId;
    private Long templateId;
    private Integer channel;
    private String title;
    private String content;
    private String target;
    private Integer status;
    private Integer sendStatus;
    private LocalDateTime sendAt;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
