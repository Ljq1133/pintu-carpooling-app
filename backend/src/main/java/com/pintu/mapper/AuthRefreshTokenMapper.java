package com.pintu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pintu.entity.AuthRefreshToken;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuthRefreshTokenMapper extends BaseMapper<AuthRefreshToken> {
}
