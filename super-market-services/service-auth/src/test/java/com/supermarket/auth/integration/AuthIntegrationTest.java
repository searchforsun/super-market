package com.supermarket.auth.integration;

import com.supermarket.common.core.result.R;
import com.supermarket.common.dubbo.api.shop.ShopDubboService;
import com.supermarket.common.dubbo.api.user.UserDubboService;
import com.supermarket.common.dubbo.api.user.dto.UserDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {AuthIntegrationTest.TestApp.class, AuthIntegrationTest.TestConfig.class},
        properties = {
                "spring.main.allow-bean-definition-overriding=true",
                "spring.cloud.nacos.config.enabled=false",
                "spring.cloud.nacos.discovery.enabled=false",
                "nacos.remote.client.grpc.enabled=false"
        })
@ActiveProfiles("test")
@DisplayName("Auth HTTP Integration Test")
class AuthIntegrationTest {

    /**
     * Minimal Spring Boot application for tests. Component scanning covers
     * every needed package under {@code com.supermarket.auth} and
     * {@code com.supermarket.common} <strong>except</strong>
     * {@code com.supermarket.auth.config} (which would activate
     * {@code @DubboComponentScan}). {@code @MapperScan} is required because
     * {@code AutoConfiguredMapperScannerRegistrar} scans the
     * {@code @SpringBootApplication} class's own package
     * ({@code com.supermarket.auth.integration}) rather than
     * {@code scanBasePackages}.
     * <p>
     * Dubbo and Nacos auto-configuration classes are excluded via
     * {@code application-test.yml}. With Dubbo's annotation post-processor
     * disabled, {@code @DubboReference} is passive -- Spring's
     * {@code @Autowired(required = false)} injects the {@code @Bean}
     * definitions from {@link TestConfig TestConfig} instead.
     */
    @SpringBootApplication(
            scanBasePackages = {
                    "com.supermarket.auth.controller",
                    "com.supermarket.auth.service",
                    "com.supermarket.auth.mapper",
                    "com.supermarket.auth.entity",
                    "com.supermarket.common.web",
                    "com.supermarket.common.core",
                    "com.supermarket.common.security",
                    "com.supermarket.common.mybatis"
            }
    )
    @MapperScan(basePackages = {"com.supermarket.auth.mapper"})
    static class TestApp {
    }

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long userId;
    private String testPhone;
    private static final String TEST_PASSWORD = "Pass1234";
    private String passwordHash;

