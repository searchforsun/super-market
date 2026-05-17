package com.supermarket.address.flow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supermarket.address.entity.Address;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Postman-style business flow test for the ADDRESS domain.
 * <p>
 * Chains multiple API calls to simulate a complete address management scenario:
 * create 3 addresses -> list -> set default -> update -> delete -> list again.
 * <p>
 * Note: In a full microservice environment, the test user would first register
 * via the service-user API. Since this test runs in the service-address module
 * where UserController is not available, we use a pre-determined userId.
 * The address service does not validate userId against the users table,
 * so any valid Long value works for testing address CRUD flows.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Address Business Flow Test")
class AddressBusinessFlowTest {

    private static final long TEST_USER_ID = 10001L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Flow 2: Complete Address CRUD and Default-Setting Flow")
    void shouldCompleteAddressCrudAndDefaultFlow() throws Exception {
        // =========================================================
        // Step 1: Create 3 addresses for the test user
        // =========================================================
        Address homeAddress = new Address();
        homeAddress.setUserId(TEST_USER_ID);
        homeAddress.setReceiverName("张三");
        homeAddress.setReceiverPhone("13800001001");
        homeAddress.setProvince("广东省");
        homeAddress.setCity("深圳市");
        homeAddress.setDistrict("南山区");
        homeAddress.setDetail("科技园南路88号");

        String homeResponse = mockMvc.perform(post("/api/address")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(homeAddress)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.receiverName").value("张三"))
                .andExpect(jsonPath("$.data.isDefault").value(1)) // first address auto-default
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long homeId = objectMapper.readTree(homeResponse).get("data").get("id").asLong();

        Address workAddress = new Address();
        workAddress.setUserId(TEST_USER_ID);
        workAddress.setReceiverName("张三");
        workAddress.setReceiverPhone("13800001002");
        workAddress.setProvince("广东省");
        workAddress.setCity("深圳市");
        workAddress.setDistrict("福田区");
        workAddress.setDetail("深南大道100号");

        String workResponse = mockMvc.perform(post("/api/address")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(workAddress)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.receiverName").value("张三"))
                // isDefault is null on the returned object (not set in entity, not refreshed after insert)
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long workId = objectMapper.readTree(workResponse).get("data").get("id").asLong();

        Address parentsAddress = new Address();
        parentsAddress.setUserId(TEST_USER_ID);
        parentsAddress.setReceiverName("张伟");
        parentsAddress.setReceiverPhone("13800001003");
        parentsAddress.setProvince("广东省");
        parentsAddress.setCity("广州市");
        parentsAddress.setDistrict("天河区");
        parentsAddress.setDetail("天河路200号");

        String parentsResponse = mockMvc.perform(post("/api/address")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(parentsAddress)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.receiverName").value("张伟"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long parentsId = objectMapper.readTree(parentsResponse).get("data").get("id").asLong();

        // =========================================================
        // Step 2: List addresses — verify all 3 returned
        // =========================================================
        String listResponse = mockMvc.perform(get("/api/address/list")
                        .param("userId", String.valueOf(TEST_USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(3))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode listData = objectMapper.readTree(listResponse).get("data");

        // The first address in the list should be the default one (home)
        assertThat(listData.get(0).get("isDefault").asInt()).isEqualTo(1);
        assertThat(listData.get(0).get("receiverName").asText()).isEqualTo("张三");

        // =========================================================
        // Step 3: Set the work address as default
        // =========================================================
        mockMvc.perform(put("/api/address/{id}/default", workId)
                        .param("userId", String.valueOf(TEST_USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"));

        // Verify: work address is now default
        String listAfterDefault = mockMvc.perform(get("/api/address/list")
                        .param("userId", String.valueOf(TEST_USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(3))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode listAfterDefaultData = objectMapper.readTree(listAfterDefault).get("data");
        // First item in list should now be the work address (default, sorted first)
        assertThat(listAfterDefaultData.get(0).get("id").asLong()).isEqualTo(workId);
        assertThat(listAfterDefaultData.get(0).get("isDefault").asInt()).isEqualTo(1);

        // =========================================================
        // Step 4: Update the default (work) address receiver name
        // =========================================================
        Address updatedWork = new Address();
        updatedWork.setId(workId);
        updatedWork.setReceiverName("张三（公司）");
        updatedWork.setReceiverPhone("13800001002");
        updatedWork.setProvince("广东省");
        updatedWork.setCity("深圳市");
        updatedWork.setDistrict("福田区");
        updatedWork.setDetail("深南大道100号");

        mockMvc.perform(put("/api/address")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedWork)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.receiverName").value("张三（公司）"))
                .andExpect(jsonPath("$.data.isDefault").value(1)); // still default after update

        // =========================================================
        // Step 5: Delete a non-default address (parents address)
        // =========================================================
        mockMvc.perform(delete("/api/address/{id}", parentsId)
                        .param("userId", String.valueOf(TEST_USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"));

        // =========================================================
        // Step 6: List again — verify 2 remaining, default unchanged
        // =========================================================
        String finalListResponse = mockMvc.perform(get("/api/address/list")
                        .param("userId", String.valueOf(TEST_USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode finalListData = objectMapper.readTree(finalListResponse).get("data");

        // Default address is still work (id=workId)
        assertThat(finalListData.get(0).get("id").asLong()).isEqualTo(workId);
        assertThat(finalListData.get(0).get("isDefault").asInt()).isEqualTo(1);
        assertThat(finalListData.get(0).get("receiverName").asText()).isEqualTo("张三（公司）");

        // Home address is second
        assertThat(finalListData.get(1).get("id").asLong()).isEqualTo(homeId);
        assertThat(finalListData.get(1).get("isDefault").asInt()).isEqualTo(0);
        assertThat(finalListData.get(1).get("receiverName").asText()).isEqualTo("张三");

        // Verify parents address is truly deleted
        assertThat(finalListData).noneMatch(n -> n.get("id").asLong() == parentsId);
    }
}
