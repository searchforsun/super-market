package com.supermarket.member.controller;

import com.supermarket.common.core.result.R;
import com.supermarket.member.entity.Member;
import com.supermarket.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
@Tag(name = "会员服务", description = "会员信息、积分管理接口")
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "获取会员信息")
    @GetMapping("/{userId}")
    public R<Member> getMember(@Parameter(description = "用户ID") @PathVariable Long userId) {
        return R.ok(memberService.getOrCreate(userId));
    }

    @Operation(summary = "增加会员积分")
    @PostMapping("/points/add")
    public R<Void> addPoints(@Parameter(description = "用户ID") @RequestParam Long userId,
                             @Parameter(description = "积分数") @RequestParam int points) {
        memberService.addPoints(userId, points);
        return R.ok();
    }

    @PostMapping("/points/deduct")
    @Operation(summary = "扣减积分")
    public R<Void> deductPoints(
        @RequestParam @NotNull(message = "用户ID不能为空") Long userId,
        @RequestParam @NotNull(message = "积分不能为空") @Min(value = 1, message = "积分必须大于0") Integer points) {
        memberService.deductPoints(userId, points);
        return R.ok();
    }
}
