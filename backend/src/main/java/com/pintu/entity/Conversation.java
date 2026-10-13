package com.pintu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("conversation")
public class Conversation {
    @TableId(type = IdType.INPUT)
    private String id;
    private String orderId;
    private String matchRequestId;
    private String passengerId;
    private String driverId;
    private String status;
    private LocalDateTime closedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
