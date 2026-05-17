package com.supermarket.file.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supermarket.common.core.exception.BizException;
import com.supermarket.common.core.result.ResultCode;
import com.supermarket.file.entity.FileRecord;
import com.supermarket.file.mapper.FileRecordMapper;
import com.supermarket.file.service.FileService;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final FileRecordMapper fileRecordMapper;
    private final MinioClient minioClient;

    @Value("${minio.endpoint:http://localhost:19000}")
    private String minioEndpoint;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );
    private static final long MAX_SIZE = 10 * 1024 * 1024; // 10MB

    @Override
    public FileRecord upload(MultipartFile file, String bucket, Long uploaderId) {
        if (file.isEmpty()) throw new BizException(ResultCode.FILE_EMPTY);
        if (file.getSize() > MAX_SIZE) throw new BizException(ResultCode.FILE_SIZE_EXCEEDED);

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new BizException(ResultCode.FILE_TYPE_NOT_ALLOWED, ResultCode.FILE_TYPE_NOT_ALLOWED.getMessage() + ": " + contentType);
        }

        bucket = (bucket != null && !bucket.isBlank()) ? bucket : "smt-product";
        String originalName = file.getOriginalFilename();
        String ext = FileUtil.extName(originalName != null ? originalName : "bin");
        String objectKey = UUID.randomUUID() + "." + ext;

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception e) {
            log.error("Read file bytes failed", e);
            throw new BizException(ResultCode.FILE_READ_FAILED);
        }

        String md5 = DigestUtil.md5Hex(bytes);
        FileRecord exist = fileRecordMapper.selectOne(
                new LambdaQueryWrapper<FileRecord>().eq(FileRecord::getMd5, md5));
        if (exist != null) return exist;

        try (InputStream is = new java.io.ByteArrayInputStream(bytes)) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket).object(objectKey)
                    .stream(is, bytes.length, -1)
                    .contentType(contentType).build());
        } catch (Exception e) {
            log.error("MinIO upload failed", e);
            throw new BizException(ResultCode.FILE_UPLOAD_FAILED);
        }

        FileRecord record = new FileRecord();
        record.setFileName(originalName);
        record.setFileSize(file.getSize());
        record.setMd5(md5);
        record.setBucket(bucket);
        record.setObjectKey(objectKey);
        record.setUrl(minioEndpoint + "/" + bucket + "/" + objectKey);
        record.setUploaderId(uploaderId);
        fileRecordMapper.insert(record);
        return record;
    }

    @Override
    public FileRecord getById(Long fileId) {
        FileRecord r = fileRecordMapper.selectById(fileId);
        if (r == null) throw new BizException(ResultCode.FILE_NOT_FOUND);
        return r;
    }

    @Override
    public Page<FileRecord> listByUploader(Long uploaderId, int page, int size) {
        return fileRecordMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<FileRecord>()
                        .eq(FileRecord::getUploaderId, uploaderId)
                        .orderByDesc(FileRecord::getCreatedAt));
    }

    @Override
    public void delete(Long fileId) {
        FileRecord r = getById(fileId);
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(r.getBucket()).object(r.getObjectKey()).build());
        } catch (Exception e) {
            log.warn("MinIO delete failed: {}", r.getObjectKey(), e);
        }
        fileRecordMapper.deleteById(fileId);
    }

    @Override
    public InputStream download(Long fileId) {
        FileRecord r = getById(fileId);
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(r.getBucket()).object(r.getObjectKey()).build());
        } catch (Exception e) {
            log.error("MinIO download failed", e);
            throw new BizException(ResultCode.FILE_DOWNLOAD_FAILED);
        }
    }
}
