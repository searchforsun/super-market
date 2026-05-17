package com.supermarket.notify.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supermarket.notify.NotifyApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * REAL HTTP integration tests for Notify domain.
 * Uses TestRestTemplate (RANDOM_PORT) + real MySQL.
 * MockEmailService is active by default (@ConditionalOnMissingBean), no middleware mocking needed.
 */
@SpringBootTest(classes = NotifyApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
@DisplayName("Notify HTTP Integration Test")
class NotifyIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Create template -> send notification -> list -> unread count -> mark read -> mark all read")
    void shouldCompleteNotifyFlow() {
        long uniqueId = System.currentTimeMillis();
        Long userId = 10000L + uniqueId % 10000;
        String templateCode = "WELCOME_" + uniqueId;

        // ===============================================================
        // 1. Create a notification template
        // ===============================================================
        String templateJson = String.format("""
                {
                    "code": "%s",
                    "name": "欢迎通知-%d",
                    "channel": 1,
                    "title": "欢迎 ${username}",
                    "content": "您好 ${username}，欢迎加入！",
                    "status": 1
                }
                """, templateCode, uniqueId);

        ResponseEntity<Map> createTplResp = restTemplate.postForEntity(
                "/api/notify/admin/template",
                createJsonEntity(templateJson),
                Map.class);
        assertThat(createTplResp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(createTplResp.getBody().get("code"))/*FIX:code now 0*/ .isEqualTo(0);
        Long templateId = extractLong(createTplResp.getBody(), "data.id");
        assertThat(templateId).isNotNull();

        // ===============================================================
        // 2. Send a notification using the template
        // ===============================================================
        HttpHeaders sendHeaders = new HttpHeaders();
        sendHeaders.setContentType(MediaType.APPLICATION_JSON);
        sendHeaders.set("X-Source-Service", "test-integration");

        Map<String, String> params = new HashMap<>();
        params.put("username", "测试用户");

        HttpEntity<Map<String, String>> sendEntity = new HttpEntity<>(params, sendHeaders);

        ResponseEntity<Map> sendResp = restTemplate.exchange(
                "/api/notify/send?templateCode={code}&userId={userId}",
                HttpMethod.POST, sendEntity, Map.class, templateCode, userId);
        assertThat(sendResp.getBody().get("code"))/*FIX:code now 0*/ .isEqualTo(0);

        // ===============================================================
        // 3. List notifications for the user
        // ===============================================================
        ResponseEntity<Map> listResp = restTemplate.getForEntity(
                "/api/notify/list?userId={userId}&page={page}&size={size}",
                Map.class, userId, 1, 20);
        assertThat(listResp.getBody().get("code"))/*FIX:code now 0*/ .isEqualTo(0);
        Map listData = (Map) listResp.getBody().get("data");
        List<Map> records = (List<Map>) listData.get("records");
        assertThat(records).isNotEmpty();

        // Verify the notification content
        Map notification = records.get(0);
        assertThat(notification.get("title")).isEqualTo("欢迎 测试用户");
        assertThat(notification.get("status")).isEqualTo(0); // 0 = unread
        Long notifyId = ((Number) notification.get("id")).longValue();

        // ===============================================================
        // 4. Check unread count
        // ===============================================================
        ResponseEntity<Map> unreadResp = restTemplate.getForEntity(
                "/api/notify/unread-count?userId={userId}",
                Map.class, userId);
        assertThat(unreadResp.getBody().get("code"))/*FIX:code now 0*/ .isEqualTo(0);
        assertThat(unreadResp.getBody().get("data")).isNotNull();
        assertThat(((Number) unreadResp.getBody().get("data")).longValue()).isPositive();

        // ===============================================================
        // 5. Mark single notification as read
        // ===============================================================
        ResponseEntity<Map> markReadResp = restTemplate.exchange(
                "/api/notify/{id}/read",
                HttpMethod.PUT, null, Map.class, notifyId);
        assertThat(markReadResp.getBody().get("code"))/*FIX:code now 0*/ .isEqualTo(0);

        // Verify status changed to read
        ResponseEntity<Map> verifyResp = restTemplate.getForEntity(
                "/api/notify/list?userId={userId}&page={page}&size={size}",
                Map.class, userId, 1, 20);
        Map updatedNotif = ((List<Map>) ((Map) verifyResp.getBody().get("data")).get("records")).get(0);
        assertThat(updatedNotif.get("status")).isEqualTo(1); // 1 = read

        // ===============================================================
        // 6. Mark all as read (create another notification first)
        // ===============================================================
        Map<String, String> params2 = new HashMap<>();
        params2.put("username", "用户B");
        HttpEntity<Map<String, String>> sendEntity2 = new HttpEntity<>(params2, sendHeaders);

        restTemplate.exchange(
                "/api/notify/send?templateCode={code}&userId={userId}",
                HttpMethod.POST, sendEntity2, Map.class, templateCode, userId);

        ResponseEntity<Map> markAllResp = restTemplate.exchange(
                "/api/notify/read-all?userId={userId}",
                HttpMethod.PUT, null, Map.class, userId);
        assertThat(markAllResp.getBody().get("code"))/*FIX:code now 0*/ .isEqualTo(0);

        // Verify all notifications are read
        ResponseEntity<Map> finalListResp = restTemplate.getForEntity(
                "/api/notify/list?userId={userId}&page={page}&size={size}",
                Map.class, userId, 1, 20);
        List<Map> finalRecords = (List<Map>) ((Map) finalListResp.getBody().get("data")).get("records");
        assertThat(finalRecords).isNotEmpty();
        assertThat(finalRecords.get(0).get("status")).isEqualTo(1);
    }

    // ---- helpers ----

    private HttpEntity<String> createJsonEntity(String json) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(json, headers);
    }

    private Long extractLong(Map body, String path) {
        String[] parts = path.split("\\.");
        Object node = body;
        for (String part : parts) {
            if (node instanceof Map) {
                node = ((Map<?, ?>) node).get(part);
            } else {
                return null;
            }
        }
        return node instanceof Number ? ((Number) node).longValue() : null;
    }
}
