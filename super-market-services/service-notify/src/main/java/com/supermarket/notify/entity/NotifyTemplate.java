package com.supermarket.notify.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notify_templates")
public class NotifyTemplate {
    private Long id;
    private String code;
    private String name;
    private Integer channel;
    private String title;
    private String content;
    private Integer status;
    private LocalDateTime createdAt;
}
