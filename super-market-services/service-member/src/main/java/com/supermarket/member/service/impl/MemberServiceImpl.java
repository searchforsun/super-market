package com.supermarket.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supermarket.common.core.exception.BizException;
import com.supermarket.member.entity.Member;
import com.supermarket.member.mapper.MemberMapper;
import com.supermarket.member.service.MemberService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberServiceImpl extends ServiceImpl<MemberMapper, Member> implements MemberService {

    @Override
    @Transactional
    public Member getOrCreate(Long userId) {
        Member member = getByUserId(userId);
        if (member == null) {
            member = new Member();
            member.setUserId(userId);
            member.setLevel(0);
            member.setPoints(0);
            member.setTotalPoints(0);
            member.setGrowthValue(0);
            save(member);
        }
        return member;
    }

    @Override
    public Member getByUserId(Long userId) {
        return getOne(new LambdaQueryWrapper<Member>().eq(Member::getUserId, userId));
    }

    @Override
    public void addPoints(Long userId, int points) {
        getOrCreate(userId);
        baseMapper.addPoints(userId, points);
        Member member = getByUserId(userId);
        int newLevel = computeLevel(member.getTotalPoints());
        if (newLevel > member.getLevel()) {
            member.setLevel(newLevel);
            updateById(member);
        }
    }

    @Override
    public boolean deductPoints(Long userId, int points) {
        Member member = getByUserId(userId);
        if (member == null || member.getPoints() < points) {
            return false;
        }
        return baseMapper.deductPoints(userId, points) > 0;
    }

    @Override
    public int computeLevel(int totalPoints) {
        if (totalPoints >= 50000) return 4; // 钻石
        if (totalPoints >= 10000) return 3; // 黄金
        if (totalPoints >= 1000)  return 2; // 白银
        if (totalPoints >= 100)   return 1; // 青铜
        return 0;                             // 普通
    }
}
