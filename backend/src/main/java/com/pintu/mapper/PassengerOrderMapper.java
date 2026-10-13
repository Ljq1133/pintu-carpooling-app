package com.pintu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pintu.entity.PassengerOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 乘客需求表 Mapper。
 */
public interface PassengerOrderMapper extends BaseMapper<PassengerOrder> {

    @Select("SELECT * FROM passenger_order WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    PassengerOrder selectByIdForUpdate(@Param("id") String id);
}
