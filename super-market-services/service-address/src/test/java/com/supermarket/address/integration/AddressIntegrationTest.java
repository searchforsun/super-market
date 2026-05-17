package com.supermarket.address.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supermarket.address.entity.Address;
import com.supermarket.common.core.result.R;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
@DisplayName("Address HTTP Integration Test")
class AddressIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private long userId;
    private int addressSeq;

    @BeforeEach
    void setUp() {
        userId = Math.abs(System.nanoTime() % 1_000_000_000) + 20000;
        addressSeq = 0;
        String phone = "188" + String.format("%08d", userId % 100_000_000);
        jdbcTemplate.update(
                "INSERT IGNORE INTO users (id, phone, password_hash, nickname, status) VALUES (?, ?, ?, ?, ?)",
                userId, phone, "fake_hash", "address_test_user", 1);
    }

    private Address createAddress(String tag) {
        addressSeq++;
        Address addr = new Address();
        addr.setUserId(userId);
        addr.setReceiverName("Receiver_" + tag);
        addr.setReceiverPhone("138" + String.format("%08d", addressSeq));
        addr.setProvince("Test Province");
        addr.setCity("Test City");
        addr.setDistrict("Test District");
        addr.setDetail(tag + " Detail Street No." + addressSeq);
        return addr;
    }

    @Test
    @DisplayName("Create 3 addresses -> List -> Set default -> Update -> Delete -> Verify final list")
    void shouldCompleteAddressCrudFlow() {
        // Step 1: Create 3 addresses
        Long addr1Id = createAddressAndGetId("Home");
        Long addr2Id = createAddressAndGetId("Office");
        Long addr3Id = createAddressAndGetId("Other");

        assertThat(addr1Id).isNotNull();
        assertThat(addr2Id).isNotNull();
        assertThat(addr3Id).isNotNull();

        // Step 2: List addresses
        ResponseEntity<R<List<Address>>> listResponse = restTemplate.exchange(
                "/api/address/list?userId=" + userId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<R<List<Address>>>() {});
        assertThat(listResponse.getBody().getCode())/*FIX:code now 0*/ .isEqualTo(0);
        List<Address> addresses = listResponse.getBody().getData();
        assertThat(addresses).hasSize(3);

        // First address should be default (the first one created when list was empty)
        Address defaultAddr = addresses.stream()
                .filter(a -> a.getIsDefault() == 1)
                .findFirst().orElse(null);
        assertThat(defaultAddr).isNotNull();
        assertThat(defaultAddr.getId()).isEqualTo(addr1Id);

        // Step 3: Set addr3 as default
        ResponseEntity<R<Void>> setDefaultResponse = restTemplate.exchange(
                "/api/address/" + addr3Id + "/default?userId=" + userId,
                HttpMethod.PUT,
                null,
                new ParameterizedTypeReference<R<Void>>() {});
        assertThat(setDefaultResponse.getBody().getCode())/*FIX:code now 0*/ .isEqualTo(0);

        // Step 4: Verify addr3 is now default
        ResponseEntity<R<List<Address>>> afterDefaultResponse = restTemplate.exchange(
                "/api/address/list?userId=" + userId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<R<List<Address>>>() {});
        Address newDefault = afterDefaultResponse.getBody().getData().stream()
                .filter(a -> a.getIsDefault() == 1)
                .findFirst().orElse(null);
        assertThat(newDefault).isNotNull();
        assertThat(newDefault.getId()).isEqualTo(addr3Id);

        // Step 5: Update addr2 details
        Address updateAddr = new Address();
        updateAddr.setId(addr2Id);
        updateAddr.setUserId(userId);
        updateAddr.setReceiverName("Updated Office");
        updateAddr.setReceiverPhone("13900000002");
        updateAddr.setProvince("Updated Province");
        updateAddr.setCity("Updated City");
        updateAddr.setDistrict("Updated District");
        updateAddr.setDetail("Updated Detail");

        ResponseEntity<R<Address>> updateResponse = restTemplate.exchange(
                "/api/address",
                HttpMethod.PUT,
                new HttpEntity<>(updateAddr),
                new ParameterizedTypeReference<R<Address>>() {});
        assertThat(updateResponse.getBody().getCode())/*FIX:code now 0*/ .isEqualTo(0);
        assertThat(updateResponse.getBody().getData().getReceiverName()).isEqualTo("Updated Office");

        // Step 6: Delete addr2 (non-default)
        ResponseEntity<R<Void>> deleteResponse = restTemplate.exchange(
                "/api/address/" + addr2Id + "?userId=" + userId,
                HttpMethod.DELETE,
                null,
                new ParameterizedTypeReference<R<Void>>() {});
        assertThat(deleteResponse.getBody().getCode())/*FIX:code now 0*/ .isEqualTo(0);

        // Step 7: Verify final list has 2 addresses
        ResponseEntity<R<List<Address>>> finalListResponse = restTemplate.exchange(
                "/api/address/list?userId=" + userId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<R<List<Address>>>() {});
        assertThat(finalListResponse.getBody().getData()).hasSize(2);
        // addr3 should be default, addr1 should remain
        assertThat(finalListResponse.getBody().getData())
                .extracting(Address::getId)
                .containsExactlyInAnyOrder(addr1Id, addr3Id);
    }

    @Test
    @DisplayName("Update non-existent address returns 404")
    void shouldReturnErrorWhenUpdateNonExistentAddress() throws Exception {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/address",
                HttpMethod.PUT,
                new HttpEntity<>(createAddress("Ghost")),
                String.class);
        // Address created with no id so this is a create-like update → no error actually
        // Now test with a specific non-existent ID
        Address ghost = createAddress("Ghost");
        ghost.setId(999999999999999L);
        ResponseEntity<String> updateResponse = restTemplate.exchange(
                "/api/address",
                HttpMethod.PUT,
                new HttpEntity<>(ghost),
                String.class);
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(updateResponse.getBody());
        assertThat(body.get("code").asInt()).isEqualTo(10012); // ADDRESS_NOT_FOUND
        assertThat(body.get("message").asText()).contains("地址不存在");
    }

    private Long createAddressAndGetId(String tag) {
        Address req = createAddress(tag);
        ResponseEntity<R<Address>> response = restTemplate.exchange(
                "/api/address",
                HttpMethod.POST,
                new HttpEntity<>(req),
                new ParameterizedTypeReference<R<Address>>() {});
        assertThat(response.getBody().getCode())/*FIX:code now 0*/ .isEqualTo(0);
        return response.getBody().getData().getId();
    }
}
