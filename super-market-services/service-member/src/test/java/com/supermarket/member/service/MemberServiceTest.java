package com.supermarket.member.service;

import com.supermarket.member.entity.Member;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MemberServiceTest {

    @Autowired
    private MemberService memberService;

    @Test
    void shouldCreateNewMember() {
        Member member = memberService.getOrCreate(100L);
        assertThat(member.getLevel()).isEqualTo(0);
        assertThat(member.getPoints()).isEqualTo(0);
    }

    @Test
    void shouldUpgradeLevel() {
        memberService.getOrCreate(101L);
        memberService.addPoints(101L, 5000);
        Member member = memberService.getByUserId(101L);
        assertThat(member.getLevel()).isEqualTo(2);
    }

    @Test
    void shouldComputeDiamondLevel() {
        assertThat(memberService.computeLevel(50000)).isEqualTo(4);
        assertThat(memberService.computeLevel(100)).isEqualTo(1);
        assertThat(memberService.computeLevel(50)).isEqualTo(0);
    }

    @Test
    void shouldDeductPoints() {
        memberService.getOrCreate(102L);
        memberService.addPoints(102L, 100);
        boolean result = memberService.deductPoints(102L, 30);
        assertThat(result).isTrue();
        Member member = memberService.getByUserId(102L);
        assertThat(member.getPoints()).isGreaterThanOrEqualTo(70);
    }

    @Test
    void shouldFailDeductWhenInsufficientPoints() {
        memberService.getOrCreate(103L);
        boolean result = memberService.deductPoints(103L, 100);
        assertThat(result).isFalse();
    }
}
