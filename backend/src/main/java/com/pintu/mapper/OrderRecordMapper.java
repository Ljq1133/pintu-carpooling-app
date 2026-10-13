package com.pintu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pintu.entity.OrderRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 正式订单表 Mapper。
 */
public interface OrderRecordMapper extends BaseMapper<OrderRecord> {

    @Select("SELECT * FROM order_record WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    OrderRecord selectByIdForUpdate(@Param("id") String id);

    @Select("""
            SELECT * FROM order_record
            WHERE passenger_order_id = #{passengerOrderId}
              AND deleted = 0
            ORDER BY created_at DESC
            LIMIT 1
            FOR UPDATE
            """)
    OrderRecord selectLatestByPassengerOrderForUpdate(@Param("passengerOrderId") String passengerOrderId);
}
