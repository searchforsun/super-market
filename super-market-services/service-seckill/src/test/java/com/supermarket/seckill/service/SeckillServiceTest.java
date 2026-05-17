package com.supermarket.seckill.service;

import com.supermarket.seckill.entity.SeckillProduct;
import com.supermarket.seckill.entity.SeckillSession;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SeckillServiceTest {

    @Autowired
    private SeckillService seckillService;

    @MockBean
    private StringRedisTemplate redisTemplate;

    @MockBean
    private RocketMQTemplate rocketMQTemplate;

    @MockBean
    private RedissonClient redissonClient;

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

    @Test
    void shouldPreheatProduct() {
        // Arrange
        SeckillSession session = seckillService.createSession(newSession());
        SeckillProduct sp = new SeckillProduct();
        sp.setSessionId(session.getId());
        sp.setSpuId(100L);
        sp.setSkuId(1001L);
        sp.setSeckillPrice(new BigDecimal("9.90"));
        sp.setSeckillStock(100);
        sp.setLimitPerUser(1);
        SeckillProduct saved = seckillService.createProduct(sp);

        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOps);

        // Act
        seckillService.preheat(saved.getId());

        // Assert
        verify(valueOps).set(eq("smt:seckill:stock:" + saved.getId()), eq("100"));
        verify(hashOps).putAll(eq("smt:seckill:product:" + saved.getId()), any());
    }

    @Test
    void shouldThrowWhenPreheatingNonExistentProduct() {
        assertThatThrownBy(() -> seckillService.preheat(99999L))
                .isInstanceOf(com.supermarket.common.core.exception.BizException.class)
                .hasMessageContaining("秒杀商品不存在");
    }

    @Test
    void shouldExecuteSeckillSuccessfully() throws InterruptedException {
        // Arrange - create session + product
        SeckillSession session = seckillService.createSession(newSession());
        SeckillProduct sp = new SeckillProduct();
        sp.setSessionId(session.getId());
        sp.setSpuId(100L);
        sp.setSkuId(1001L);
        sp.setSeckillPrice(new BigDecimal("9.90"));
        sp.setSeckillStock(100);
        sp.setLimitPerUser(2);
        SeckillProduct saved = seckillService.createProduct(sp);

        // Preheat (mock redis for preheat)
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        seckillService.preheat(saved.getId());

        // Mock Redisson lock
        RLock mockLock = mock(RLock.class);
        when(mockLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(mockLock.isHeldByCurrentThread()).thenReturn(true);
        when(redissonClient.getLock(anyString())).thenReturn(mockLock);

        // Mock Lua script success (return 1)
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString(), anyString()))
                .thenReturn(1L);

        // Act
        Map<String, Object> result = seckillService.execute(1L, saved.getId(), 1);

        // Assert
        assertThat(result.get("success")).isEqualTo(true);
        assertThat(result.get("message")).isEqualTo("抢购成功，订单处理中");
        verify(rocketMQTemplate).syncSend(eq("seckill-order-topic"), any(), eq(3000L), eq(0));
    }

    @Test
    void shouldFailSeckillWhenStockExhausted() throws InterruptedException {
        // Arrange
        SeckillSession session = seckillService.createSession(newSession());
        SeckillProduct sp = new SeckillProduct();
        sp.setSessionId(session.getId());
        sp.setSpuId(100L);
        sp.setSkuId(1001L);
        sp.setSeckillPrice(new BigDecimal("9.90"));
        sp.setSeckillStock(0);
        sp.setLimitPerUser(1);
        SeckillProduct saved = seckillService.createProduct(sp);

        // Preheat
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        seckillService.preheat(saved.getId());

        // Mock Redisson lock
        RLock mockLock = mock(RLock.class);
        when(mockLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(mockLock.isHeldByCurrentThread()).thenReturn(true);
        when(redissonClient.getLock(anyString())).thenReturn(mockLock);

        // Mock Lua deduct failure (stock = 0)
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString(), anyString()))
                .thenReturn(0L);

        // Act
        Map<String, Object> result = seckillService.execute(1L, saved.getId(), 1);

        // Assert
        assertThat(result.get("success")).isEqualTo(false);
        assertThat(result.get("message")).isEqualTo("库存不足，已抢光");
    }

    @Test
    void shouldFailSeckillWhenUserLimitReached() throws InterruptedException {
        // Arrange
        SeckillSession session = seckillService.createSession(newSession());
        SeckillProduct sp = new SeckillProduct();
        sp.setSessionId(session.getId());
        sp.setSpuId(100L);
        sp.setSkuId(1001L);
        sp.setSeckillPrice(new BigDecimal("9.90"));
        sp.setSeckillStock(100);
        sp.setLimitPerUser(1);
        SeckillProduct saved = seckillService.createProduct(sp);

        // Preheat
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        seckillService.preheat(saved.getId());

        // Mock Redisson lock
        RLock mockLock = mock(RLock.class);
        when(mockLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(mockLock.isHeldByCurrentThread()).thenReturn(true);
        when(redissonClient.getLock(anyString())).thenReturn(mockLock);

        // Mock Lua limit failure (return -1)
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString(), anyString()))
                .thenReturn(-1L);

        // Act
        Map<String, Object> result = seckillService.execute(1L, saved.getId(), 1);

        // Assert
        assertThat(result.get("success")).isEqualTo(false);
        assertThat(result.get("message")).isEqualTo("已达每人限购数量");
    }

    @Test
    void shouldFailSeckillWhenProductNotPreheated() throws InterruptedException {
        // Arrange - create but DON'T preheat
        SeckillSession session = seckillService.createSession(newSession());
        SeckillProduct sp = new SeckillProduct();
        sp.setSessionId(session.getId());
        sp.setSpuId(100L);
        sp.setSkuId(1001L);
        sp.setSeckillPrice(new BigDecimal("9.90"));
        sp.setSeckillStock(100);
        sp.setLimitPerUser(1);
        SeckillProduct saved = seckillService.createProduct(sp);

        // Mock Redisson lock
        RLock mockLock = mock(RLock.class);
        when(mockLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(mockLock.isHeldByCurrentThread()).thenReturn(true);
        when(redissonClient.getLock(anyString())).thenReturn(mockLock);

        // Act
        Map<String, Object> result = seckillService.execute(1L, saved.getId(), 1);

        // Assert
        assertThat(result.get("success")).isEqualTo(false);
        assertThat(result.get("message")).isEqualTo("秒杀商品不可用");
    }

    @Test
    void shouldFailSeckillWhenLockNotAcquired() throws InterruptedException {
        // Arrange
        RLock mockLock = mock(RLock.class);
        when(mockLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);
        when(redissonClient.getLock(anyString())).thenReturn(mockLock);
        when(mockLock.isHeldByCurrentThread()).thenReturn(false);

        // Act - any product ID, lock should fail
        Map<String, Object> result = seckillService.execute(1L, 99999L, 1);

        // Assert
        assertThat(result.get("success")).isEqualTo(false);
        assertThat(result.get("message")).isEqualTo("系统繁忙，请稍后再试");
    }

    private SeckillSession newSession() {
        SeckillSession session = new SeckillSession();
        session.setName("测试秒杀场次");
        session.setStartTime(LocalDateTime.now().plusMinutes(10));
        session.setEndTime(LocalDateTime.now().plusHours(2));
        return session;
    }
}
