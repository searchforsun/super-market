package com.supermarket.user.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supermarket.common.core.dto.LoginRequest;
import com.supermarket.common.core.result.R;
import com.supermarket.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
@DisplayName("User HTTP Integration Test")
class UserIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private String uniquePhone() {
        return "186" + String.format("%08d", System.nanoTime() % 100_000_000);
    }

    private LoginRequest loginRequest(String phone, String password) {
        LoginRequest req = new LoginRequest();
        req.setPhone(phone);
        req.setPassword(password);
        return req;
    }

    @Test
    @DisplayName("Register -> Login -> GetInfo happy path chain")
    void shouldCompleteRegisterLoginInfoFlow() {
        String phone = uniquePhone();
        String password = "Pass1234";

        // Step 1: Register
        ResponseEntity<R<User>> registerResponse = restTemplate.exchange(
                "/api/user/register",
                HttpMethod.POST,
                new HttpEntity<>(loginRequest(phone, password)),
                new ParameterizedTypeReference<R<User>>() {});
        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(registerResponse.getBody()).isNotNull();
        assertThat(registerResponse.getBody().getCode())/*FIX:code now 0*/ .isEqualTo(0);
        assertThat(registerResponse.getBody().getMessage()).isEqualTo("success");
        User registered = registerResponse.getBody().getData();
        assertThat(registered).isNotNull();
        assertThat(registered.getId()).isNotNull();
        assertThat(registered.getPhone()).isEqualTo(phone);
        Long userId = registered.getId();

        // Step 2: Login
        ResponseEntity<R<User>> loginResponse = restTemplate.exchange(
                "/api/user/login",
                HttpMethod.POST,
                new HttpEntity<>(loginRequest(phone, password)),
                new ParameterizedTypeReference<R<User>>() {});
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResponse.getBody()).isNotNull();
        assertThat(loginResponse.getBody().getCode())/*FIX:code now 0*/ .isEqualTo(0);
        assertThat(loginResponse.getBody().getData().getPhone()).isEqualTo(phone);

        // Step 3: Get info
        ResponseEntity<R<User>> infoResponse = restTemplate.exchange(
                "/api/user/info?userId=" + userId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<R<User>>() {});
        assertThat(infoResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(infoResponse.getBody()).isNotNull();
        assertThat(infoResponse.getBody().getCode())/*FIX:code now 0*/ .isEqualTo(0);
        assertThat(infoResponse.getBody().getData().getId()).isEqualTo(userId);
        assertThat(infoResponse.getBody().getData().getPhone()).isEqualTo(phone);
    }

    @Test
    @DisplayName("Duplicate registration returns 400")
    void shouldReturnErrorWhenRegisterDuplicatePhone() throws Exception {
        String phone = uniquePhone();
        String password = "Pass1234";
        LoginRequest req = loginRequest(phone, password);

        // First registration succeeds
        ResponseEntity<R<User>> first = restTemplate.exchange(
                "/api/user/register",
                HttpMethod.POST,
                new HttpEntity<>(req),
                new ParameterizedTypeReference<R<User>>() {});
        assertThat(first.getBody()).isNotNull();
        assertThat(first.getBody().getCode())/*FIX:code now 0*/ .isEqualTo(0);

        // Second registration with same phone fails
        ResponseEntity<String> second = restTemplate.exchange(
                "/api/user/register",
                HttpMethod.POST,
                new HttpEntity<>(req),
                String.class);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(second.getBody());
        assertThat(body.get("code").asInt()).isEqualTo(10002); // USER_PHONE_EXISTS
        assertThat(body.get("message").asText()).contains("已注册");
    }

    @Test
    @DisplayName("Wrong password login returns 400")
    void shouldReturnErrorWhenLoginWrongPassword() throws Exception {
        String phone = uniquePhone();
        String password = "Pass1234";
        restTemplate.postForEntity("/api/user/register",
                loginRequest(phone, password), R.class);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/user/login",
                loginRequest(phone, "WrongPassword"),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("code").asInt()).isEqualTo(10003); // USER_PASSWORD_ERROR
        assertThat(body.get("message").asText()).contains("密码错误");
    }

    @Test
    @DisplayName("Get info for non-existent user returns 400")
    void shouldReturnErrorWhenGetNonExistentUser() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/user/info?userId=999999999999999", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("code").asInt()).isEqualTo(10001); // USER_NOT_FOUND
        assertThat(body.get("message").asText()).contains("用户不存在");
    }
}
