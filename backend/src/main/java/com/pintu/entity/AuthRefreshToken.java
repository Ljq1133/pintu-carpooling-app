package com.pintu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("auth_refresh_token")
public class AuthRefreshToken {

    @TableId(type = IdType.INPUT)
    private String id;
    private String userId;
    private String tokenHash;
    private LocalDateTime expiresAt;
    private LocalDateTime revokedAt;
    private String deviceInfo;
    private String activeRole;
    private LocalDateTime createdAt;
}
