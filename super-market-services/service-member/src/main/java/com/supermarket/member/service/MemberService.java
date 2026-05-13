package com.supermarket.member.service;

import com.supermarket.member.entity.Member;

public interface MemberService {
    Member getOrCreate(Long userId);
    Member getByUserId(Long userId);
    void addPoints(Long userId, int points);
    boolean deductPoints(Long userId, int points);
    int computeLevel(int totalPoints);
}
