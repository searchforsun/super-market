package com.supermarket.member.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supermarket.common.core.result.R;
import com.supermarket.member.entity.Member;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
@DisplayName("Member HTTP Integration Test")
class MemberIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Unique user ID for the test run, seeded into db_user.users if not present. */
    private long userId;

    @BeforeEach
    void setUp() {
        userId = Math.abs(System.nanoTime() % 1_000_000_000) + 10000;
        // Ensure a users row exists (foreign key or query expectation)
        String phone = "199" + String.format("%08d", userId % 100_000_000);
        jdbcTemplate.update(
                "INSERT IGNORE INTO users (id, phone, password_hash, nickname, status) VALUES (?, ?, ?, ?, ?)",
                userId, phone, "fake_hash", "test_user", 1);
    }

    @Test
    @DisplayName("GetOrCreate -> AddPoints -> Verify -> DeductPoints -> Verify -> AddMore -> LevelUp -> Verify")
    void shouldCompleteMemberPointsAndLevelFlow() {
        // Step 1: GetOrCreate member
        ResponseEntity<R<Member>> getResponse = restTemplate.exchange(
                "/api/member/" + userId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<R<Member>>() {});
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        assertThat(getResponse.getBody().getCode())/*FIX:code now 0*/ .isEqualTo(0);
        Member member = getResponse.getBody().getData();
        assertThat(member).isNotNull();
        assertThat(member.getUserId()).isEqualTo(userId);
        assertThat(member.getLevel()).isEqualTo(0);
        assertThat(member.getPoints()).isEqualTo(0);
        assertThat(member.getTotalPoints()).isEqualTo(0);

        // Step 2: Add points
        ResponseEntity<R<Void>> addResponse = restTemplate.exchange(
                "/api/member/points/add?userId=" + userId + "&points=500",
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<R<Void>>() {});
        assertThat(addResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(addResponse.getBody()).isNotNull();
        assertThat(addResponse.getBody().getCode())/*FIX:code now 0*/ .isEqualTo(0);

        // Step 3: Verify points after adding
        ResponseEntity<R<Member>> afterAddResponse = restTemplate.exchange(
                "/api/member/" + userId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<R<Member>>() {});
        assertThat(afterAddResponse.getBody().getData().getPoints()).isEqualTo(500);
        assertThat(afterAddResponse.getBody().getData().getTotalPoints()).isEqualTo(500);
        // 500 total points is below 1000 threshold, so level should be 1 (bronze: 100+)
        assertThat(afterAddResponse.getBody().getData().getLevel()).isGreaterThanOrEqualTo(1);

        // Step 4: Deduct points
        ResponseEntity<R<Void>> deductResponse = restTemplate.exchange(
                "/api/member/points/deduct?userId=" + userId + "&points=200",
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<R<Void>>() {});
        assertThat(deductResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(deductResponse.getBody()).isNotNull();
        assertThat(deductResponse.getBody().getCode())/*FIX:code now 0*/ .isEqualTo(0);

        // Step 5: Verify after deduction
        ResponseEntity<R<Member>> afterDeductResponse = restTemplate.exchange(
                "/api/member/" + userId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<R<Member>>() {});
        assertThat(afterDeductResponse.getBody().getData().getPoints()).isEqualTo(300);
        assertThat(afterDeductResponse.getBody().getData().getTotalPoints()).isEqualTo(500);

        // Step 6: Add more points to level up (1000+ for level 2)
        ResponseEntity<R<Void>> levelUpResponse = restTemplate.exchange(
                "/api/member/points/add?userId=" + userId + "&points=700",
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<R<Void>>() {});
        assertThat(levelUpResponse.getBody().getCode())/*FIX:code now 0*/ .isEqualTo(0);

        // Step 7: Verify level up
        ResponseEntity<R<Member>> afterLevelUpResponse = restTemplate.exchange(
                "/api/member/" + userId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<R<Member>>() {});
        Member leveled = afterLevelUpResponse.getBody().getData();
        assertThat(leveled.getTotalPoints()).isEqualTo(1200);
        // 1200 >= 1000 → level 2 (silver)
        assertThat(leveled.getLevel()).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("Deduct more points than available returns R.ok but points are unchanged")
    void shouldReturnOkWhenDeductMoreThanAvailable() {
        // The controller always returns R.ok() regardless of deduction result.
        // The test verifies the business behavior: insufficient points → no change.

        // Create member with 100 points
        restTemplate.exchange(
                "/api/member/" + userId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<R<Member>>() {});
        restTemplate.exchange(
                "/api/member/points/add?userId=" + userId + "&points=100",
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<R<Void>>() {});

        // Try to deduct 200 points (more than available 100)
        ResponseEntity<String> deductResponse = restTemplate.exchange(
                "/api/member/points/deduct?userId=" + userId + "&points=200",
                HttpMethod.POST,
                null,
                String.class);
        assertThat(deductResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Verify points unchanged
        ResponseEntity<R<Member>> verifyResponse = restTemplate.exchange(
                "/api/member/" + userId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<R<Member>>() {});
        assertThat(verifyResponse.getBody().getData().getPoints()).isEqualTo(100);
    }
}
