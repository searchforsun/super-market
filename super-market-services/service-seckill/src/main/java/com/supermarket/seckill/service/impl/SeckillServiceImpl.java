package com.supermarket.seckill.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.supermarket.common.core.exception.BizException;
import com.supermarket.seckill.entity.SeckillProduct;
import com.supermarket.seckill.entity.SeckillSession;
import com.supermarket.seckill.mapper.SeckillProductMapper;
import com.supermarket.seckill.mapper.SeckillSessionMapper;
import com.supermarket.seckill.service.SeckillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillServiceImpl implements SeckillService {

    private final SeckillSessionMapper sessionMapper;
    private final SeckillProductMapper productMapper;
    private final StringRedisTemplate redisTemplate;
    private final RedissonClient redissonClient;
    private final RocketMQTemplate rocketMQTemplate;

    private static final String STOCK_KEY = "smt:seckill:stock:";
    private static final String PRODUCT_KEY = "smt:seckill:product:";
    private static final String USER_LIMIT_KEY = "smt:seckill:user:";
    private static final String SECKILL_ORDER_TOPIC = "seckill-order-topic";

    private static final String LUA_DEDUCT = """
            local stockKey = KEYS[1]
            local limitKey = KEYS[2]
            local quantity = tonumber(ARGV[1])
            local limit = tonumber(ARGV[2])
            local stock = redis.call('get', stockKey)
            if not stock or tonumber(stock) < quantity then
                return 0
            end
            local userCount = redis.call('get', limitKey)
            if userCount and tonumber(userCount) >= limit then
                return -1
            end
            redis.call('decrby', stockKey, quantity)
            redis.call('incrby', limitKey, quantity)
            return 1
            """;

    @Override
    @Transactional
    public SeckillSession createSession(SeckillSession session) {
        session.setStatus(0);
        sessionMapper.insert(session);
        return session;
    }

    @Override
    @Transactional
    public SeckillProduct createProduct(SeckillProduct product) {
        product.setStatus(0);
        productMapper.insert(product);
        return product;
    }

    @Override
    public List<SeckillSession> listSessions() {
        return sessionMapper.selectList(new LambdaQueryWrapper<SeckillSession>()
                .ge(SeckillSession::getEndTime, LocalDateTime.now())
                .orderByAsc(SeckillSession::getStartTime));
    }

    @Override
    public List<SeckillProduct> listProducts(Long sessionId) {
        return productMapper.selectList(new LambdaQueryWrapper<SeckillProduct>()
                .eq(SeckillProduct::getSessionId, sessionId));
    }

    @Override
    public void preheat(Long seckillProductId) {
        SeckillProduct sp = productMapper.selectById(seckillProductId);
        if (sp == null) throw new BizException(404, "秒杀商品不存在");

        String stockKey = STOCK_KEY + seckillProductId;
        String productKey = PRODUCT_KEY + seckillProductId;

        redisTemplate.opsForValue().set(stockKey, String.valueOf(sp.getSeckillStock()));
        Map<String, String> info = new HashMap<>();
        info.put("spuId", String.valueOf(sp.getSpuId()));
        info.put("skuId", String.valueOf(sp.getSkuId()));
        info.put("price", sp.getSeckillPrice().toString());
        info.put("limit", String.valueOf(sp.getLimitPerUser()));
        redisTemplate.opsForHash().putAll(productKey, info);

        sp.setStatus(1);
        productMapper.updateById(sp);
        log.info("秒杀商品预热完成: seckillProductId={}, stock={}", seckillProductId, sp.getSeckillStock());
    }

    @Override
    public Map<String, Object> execute(Long userId, Long seckillProductId, int quantity) {
        String lockKey = "smt:lock:seckill:execute:" + seckillProductId + ":" + userId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(2, 3, java.util.concurrent.TimeUnit.SECONDS)) {
                return Map.of("success", false, "message", "系统繁忙，请稍后再试");
            }

            SeckillProduct sp = productMapper.selectById(seckillProductId);
            if (sp == null || sp.getStatus() != 1) {
                return Map.of("success", false, "message", "秒杀商品不可用");
            }

            String stockKey = STOCK_KEY + seckillProductId;
            String limitKey = USER_LIMIT_KEY + seckillProductId + ":" + userId;

            DefaultRedisScript<Long> script = new DefaultRedisScript<>(LUA_DEDUCT, Long.class);
            Long result = redisTemplate.execute(script, List.of(stockKey, limitKey),
                    String.valueOf(quantity), String.valueOf(sp.getLimitPerUser()));

            if (result == null || result == 0) {
                return Map.of("success", false, "message", "库存不足，已抢光");
            }
            if (result == -1) {
                return Map.of("success", false, "message", "已达每人限购数量");
            }

            // Lua 扣减成功 → 发送 RocketMQ 异步创建订单
            String requestId = IdUtil.fastSimpleUUID();
            Map<String, Object> mqMsg = new HashMap<>();
            mqMsg.put("requestId", requestId);
            mqMsg.put("userId", userId);
            mqMsg.put("seckillProductId", seckillProductId);
            mqMsg.put("skuId", sp.getSkuId());
            mqMsg.put("spuId", sp.getSpuId());
            mqMsg.put("price", sp.getSeckillPrice().toString());
            mqMsg.put("quantity", quantity);

            Message<String> msg = MessageBuilder.withPayload(
                    new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(mqMsg)).build();
            rocketMQTemplate.syncSend(SECKILL_ORDER_TOPIC, msg, 3000, 0);

            return Map.of("success", true, "message", "抢购成功，订单处理中", "requestId", requestId);

        } catch (Exception e) {
            log.error("秒杀执行失败: userId={}, productId={}", userId, seckillProductId, e);
            return Map.of("success", false, "message", "系统异常，请稍后再试");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
