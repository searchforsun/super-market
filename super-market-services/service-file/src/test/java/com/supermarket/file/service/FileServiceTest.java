package com.supermarket.file.service;

import com.supermarket.common.core.exception.BizException;
import com.supermarket.file.entity.FileRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FileServiceTest {

    @Autowired
    private FileService fileService;

    @MockBean
    private io.minio.MinioClient minioClient;

    @Test
    void shouldGetFileById() {
        assertThatThrownBy(() -> fileService.getById(99999L))
                .isInstanceOf(BizException.class);
    }

    @Test
    void shouldListByUploader() {
        var page = fileService.listByUploader(1L, 1, 20);
        assertThat(page).isNotNull();
        assertThat(page.getTotal()).isEqualTo(0);
    }

    @Test
    void shouldThrowWhenFileNotFound() {
        assertThatThrownBy(() -> fileService.delete(99999L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("文件不存在");
    }
}
