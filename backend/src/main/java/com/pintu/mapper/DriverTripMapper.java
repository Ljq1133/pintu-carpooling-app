package com.pintu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pintu.entity.DriverTrip;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 司机行程表 Mapper。
 */
public interface DriverTripMapper extends BaseMapper<DriverTrip> {

    @Select("SELECT * FROM driver_trip WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    DriverTrip selectByIdForUpdate(@Param("id") String id);
}
