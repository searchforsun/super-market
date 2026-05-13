package com.supermarket.seckill.service;

import com.supermarket.seckill.entity.SeckillProduct;
import com.supermarket.seckill.entity.SeckillSession;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class SeckillServiceTest {

    @Autowired
    private SeckillService seckillService;

    @MockBean
    private StringRedisTemplate redisTemplate;

    @MockBean
    private RocketMQTemplate rocketMQTemplate;

    @Test
    void shouldCreateSession() {
        SeckillSession session = new SeckillSession();
        session.setName("双11秒杀专场");
        session.setStartTime(LocalDateTime.now().plusHours(1));
        session.setEndTime(LocalDateTime.now().plusHours(3));
        SeckillSession saved = seckillService.createSession(session);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(0);
    }

    @Test
    void shouldCreateProduct() {
        SeckillSession session = seckillService.createSession(newSession());

        SeckillProduct sp = new SeckillProduct();
        sp.setSessionId(session.getId());
        sp.setSpuId(100L);
        sp.setSkuId(1001L);
        sp.setSeckillPrice(new BigDecimal("9.90"));
        sp.setSeckillStock(100);
        sp.setLimitPerUser(1);
        SeckillProduct saved = seckillService.createProduct(sp);
        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void shouldListSessions() {
        seckillService.createSession(newSession());
        List<SeckillSession> list = seckillService.listSessions();
        assertThat(list).hasSize(1);
    }

    @Test
    void shouldListProducts() {
        SeckillSession session = seckillService.createSession(newSession());
        SeckillProduct sp = new SeckillProduct();
        sp.setSessionId(session.getId());
        sp.setSpuId(200L);
        sp.setSkuId(2001L);
        sp.setSeckillPrice(new BigDecimal("19.90"));
        sp.setSeckillStock(50);
        sp.setLimitPerUser(1);
        seckillService.createProduct(sp);

        List<SeckillProduct> list = seckillService.listProducts(session.getId());
        assertThat(list).hasSize(1);
    }

    private SeckillSession newSession() {
        SeckillSession session = new SeckillSession();
        session.setName("测试秒杀场次");
        session.setStartTime(LocalDateTime.now().plusMinutes(10));
        session.setEndTime(LocalDateTime.now().plusHours(2));
        return session;
    }
}
