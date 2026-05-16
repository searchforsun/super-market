package com.supermarket.file.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supermarket.common.core.result.R;
import com.supermarket.file.entity.FileRecord;
import com.supermarket.file.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Validated
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
@Tag(name = "文件服务", description = "文件上传、查询、删除接口")
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    @Operation(summary = "上传文件")
    public R<FileRecord> upload(@Parameter(description = "文件") @RequestParam MultipartFile file,
                                @Parameter(description = "存储桶名称") @RequestParam(required = false) String bucket,
                                @Parameter(description = "上传者ID") @RequestParam(required = false) Long uploaderId) {
        return R.ok(fileService.upload(file, bucket, uploaderId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取文件详情")
    public R<FileRecord> detail(@Parameter(description = "文件记录ID") @PathVariable Long id) {
        return R.ok(fileService.getById(id));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "下载文件")
    public ResponseEntity<byte[]> download(@PathVariable Long id) throws IOException {
        var record = fileService.getById(id);
        try (InputStream in = fileService.download(id)) {
            byte[] data = in.readAllBytes();
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + URLEncoder.encode(record.getFileName(), StandardCharsets.UTF_8) + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
        }
    }

    @GetMapping("/list")
    @Operation(summary = "分页查询上传文件列表")
    public R<Page<FileRecord>> list(@Parameter(description = "上传者ID") @RequestParam Long uploaderId,
                                     @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
                                     @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size) {
        return R.ok(fileService.listByUploader(uploaderId, page, size));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除文件")
    public R<Void> delete(@Parameter(description = "文件记录ID") @PathVariable Long id) {
        fileService.delete(id);
        return R.ok();
    }
}
