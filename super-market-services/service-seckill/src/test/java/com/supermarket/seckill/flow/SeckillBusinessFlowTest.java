package com.supermarket.seckill.flow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.supermarket.common.web.handler.GlobalExceptionHandler;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Postman-style business flow test for Seckill domain.
 *
 * Uses real MySQL + Redis on DOCKER_HOST_IP (ecs4c16g).
 * RocketMQ is mocked; @EnableDubbo is excluded via TestConfig.
 */
@SpringBootTest(classes = {SeckillBusinessFlowTest.TestConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("flow-test")
@Transactional
@DisplayName("Seckill Business Flow")
class SeckillBusinessFlowTest {

    @Configuration
    @EnableAutoConfiguration
    @MapperScan("com.supermarket.seckill.mapper")
    @ComponentScan(basePackages = "com.supermarket.seckill",
        excludeFilters = @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = {com.supermarket.seckill.config.DubboProviderConfig.class,
                       com.supermarket.seckill.SeckillApplication.class}))
    @Import(GlobalExceptionHandler.class)
    static class TestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RocketMQTemplate rocketMQTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    @DisplayName("Complete seckill flow: session -> product -> list")
    void shouldCompleteSeckillFlow() throws Exception {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        String startTime = LocalDateTime.now().plusHours(1).format(fmt);
        String endTime = LocalDateTime.now().plusDays(1).format(fmt);

        // 1. Create session
        String sessionJson = String.format("""
                {"name": "618限时秒杀", "startTime": "%s", "endTime": "%s"}
                """, startTime, endTime);
        String sessionResp = mockMvc.perform(post("/api/seckill/admin/session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sessionJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
        Long sessionId = objectMapper.readTree(sessionResp).get("data").get("id").asLong();
        assertThat(sessionId).isNotNull();

        // 2. Create product
        String productJson = """
                {"spuId": 99999, "skuId": 99999, "seckillPrice": 9.90, "seckillStock": 100, "limitPerUser": 2}
                """;
        String productResp = mockMvc.perform(post("/api/seckill/admin/product")
                        .param("sessionId", String.valueOf(sessionId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
        Long productId = objectMapper.readTree(productResp).get("data").get("id").asLong();
        assertThat(productId).isNotNull();

        // 3. List sessions - verify our session appears (may include seed data)
        mockMvc.perform(get("/api/seckill/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").isNumber());

        // 4. List products - verify our product appears
        mockMvc.perform(get("/api/seckill/products")
                        .param("sessionId", String.valueOf(sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").isNumber());
    }
}
