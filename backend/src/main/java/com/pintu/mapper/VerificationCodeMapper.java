package com.pintu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pintu.entity.VerificationCode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface VerificationCodeMapper extends BaseMapper<VerificationCode> {

    @Update("""
            UPDATE verification_code
            SET consumed_at = #{consumedAt}
            WHERE id = #{id} AND consumed_at IS NULL AND attempt_count < #{maxAttempts}
            """)
    int consume(@Param("id") String id,
                @Param("consumedAt") LocalDateTime consumedAt,
                @Param("maxAttempts") int maxAttempts);

    @Update("""
            UPDATE verification_code
            SET attempt_count = attempt_count + 1
            WHERE id = #{id} AND consumed_at IS NULL AND attempt_count < #{maxAttempts}
            """)
    int incrementAttempt(@Param("id") String id, @Param("maxAttempts") int maxAttempts);
}
