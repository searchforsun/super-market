package com.supermarket.file.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supermarket.common.core.result.R;
import com.supermarket.file.entity.FileRecord;
import com.supermarket.file.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    public R<FileRecord> upload(@RequestParam MultipartFile file,
                                @RequestParam(required = false) String bucket,
                                @RequestParam(required = false) Long uploaderId) {
        return R.ok(fileService.upload(file, bucket, uploaderId));
    }

    @GetMapping("/{id}")
    public R<FileRecord> detail(@PathVariable Long id) {
        return R.ok(fileService.getById(id));
    }

    @GetMapping("/list")
    public R<Page<FileRecord>> list(@RequestParam Long uploaderId,
                                     @RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        return R.ok(fileService.listByUploader(uploaderId, page, size));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        fileService.delete(id);
        return R.ok();
    }
}
