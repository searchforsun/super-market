package com.supermarket.file.integration;

import com.supermarket.file.FileApplication;
import io.minio.MinioClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * REAL HTTP integration tests for File domain.
 * Uses TestRestTemplate (RANDOM_PORT) + real MySQL.
 * MinioClient is mocked since MinIO storage is typically not available in test environments.
 */
@SpringBootTest(classes = FileApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
@MockBean(MinioClient.class)
@DisplayName("File HTTP Integration Test")
class FileIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("Upload file -> get file info -> delete file")
    void shouldCompleteFileFlow() {
        // ===============================================================
        // 1. Upload a file
        // ===============================================================
        String uniqueContent = "test-image-content-" + System.currentTimeMillis();
        byte[] fileContent = uniqueContent.getBytes(StandardCharsets.UTF_8);

        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(MediaType.IMAGE_PNG);

        ByteArrayResource fileResource = new ByteArrayResource(fileContent) {
            @Override
            public String getFilename() {
                return "test.png";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new HttpEntity<>(fileResource, fileHeaders));

        ResponseEntity<Map> uploadResp = restTemplate.postForEntity(
                "/api/file/upload", body, Map.class);
        assertThat(uploadResp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(uploadResp.getBody().get("code"))/*FIX:code now 0*/ .isEqualTo(0);

        Map data = (Map) uploadResp.getBody().get("data");
        assertThat(data).isNotNull();
        assertThat(data.get("fileName")).isEqualTo("test.png");
        assertThat(data.get("fileSize")).isEqualTo((long) fileContent.length);

        Long fileId = ((Number) data.get("id")).longValue();
        assertThat(fileId).isPositive();

        // ===============================================================
        // 2. Get file info by ID
        // ===============================================================
        ResponseEntity<Map> getResp = restTemplate.getForEntity(
                "/api/file/{id}", Map.class, fileId);
        assertThat(getResp.getBody().get("code"))/*FIX:code now 0*/ .isEqualTo(0);
        Map getData = (Map) getResp.getBody().get("data");
        assertThat(getData.get("id")).isEqualTo(fileId);
        assertThat(getData.get("fileName")).isEqualTo("test.png");

        // ===============================================================
        // 3. Delete the file
        // ===============================================================
        ResponseEntity<Map> deleteResp = restTemplate.exchange(
                "/api/file/{id}", HttpMethod.DELETE, null, Map.class, fileId);
        assertThat(deleteResp.getBody().get("code"))/*FIX:code now 0*/ .isEqualTo(0);

        // ===============================================================
        // 4. Verify deletion — getting the file should return 404 error
        // ===============================================================
        ResponseEntity<Map> afterDeleteResp = restTemplate.getForEntity(
                "/api/file/{id}", Map.class, fileId);
        // The controller returns R.fail with error message wrapped in R
        // Since the global exception handler catches BizException(404, ...)
        // the response should have a non-200 code
        assertThat(afterDeleteResp.getBody().get("code")).isNotEqualTo(0);
    }
}
