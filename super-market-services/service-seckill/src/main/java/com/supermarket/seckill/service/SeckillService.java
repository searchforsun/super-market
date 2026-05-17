package com.supermarket.seckill.service;

import com.supermarket.seckill.entity.SeckillProduct;
import com.supermarket.seckill.entity.SeckillSession;

import java.util.List;
import java.util.Map;

public interface SeckillService {

    SeckillSession createSession(SeckillSession session);

    SeckillProduct createProduct(SeckillProduct product);

    List<SeckillSession> listSessions();

    List<SeckillSession> listAllSessions();

    SeckillSession updateSession(SeckillSession session);

    void deleteSession(Long id);

    List<SeckillProduct> listProducts(Long sessionId);

    void deleteProduct(Long id);

    void preheat(Long seckillProductId);

    Map<String, Object> execute(Long userId, Long seckillProductId, int quantity);
}
