package com.pintu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pintu.entity.Conversation;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ConversationMapper extends BaseMapper<Conversation> {
    @Select("SELECT * FROM conversation WHERE id = #{id} FOR UPDATE")
    Conversation selectByIdForUpdate(@Param("id") String id);
}
