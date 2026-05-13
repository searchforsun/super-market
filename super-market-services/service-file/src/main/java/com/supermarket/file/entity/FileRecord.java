package com.supermarket.file.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("file_records")
public class FileRecord {
    private Long id;
    private String fileName;
    private Long fileSize;
    private String md5;
    private String bucket;
    private String objectKey;
    private String url;
    private String thumbUrl;
    private Long uploaderId;
    private LocalDateTime createdAt;
}
