package com.supermarket.file.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supermarket.file.entity.FileRecord;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface FileService {

    FileRecord upload(MultipartFile file, String bucket, Long uploaderId);

    FileRecord getById(Long fileId);

    Page<FileRecord> listByUploader(Long uploaderId, int page, int size);

    void delete(Long fileId);

    InputStream download(Long fileId);

    InputStream downloadRaw(String bucket, String objectKey);
}
