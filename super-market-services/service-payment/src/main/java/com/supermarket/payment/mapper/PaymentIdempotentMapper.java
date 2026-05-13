package com.supermarket.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supermarket.payment.entity.PaymentIdempotent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PaymentIdempotentMapper extends BaseMapper<PaymentIdempotent> {

    @Select("SELECT response_data FROM payment_idempotent WHERE request_id = #{requestId}")
    String getResponseByRequestId(@Param("requestId") String requestId);
}
