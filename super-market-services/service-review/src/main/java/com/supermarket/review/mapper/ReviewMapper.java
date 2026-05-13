package com.supermarket.review.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supermarket.review.entity.Review;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ReviewMapper extends BaseMapper<Review> {

    @Select("SELECT rating, COUNT(*) as cnt FROM reviews WHERE spu_id = #{spuId} AND status = 1 GROUP BY rating")
    List<Map<String, Object>> countByRating(@Param("spuId") Long spuId);

    @Select("SELECT AVG(rating) FROM reviews WHERE spu_id = #{spuId} AND status = 1")
    Double avgRating(@Param("spuId") Long spuId);
}
