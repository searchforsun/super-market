package com.supermarket.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supermarket.member.entity.Member;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MemberMapper extends BaseMapper<Member> {

    @Update("UPDATE members SET points = points + #{points}, total_points = total_points + #{points} WHERE user_id = #{userId}")
    int addPoints(Long userId, int points);

    @Update("UPDATE members SET points = points - #{points} WHERE user_id = #{userId} AND points >= #{points}")
    int deductPoints(Long userId, int points);
}
