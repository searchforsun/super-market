package com.supermarket.shop.flow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supermarket.shop.ShopApplication;
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
 * Postman-style business flow test for the Shop domain.
 *
 * Tests merchant onboarding and shop creation: merchant apply → audit →
 * query shop by id → query shop by merchant → page merchants.
 *
 * Requires a running MySQL instance (configured via application-test.yml).
 * No middleware mocking needed — ShopService only uses the database.
 */
@SpringBootTest(classes = ShopApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("flow-test")
@Transactional
@DisplayName("Shop Business Flow")
class ShopBusinessFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Complete shop flow: apply merchant → audit → query shop → query by merchant → page")
    void shouldCompleteShopFlow() throws Exception {
        // ---------------------------------------------------------------
        // 1. Apply merchant registration
        // ---------------------------------------------------------------
        String merchantJson = """
                {
                    "userId": 1001,
                    "companyName": "测试科技股份有限公司",
                    "businessLicense": "91440101MA5XXXXXX",
                    "legalPerson": "张三",
                    "idCard": "110101199001011234",
                    "contactPhone": "13800138000"
                }
                """;
        String applyResp = mockMvc.perform(post("/api/shop/merchant/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(merchantJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.companyName").value("测试科技股份有限公司"))
                .andExpect(jsonPath("$.data.auditStatus").value(0))
                .andReturn().getResponse().getContentAsString();

        Long merchantId = extractId(applyResp, "data.id");
        assertThat(merchantId).isNotNull();

        // ---------------------------------------------------------------
        // 2. Audit merchant — approved (auditStatus=1)
        //    This should auto-create a Shop for the merchant.
        // ---------------------------------------------------------------
        mockMvc.perform(put("/api/shop/merchant/{id}/audit", merchantId)
                        .param("auditStatus", "1")
                        .param("reason", "资质审核通过"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // ---------------------------------------------------------------
        // 3. Get shop by merchant ID (verifies shop was auto-created)
        // ---------------------------------------------------------------
        String shopByMerchantResp = mockMvc.perform(get("/api/shop/merchant/{merchantId}", merchantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.merchantId").value(merchantId))
                .andReturn().getResponse().getContentAsString();

        Long shopId = extractId(shopByMerchantResp, "data.id");
        String shopName = objectMapper.readTree(shopByMerchantResp).path("data").path("shopName").asText();
        assertThat(shopId).isNotNull();
        assertThat(shopName).contains("旗舰店");

        // ---------------------------------------------------------------
        // 4. Get shop by ID directly
        // ---------------------------------------------------------------
        mockMvc.perform(get("/api/shop/{id}", shopId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(shopId))
                .andExpect(jsonPath("$.data.shopName").value(shopName));

        // ---------------------------------------------------------------
        // 5. Page merchants — verify our merchant appears
        // ---------------------------------------------------------------
        String pageResp = mockMvc.perform(get("/api/shop/merchant/page")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records[0].id").value(merchantId))
                .andReturn().getResponse().getContentAsString();

        JsonNode records = objectMapper.readTree(pageResp).path("data").path("records");
        assertThat(records).isNotEmpty();
        assertThat(records.get(0).path("auditStatus").asInt()).isEqualTo(1);

        // ---------------------------------------------------------------
        // (Negative) Duplicate apply for same userId should fail
        // ---------------------------------------------------------------
        mockMvc.perform(post("/api/shop/merchant/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(merchantJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(50003))
                .andExpect(jsonPath("$.message").value("该用户已提交入驻申请"));
    }

    private Long extractId(String json, String path) throws Exception {
        String[] parts = path.split("\\.");
        JsonNode node = objectMapper.readTree(json);
        for (String part : parts) {
            node = node.path(part);
        }
        return node.isMissingNode() ? null : node.asLong();
    }
}
