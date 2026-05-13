package com.supermarket.member.controller;

import com.supermarket.common.core.result.R;
import com.supermarket.member.entity.Member;
import com.supermarket.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/{userId}")
    public R<Member> getMember(@PathVariable Long userId) {
        return R.ok(memberService.getOrCreate(userId));
    }

    @PostMapping("/points/add")
    public R<Void> addPoints(@RequestParam Long userId, @RequestParam int points) {
        memberService.addPoints(userId, points);
        return R.ok();
    }
}
