package com.pintu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("trip_share_token")
public class TripShareToken {

    @TableId(type = IdType.INPUT)
    private String id;
    private String token;
    private String orderId;
    private String createdBy;
    private LocalDateTime expiresAt;
    private LocalDateTime revokedAt;
    private LocalDateTime createdAt;
}