    @BeforeEach
    void setUp() {
        userId = Math.abs(System.nanoTime() % 1_000_000_000) + 30000;
        testPhone = "189" + String.format("%08d", userId % 100_000_000);
        passwordHash = ENCODER.encode(TEST_PASSWORD);

        // Ensure the user_roles table exists (may not be present if schema
        // initialization has not been run on the shared db_user database)
        jdbcTemplate.update(
                "CREATE TABLE IF NOT EXISTS user_roles ("
                + "  id BIGINT NOT NULL PRIMARY KEY,"
                + "  user_id BIGINT NOT NULL,"
                + "  role_id BIGINT NOT NULL,"
                + "  created_at DATETIME DEFAULT CURRENT_TIMESTAMP"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        // Seed a user in the shared db_user.users table
        jdbcTemplate.update(
                "INSERT IGNORE INTO users (id, phone, password_hash, nickname, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                userId, testPhone, passwordHash, "auth_test_user", 1);

        // Ensure ROLE_USER exists in roles table
        jdbcTemplate.update(
                "INSERT IGNORE INTO roles (id, name, label, created_at, updated_at) VALUES (1, 'ROLE_USER', '普通用户', NOW(), NOW())");

        // Assign ROLE_USER to the test user
        jdbcTemplate.update(
                "INSERT IGNORE INTO user_roles (user_id, role_id, created_at) VALUES (?, 1, NOW())",
                userId);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM user_roles WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);
    }

    @Test
    @DisplayName("Login -> Refresh -> Verify new tokens")
    void shouldCompleteLoginAndTokenRefreshFlow() {
        // Step 1: Login via /api/auth/login
        Map<String, String> loginBody = Map.of(
                "phone", testPhone,
                "password", TEST_PASSWORD
        );

        ResponseEntity<Map<String, Object>> loginResponse = restTemplate.exchange(
                "/api/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(loginBody),
                new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> loginData = (Map<String, Object>) loginResponse.getBody().get("data");
        assertThat(loginData).isNotNull();
        assertThat(loginData.get("accessToken")).isNotNull();
        assertThat(loginData.get("refreshToken")).isNotNull();
        assertThat(loginData.get("userId")).isNotNull();
        assertThat(loginData.get("roles")).isNotNull();
        String refreshToken = (String) loginData.get("refreshToken");
        assertThat(refreshToken).contains(".");

        // Step 2: Refresh tokens
        ResponseEntity<Map<String, Object>> refreshResponse = restTemplate.exchange(
                "/api/auth/refresh?refreshToken=" + refreshToken,
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> refreshData = (Map<String, Object>) refreshResponse.getBody().get("data");
        assertThat(refreshData).isNotNull();
        assertThat(refreshData.get("accessToken")).isNotNull();
        assertThat(refreshData.get("refreshToken")).isNotNull();
        String newRefreshToken = (String) refreshData.get("refreshToken");
        assertThat(newRefreshToken).contains(".");

        // Step 3: Verify the new refresh token also works
        ResponseEntity<Map<String, Object>> reRefreshResponse = restTemplate.exchange(
                "/api/auth/refresh?refreshToken=" + newRefreshToken,
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(reRefreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> reRefreshData = (Map<String, Object>) reRefreshResponse.getBody().get("data");
        assertThat(reRefreshData).isNotNull();
        assertThat(reRefreshData.get("accessToken")).isNotNull();
    }

    @Test
    @DisplayName("Login with wrong password returns error")
    void shouldReturnErrorWhenLoginWrongPassword() {
        Map<String, String> loginBody = Map.of(
                "phone", testPhone,
                "password", "WrongPassword"
        );

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/api/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(loginBody),
                new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // ResultCode.USER_PASSWORD_ERROR = 10003 ("密码错误")
        assertThat(response.getBody().get("code")).isEqualTo(10003);
    }

    /**
     * Provides Dubbo service beans backed by real database queries.
     * These are injected via {@code @Autowired(required = false)} on the
     * {@code AuthServiceImpl} fields. Dubbo's {@code @DubboReference} annotation
     * is passive because Dubbo auto-configuration is excluded, so Spring's
     * {@code @Autowired(required = false)} handles the injection without
     * interference.
     */
    @Configuration
    static class TestConfig {

        @Bean
        UserDubboService userDubboService(JdbcTemplate jdbcTemplate) {
            return new UserDubboService() {
                @Override
                public UserDTO getUserById(Long id) {
                    return jdbcTemplate.queryForObject(
                            "SELECT id, phone, nickname, avatar_url, status, password_hash, created_at FROM users WHERE id = ?",
                            (rs, rowNum) -> {
                                UserDTO dto = new UserDTO();
                                dto.setId(rs.getLong("id"));
                                dto.setPhone(rs.getString("phone"));
                                dto.setNickname(rs.getString("nickname"));
                                dto.setAvatarUrl(rs.getString("avatar_url"));
                                dto.setStatus(rs.getInt("status"));
                                dto.setPasswordHash(rs.getString("password_hash"));
                                dto.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                                return dto;
                            },
                            id);
                }

                @Override
                public UserDTO getUserByPhone(String phone) {
                    try {
                        return jdbcTemplate.queryForObject(
                                "SELECT id, phone, nickname, avatar_url, status, password_hash, created_at FROM users WHERE phone = ?",
                                (rs, rowNum) -> {
                                    UserDTO dto = new UserDTO();
                                    dto.setId(rs.getLong("id"));
                                    dto.setPhone(rs.getString("phone"));
                                    dto.setNickname(rs.getString("nickname"));
                                    dto.setAvatarUrl(rs.getString("avatar_url"));
                                    dto.setStatus(rs.getInt("status"));
                                    dto.setPasswordHash(rs.getString("password_hash"));
                                    dto.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                                    return dto;
                                },
                                phone);
                    } catch (Exception e) {
                        return null;
                    }
                }

                @Override
                public java.util.List<java.util.Map<String, Object>> getDailyNewUsers(int days) {
                    return java.util.Collections.emptyList();
                }

                @Override
                public long countTotalUsers() {
                    return 0L;
                }

                @Override
                public long countTodayNewUsers() {
                    return 0L;
                }
            };
        }

        @Bean
        ShopDubboService shopDubboService() {
            return new ShopDubboService() {
                @Override
                public boolean hasMerchant(Long userId) {
                    return false;
                }

                @Override
                public Long getShopIdByUserId(Long userId) {
                    return null;
                }
            };
        }
    }
}
