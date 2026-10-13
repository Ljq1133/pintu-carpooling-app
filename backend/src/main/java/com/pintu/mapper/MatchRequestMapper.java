package com.pintu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pintu.entity.MatchRequest;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 匹配请求表 Mapper。
 */
public interface MatchRequestMapper extends BaseMapper<MatchRequest> {

    @Select("SELECT * FROM match_request WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    MatchRequest selectByIdForUpdate(@Param("id") String id);
}
