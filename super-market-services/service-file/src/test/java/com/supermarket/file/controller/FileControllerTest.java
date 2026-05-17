package com.supermarket.file.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supermarket.common.web.handler.GlobalExceptionHandler;
import com.supermarket.file.entity.FileRecord;
import com.supermarket.file.service.FileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {FileController.class, FileControllerTest.TestConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileService fileService;

    @Configuration
    @EnableAutoConfiguration
    @ComponentScan(basePackageClasses = FileController.class)
    @Import(GlobalExceptionHandler.class)
    static class TestConfig {
    }

    @Test
    void shouldUploadFileWhenFileProvided() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", MediaType.TEXT_PLAIN_VALUE, "test content".getBytes());

        FileRecord record = new FileRecord();
        record.setId(1L);
        record.setFileName("test.txt");
        record.setFileSize(12L);
        record.setUrl("https://minio.example.com/test-bucket/test.txt");

        when(fileService.upload(any(), eq("test-bucket"), eq(1L))).thenReturn(record);

        // Act & Assert
        mockMvc.perform(multipart("/api/file/upload")
                .file(file)
                .param("bucket", "test-bucket")
                .param("uploaderId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.fileName").value("test.txt"))
                .andExpect(jsonPath("$.data.url").value("https://minio.example.com/test-bucket/test.txt"));

        verify(fileService).upload(any(), eq("test-bucket"), eq(1L));
    }

    @Test
    void shouldReturnFileDetailWhenIdProvided() throws Exception {
        // Arrange
        FileRecord record = new FileRecord();
        record.setId(1L);
        record.setFileName("test.txt");
        record.setFileSize(1024L);
        record.setBucket("default");

        when(fileService.getById(1L)).thenReturn(record);

        // Act & Assert
        mockMvc.perform(get("/api/file/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.fileName").value("test.txt"));

        verify(fileService).getById(1L);
    }

    @Test
    void shouldDownloadFileWhenIdProvided() throws Exception {
        // Arrange
        FileRecord record = new FileRecord();
        record.setId(1L);
        record.setFileName("test.txt");
        record.setFileSize(12L);

        when(fileService.getById(1L)).thenReturn(record);
        when(fileService.download(1L)).thenReturn(new ByteArrayInputStream("test content".getBytes()));

        // Act & Assert
        mockMvc.perform(get("/api/file/1/download"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("test.txt")))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, org.hamcrest.Matchers.startsWith(MediaType.APPLICATION_OCTET_STREAM_VALUE)));

        verify(fileService).getById(1L);
        verify(fileService).download(1L);
    }

    @Test
    void shouldReturnFileListWhenUploaderIdProvided() throws Exception {
        // Arrange
        Page<FileRecord> page = new Page<>(1, 20);
        FileRecord record = new FileRecord();
        record.setId(1L);
        record.setFileName("test.txt");
        record.setUploaderId(1L);
        page.setRecords(List.of(record));
        page.setTotal(1);

        when(fileService.listByUploader(1L, 1, 20)).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/file/list")
                .param("uploaderId", "1")
                .param("page", "1")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records[0].fileName").value("test.txt"))
                .andExpect(jsonPath("$.data.total").value(1));

        verify(fileService).listByUploader(1L, 1, 20);
    }

    @Test
    void shouldDeleteFileWhenIdProvided() throws Exception {
        // Arrange
        doNothing().when(fileService).delete(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/file/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"));

        verify(fileService).delete(1L);
    }
}
