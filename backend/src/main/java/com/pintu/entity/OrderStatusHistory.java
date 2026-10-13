package com.pintu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("order_status_history")
public class OrderStatusHistory {
    @TableId(type = IdType.INPUT)
    private String id;
    private String orderId;
    private String operatorId;
    private String operatorRole;
    private String fromStatus;
    private String toStatus;
    private String reason;
    private String idempotencyKey;
    private LocalDateTime createdAt;
}
